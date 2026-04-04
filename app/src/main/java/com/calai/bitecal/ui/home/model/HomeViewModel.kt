package com.calai.bitecal.ui.home.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.activity.model.DailyActivityStatus
import com.calai.bitecal.data.activity.sync.DailyActivitySyncer
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.foodlog.repo.HomeCardPollResult
import com.calai.bitecal.data.health.HealthConnectRepository
import com.calai.bitecal.data.home.repo.HomeRepository
import com.calai.bitecal.data.home.repo.HomeSummary
import com.calai.bitecal.data.profile.repo.ProfileRepository
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.ui.home.ui.foodlog.FoodLogTimeResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

data class HomeUiState(
    val loading: Boolean = true,
    val summary: HomeSummary? = null,
    val error: String? = null,
    val selectedDayOffset: Int = 0 // 0=今天，-1=昨天...
)

/** 只用 UI 會渲染到的欄位建立簽章，降低不必要重組 */
private data class SummaryUiKey(
    val avatarUrl: String?,
    val tdee: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val waterTodayMl: Int,
    val waterGoalMl: Int,
    val steps: Long,
    val exerciseMinutes: Long,
    val activeKcalInt: Int,
    val fastingPlan: String?,
    val weightDiffSigned: Double,
    val weightDiffUnit: String,
    val recentMealsSig: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val hc: HealthConnectRepository,
    private val profileRepo: ProfileRepository,
    private val dailySyncer: DailyActivitySyncer,
    private val profileStore: UserProfileStore,
    private val zoneId: ZoneId,
    private val foodLogsRepository: FoodLogsRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val recentPreviewCacheMaxAgeMs = TimeUnit.DAYS.toMillis(3)
    private val recentUploadLookBackDays = 3L

    private val _pendingOpenCamera = MutableStateFlow(false)
    val pendingOpenCamera: StateFlow<Boolean> = _pendingOpenCamera.asStateFlow()

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    // ====== Daily Activity override（給 HomeScreen 顯示） ======
    private val _dailyStatus = MutableStateFlow(DailyActivityStatus.ERROR_RETRYABLE)
    val dailyStatus: StateFlow<DailyActivityStatus> = _dailyStatus.asStateFlow()

    private val _dailyStepsToday = MutableStateFlow<Long?>(null)
    val dailyStepsToday: StateFlow<Long?> = _dailyStepsToday.asStateFlow()

    private val _dailyActiveKcalToday = MutableStateFlow<Int?>(null)
    val dailyActiveKcalToday: StateFlow<Int?> = _dailyActiveKcalToday.asStateFlow()

    private val _dailyStepGoal = MutableStateFlow(10000L)
    val dailyStepGoal: StateFlow<Long> = _dailyStepGoal.asStateFlow()

    private val _dailyWorkoutGoalKcal = MutableStateFlow(450) // fallback
    val dailyWorkoutGoalKcal: StateFlow<Int> = _dailyWorkoutGoalKcal.asStateFlow()

    // ====== Summary key：沒變就不要 emit，避免整頁重組 ======
    private var lastSummaryKey: SummaryUiKey? = null

    // ✅ 避免重複同步（回前景/refresh 連發）
    private var refreshDailyJob: Job? = null

    // ✅ 1.5 秒 debounce（只擋「自動連發」，手動/授權要能 bypass）
    private val dailyDebounceMs: Long = 1_500L
    private var lastDailyRefreshAtMs: Long = 0L

    //============= recentUploads ==================
    private val _recentUploads = MutableStateFlow<List<HomeRecentUploadUi>>(emptyList())
    val recentUploads: StateFlow<List<HomeRecentUploadUi>> = _recentUploads.asStateFlow()

    private var recentUploadPollJob: Job? = null
    private var recentUploadRestoreJob: Job? = null

    private companion object {
        const val TAG = "HomeViewModel"
    }

    init {
        refresh()
        restoreRecentUploadsFromServer()

        viewModelScope.launch {
            profileStore.dailyStepGoalFlow.collectLatest { v ->
                _dailyStepGoal.value = v.toLong()
            }
        }

        viewModelScope.launch {
            profileStore.dailyWorkoutGoalUiFlow.collectLatest { v ->
                _dailyWorkoutGoalKcal.value = v
            }
        }
    }

    private fun shouldStartDailyRefresh(force: Boolean): Boolean {
        val now = SystemClock.elapsedRealtime()

        // force：永遠允許（但會先 cancel 舊 job）
        if (force) {
            lastDailyRefreshAtMs = now
            return true
        }

        // job 還在跑：不要重進（避免浪費）
        if (refreshDailyJob?.isActive == true) return false

        // debounce：1.5 秒內忽略
        if (now - lastDailyRefreshAtMs < dailyDebounceMs) return false

        lastDailyRefreshAtMs = now
        return true
    }

    private fun resolveRecentUploadTimeText(
        env: FoodLogEnvelopeDto,
        fallbackTimeText: String = ""
    ): String {
        return FoodLogTimeResolver.resolveDisplayTimeText(
            zoneId = zoneId,
            updatedAtUtc = env.updatedAtUtc,
            serverReceivedAtUtc = env.serverReceivedAtUtc,
            capturedAtUtc = env.capturedAtUtc,
            capturedLocalDate = env.capturedLocalDate
        ).ifBlank { fallbackTimeText }
    }

    fun onFoodLogCreated(
        env: FoodLogEnvelopeDto,
        previewUri: String?,
        timeText: String
    ) {
        recentUploadPollJob?.cancel()

        when (env.status) {
            FoodLogStatus.PENDING -> {
                upsertRecentUpload(
                    HomeRecentUploadMapper.pending(
                        foodLogId = env.foodLogId,
                        previewUri = previewUri,
                        timeText = timeText
                    )
                )
                startRecentUploadPolling(
                    foodLogId = env.foodLogId,
                    previewUri = previewUri,
                    timeText = timeText
                )
            }

            FoodLogStatus.DRAFT,
            FoodLogStatus.SAVED -> {
                val resolvedTimeText = resolveRecentUploadTimeText(
                    env = env,
                    fallbackTimeText = timeText
                )

                upsertRecentUpload(
                    HomeRecentUploadMapper.success(
                        foodLogId = env.foodLogId,
                        previewUri = previewUri,
                        timeText = resolvedTimeText,
                        env = env
                    )
                )
                refresh()
            }

            FoodLogStatus.FAILED,
            FoodLogStatus.DELETED -> {
                removeRecentUpload(env.foodLogId)
            }
        }
    }

    fun clearRecentUpload() {
        recentUploadPollJob?.cancel()
        recentUploadPollJob = null
        _recentUploads.value = emptyList()
    }

    private fun upsertRecentUpload(item: HomeRecentUploadUi) {
        _recentUploads.update { current ->
            buildList {
                add(item)
                current
                    .filterNot { it.foodLogId == item.foodLogId }
                    .take(9)
                    .forEach(::add)
            }
        }
    }

    private fun removeRecentUpload(foodLogId: String) {
        _recentUploads.update { current ->
            current.filterNot { it.foodLogId == foodLogId }
        }
    }

    fun onRecentUploadDeleted(foodLogId: String) {
        recentUploadPollJob?.cancel()
        recentUploadPollJob = null

        removeRecentUpload(foodLogId)
        deleteRecentUploadPreviewCache(foodLogId)
    }

    fun deleteRecentUpload(
        foodLogId: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val deletingPendingLike = _recentUploads.value
                .firstOrNull { it.foodLogId == foodLogId }
                ?.let { item ->
                    item is HomeRecentUploadUi.Pending || item is HomeRecentUploadUi.Delayed
                } == true

            if (deletingPendingLike) {
                recentUploadPollJob?.cancel()
                recentUploadPollJob = null
            }

            try {
                withContext(Dispatchers.IO) {
                    foodLogsRepository.delete(foodLogId)
                }

                onRecentUploadDeleted(foodLogId)
                refresh()

                if (deletingPendingLike) {
                    restoreRecentUploadsFromServer()
                }

                onSuccess()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                if (deletingPendingLike) {
                    restoreRecentUploadsFromServer()
                }
                onFailure(t)
            }
        }
    }

    private fun deleteRecentUploadPreviewCache(foodLogId: String) {
        val file = File(
            appContext.cacheDir,
            "foodlog_recent_upload_preview/foodlog_$foodLogId.img"
        )

        runCatching {
            if (file.exists() && !file.delete()) {
                Log.w(
                    TAG,
                    "deleteRecentUploadPreviewCache delete returned false path=${file.absolutePath}"
                )
            }
        }.onFailure { t ->
            Log.w(
                TAG,
                "deleteRecentUploadPreviewCache failed foodLogId=$foodLogId: ${t.javaClass.simpleName}: ${t.message}",
                t
            )
        }
    }

    private fun replaceRecentUploads(items: List<HomeRecentUploadUi>) {
        _recentUploads.value = items.take(10)
    }

    private fun startRecentUploadPolling(
        foodLogId: String,
        previewUri: String?,
        timeText: String
    ) {
        recentUploadPollJob?.cancel()

        recentUploadPollJob = viewModelScope.launch {
            var enteredDelayedState = false

            while (true) {
                try {
                    when (val result = withContext(Dispatchers.IO) {
                        foodLogsRepository.pollForHomeCard(
                            id = foodLogId,
                            hotWindowMs = if (enteredDelayedState) 8_000L else 15_000L,
                            maxAttempts = if (enteredDelayedState) 4 else 8
                        )
                    }) {
                        is HomeCardPollResult.Terminal -> {
                            when (result.env.status) {
                                FoodLogStatus.DRAFT,
                                FoodLogStatus.SAVED -> {
                                    val resolvedTimeText = resolveRecentUploadTimeText(
                                        env = result.env,
                                        fallbackTimeText = timeText
                                    )

                                    upsertRecentUpload(
                                        HomeRecentUploadMapper.success(
                                            foodLogId = foodLogId,
                                            previewUri = previewUri,
                                            timeText = resolvedTimeText,
                                            env = result.env
                                        )
                                    )
                                    refresh()
                                    return@launch
                                }

                                FoodLogStatus.FAILED,
                                FoodLogStatus.DELETED -> {
                                    removeRecentUpload(foodLogId)
                                    return@launch
                                }

                                FoodLogStatus.PENDING -> {
                                    if (!enteredDelayedState) {
                                        enteredDelayedState = true
                                        upsertRecentUpload(
                                            HomeRecentUploadMapper.delayed(
                                                foodLogId = foodLogId,
                                                previewUri = previewUri,
                                                timeText = timeText
                                            )
                                        )
                                    }
                                    delay(8_000L)
                                }
                            }
                        }

                        is HomeCardPollResult.StillPending -> {
                            if (!enteredDelayedState) {
                                enteredDelayedState = true
                                upsertRecentUpload(
                                    HomeRecentUploadMapper.delayed(
                                        foodLogId = foodLogId,
                                        previewUri = previewUri,
                                        timeText = timeText
                                    )
                                )
                            }
                            delay(8_000L)
                        }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    upsertRecentUpload(
                        HomeRecentUploadMapper.delayed(
                            foodLogId = foodLogId,
                            previewUri = previewUri,
                            timeText = timeText,
                            title = "網路較慢",
                            subtitle = "稍後會自動再試"
                        )
                    )
                    delay(10_000L)
                }
            }
        }
    }

    override fun onCleared() {
        recentUploadPollJob?.cancel()
        recentUploadRestoreJob?.cancel()
        super.onCleared()
    }

    private fun restoreRecentUploadsFromServer() {
        if (recentUploadPollJob?.isActive == true) return

        recentUploadRestoreJob?.cancel()
        recentUploadRestoreJob = viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                pruneRecentUploadPreviewCache()
                loadRecentUploadsFromServer()
            }

            if (recentUploadPollJob?.isActive != true) {
                replaceRecentUploads(restored)

                val pendingLike = restored.firstOrNull {
                    it is HomeRecentUploadUi.Pending || it is HomeRecentUploadUi.Delayed
                }

                if (pendingLike != null) {
                    startRecentUploadPolling(
                        foodLogId = pendingLike.foodLogId,
                        previewUri = pendingLike.previewUri,
                        timeText = pendingLike.timeText
                    )
                }
            }
        }
    }

    private suspend fun loadRecentUploadsFromServer(): List<HomeRecentUploadUi> {
        val items = foodLogsRepository.listHomeRecentUploads(
            zoneId = zoneId,
            lookBackDays = recentUploadLookBackDays,
            maxItems = 10
        )

        return items.mapNotNull { item ->
            HomeRecentUploadMapper.fromListItem(
                previewUri = cacheRecentUploadPreview(item.foodLogId),
                timeText = FoodLogTimeResolver.resolveDisplayTimeText(
                    zoneId = zoneId,
                    updatedAtUtc = item.updatedAtUtc,
                    serverReceivedAtUtc = item.serverReceivedAtUtc,
                    capturedAtUtc = item.capturedAtUtc,
                    capturedLocalDate = item.capturedLocalDate
                ),
                item = item
            )
        }
    }

    private suspend fun cacheRecentUploadPreview(foodLogId: String): String? {
        return try {
            val bytes = foodLogsRepository.downloadImageBytes(foodLogId)
            if (bytes.isEmpty()) {
                null
            } else {
                val dir = File(appContext.cacheDir, "foodlog_recent_upload_preview")
                    .apply { mkdirs() }

                val file = File(dir, "foodlog_$foodLogId.img")
                file.writeBytes(bytes)
                file.setLastModified(System.currentTimeMillis())
                Uri.fromFile(file).toString()
            }
        } catch (t: Throwable) {
            Log.w(
                TAG,
                "cacheRecentUploadPreview failed foodLogId=$foodLogId: ${t.javaClass.simpleName}: ${t.message}",
                t
            )
            null
        }
    }

    fun onRecentUploadUpdated(
        env: FoodLogEnvelopeDto,
        previewUri: String?
    ) {
        recentUploadPollJob?.cancel()
        recentUploadPollJob = null

        val resolvedTimeText = resolveRecentUploadTimeText(env = env)

        when (env.status) {
            FoodLogStatus.DRAFT,
            FoodLogStatus.SAVED -> {
                upsertRecentUpload(
                    HomeRecentUploadMapper.success(
                        foodLogId = env.foodLogId,
                        previewUri = previewUri,
                        timeText = resolvedTimeText,
                        env = env
                    )
                )
                refresh()
                recentUploadRestoreJob?.cancel()
                recentUploadRestoreJob = null
            }

            FoodLogStatus.PENDING -> {
                upsertRecentUpload(
                    HomeRecentUploadMapper.pending(
                        foodLogId = env.foodLogId,
                        previewUri = previewUri,
                        timeText = resolvedTimeText
                    )
                )
                startRecentUploadPolling(
                    foodLogId = env.foodLogId,
                    previewUri = previewUri,
                    timeText = resolvedTimeText
                )
            }

            FoodLogStatus.FAILED,
            FoodLogStatus.DELETED -> {
                removeRecentUpload(env.foodLogId)
                deleteRecentUploadPreviewCache(env.foodLogId)
                refresh()
            }
        }
    }

    private fun pruneRecentUploadPreviewCache() {
        val dir = File(appContext.cacheDir, "foodlog_recent_upload_preview")
        if (!dir.exists() || !dir.isDirectory) return

        val cutoff = System.currentTimeMillis() - recentPreviewCacheMaxAgeMs
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                runCatching {
                    val deleted = file.delete()
                    if (!deleted && file.exists()) {
                        Log.w(
                            TAG,
                            "pruneRecentUploadPreviewCache delete returned false path=${file.absolutePath}"
                        )
                    }
                }.onFailure { t ->
                    Log.w(
                        TAG,
                        "pruneRecentUploadPreviewCache failed path=${file.absolutePath}: ${t.javaClass.simpleName}: ${t.message}",
                        t
                    )
                }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }

        // ✅ 自動觸發：不 force（會被 1.5 秒 debounce 擋）
        refreshDailyActivity(force = false)

        val first = runCatching {
            withContext(Dispatchers.IO) { repo.loadSummaryFromServer().getOrThrow() }
        }

        if (first.isSuccess) {
            applySummary(first.getOrThrow())
            return@launch
        }

        val e = first.exceptionOrNull()
        val code = (e as? HttpException)?.code()
        val recoverable = code == 401 || code == 404

        if (recoverable) {
            val ok = withContext(Dispatchers.IO) { profileRepo.upsertFromLocal().isSuccess }
            if (ok) {
                val second = runCatching {
                    withContext(Dispatchers.IO) { repo.loadSummaryFromServer().getOrThrow() }
                }
                if (second.isSuccess) {
                    applySummary(second.getOrThrow())
                    return@launch
                }
                _ui.update {
                    it.copy(
                        loading = false,
                        error = second.exceptionOrNull()?.message ?: "Failed after recovery"
                    )
                }
                return@launch
            }
        }

        _ui.update { it.copy(loading = false, error = e?.message ?: "Failed to load profile") }
    }

    private fun applySummary(summary: HomeSummary) {
        val newKey = summary.toUiKey()
        val firstTime = _ui.value.summary == null
        val changed = newKey != lastSummaryKey

        // ✅ 沒變就不要 emit 新 summary，避免整頁重組
        if (!firstTime && !changed) {
            _ui.update { it.copy(loading = false, error = null) }
            return
        }

        lastSummaryKey = newKey
        _ui.update {
            it.copy(
                loading = false,
                summary = summary,
                error = null
            )
        }
    }

    fun onAddWater(ml: Int) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { repo.addWater(ml) } }
        refresh()
    }

    /**
     * ✅ today 活動同步（加 debounce / force）
     *
     * @param force
     * - false：自動觸發（onResume / refresh()）會被 1.5 秒 debounce 擋掉
     * - true：使用者點擊 / permission result 需要「立刻更新」→ 不擋
     */
    fun refreshDailyActivity(force: Boolean = false) {
        if (!shouldStartDailyRefresh(force)) return

        if (force) refreshDailyJob?.cancel()

        refreshDailyJob = viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) {
                    dailySyncer.syncLast7DaysWithStatus(zoneId)
                }

                r.onFailure { t ->
                    if (t is CancellationException) throw t
                    _dailyStatus.value = DailyActivityStatus.ERROR_RETRYABLE
                    _dailyStepsToday.value = null
                    _dailyActiveKcalToday.value = null
                }

                r.onSuccess { result ->
                    _dailyStatus.value = result.status

                    if (result.status != DailyActivityStatus.AVAILABLE_GRANTED) {
                        _dailyStepsToday.value = null
                        _dailyActiveKcalToday.value = null
                        return@onSuccess
                    }

                    val today = LocalDate.now(zoneId)
                    val todayRow = result.days.lastOrNull { it.localDate == today }

                    if (todayRow == null) {
                        _dailyStatus.value = DailyActivityStatus.NO_DATA
                        // ✅ 合成 0：因為 HC 沒有「0 record」，只有「沒有 record」
                        _dailyStepsToday.value = 0L
                        _dailyActiveKcalToday.value = 0
                    } else {
                        _dailyStepsToday.value = todayRow.steps
                        _dailyActiveKcalToday.value = todayRow.activeKcal
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                _dailyStatus.value = DailyActivityStatus.ERROR_RETRYABLE
                _dailyStepsToday.value = null
                _dailyActiveKcalToday.value = null
            }
        }
    }

    /**
     * ✅ 卡片 CTA 點擊（最小可用版）
     * - 未安裝：導 Play Store
     * - 未授權/不可用：目前也先導 Play Store
     */
    fun onDailyCtaClick(ctx: Context) {
        when (_dailyStatus.value) {
            DailyActivityStatus.HC_NOT_INSTALLED -> {
                openPlayStore(ctx, "com.google.android.apps.healthdata")
            }

            DailyActivityStatus.NO_DATA -> openHealthConnectOrStore(ctx)

            DailyActivityStatus.PERMISSION_NOT_GRANTED,
            DailyActivityStatus.HC_UNAVAILABLE -> openHealthConnectOrStore(ctx)

            DailyActivityStatus.ERROR_RETRYABLE -> refreshDailyActivity(force = true)

            DailyActivityStatus.AVAILABLE_GRANTED -> Unit
        }
    }

    private fun openHealthConnectOrStore(ctx: Context) {
        val pkg = "com.google.android.apps.healthdata"
        val launch = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        when {
            launch != null -> runCatching { ctx.startActivity(launch) }
            else -> runCatching { ctx.startActivity(market) }
                .recoverCatching { ctx.startActivity(web) }
        }
    }

    private fun openPlayStore(ctx: Context, pkg: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(market) }
            .recoverCatching { ctx.startActivity(web) }
    }

    fun onRequestHealthPermissions() = viewModelScope.launch {
        refresh()
    }

    fun refreshAfterLogin() {
        viewModelScope.launch {
            // TODO: 重新拉 summary / profile / macros
        }
    }

    fun markPendingOpenCamera() {
        _pendingOpenCamera.value = true
    }

    fun clearPendingOpenCamera() {
        _pendingOpenCamera.value = false
    }
}

/* -------------------- 私有工具 -------------------- */

private fun HomeSummary.toUiKey(): SummaryUiKey {
    val rm = this.recentMeals
    val recentSig = buildString {
        append(rm.size)
        rm.take(3).forEach { m -> append('|').append(m.hashCode()) }
    }

    // ✅ activeKcal 可能為 null：用 -1 當 key 的穩定 sentinel（不影響 UI 顯示）
    val activeKcalSafe = this.todayActivity.activeKcal?.toDouble()?.roundToInt() ?: -1

    return SummaryUiKey(
        avatarUrl = this.avatarUrl?.toString(),
        tdee = this.tdee,
        proteinG = this.proteinG,
        carbsG = this.carbsG,
        fatG = this.fatG,
        waterTodayMl = this.waterTodayMl,
        waterGoalMl = this.waterGoalMl,
        steps = this.todayActivity.steps,
        exerciseMinutes = this.todayActivity.exerciseMinutes,
        activeKcalInt = activeKcalSafe,
        fastingPlan = this.fastingPlan,
        weightDiffSigned = this.weightDiffSigned,
        weightDiffUnit = this.weightDiffUnit,
        recentMealsSig = recentSig
    )
}
