package com.calai.bitecal.ui.home.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.activity.model.DailyActivityStatus
import com.calai.bitecal.data.activity.sync.DailyActivitySyncer
import com.calai.bitecal.data.foodlog.event.FoodLogMutationBus
import com.calai.bitecal.data.foodlog.event.FoodLogMutationEvent
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.foodlog.repo.HomeCardPollResult
import com.calai.bitecal.data.foodlog.repo.HomeTodayNutritionSummary
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

data class HomeUiState(
    val loading: Boolean = true,
    val summary: HomeSummary? = null,
    val todayNutrition: HomeTodayNutritionSummary = HomeTodayNutritionSummary(),
    val calendarNutritionByDate: Map<LocalDate, HomeTodayNutritionSummary> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(),
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
    val fiberG: Int,
    val sugarG: Int,
    val sodiumMg: Int,
    val waterTodayMl: Int,
    val waterGoalMl: Int,
    val steps: Long,
    val exerciseMinutes: Long,
    val activeKcalInt: Int,
    val fastingPlan: String?,
    val weightDiffSigned: Double,
    val weightDiffUnit: String
)

private data class DailyStableSnapshot(
    val status: DailyActivityStatus,
    val steps: Long?,
    val activeKcal: Int?
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
    private val foodLogMutationBus: FoodLogMutationBus,
    @ApplicationContext private val appContext: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recentPreviewCacheMaxAgeMs = TimeUnit.DAYS.toMillis(3)
    private val recentUploadLookBackDays = 3L

    private val _pendingOpenCamera = MutableStateFlow(false)
    val pendingOpenCamera: StateFlow<Boolean> = _pendingOpenCamera.asStateFlow()

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    // ====== Daily Activity override（給 HomeScreen 顯示） ======
    private companion object {
        const val TAG = "HomeViewModel"

        const val KEY_LAST_STABLE_DAILY_STATUS = "last_stable_daily_status"
        const val KEY_LAST_STABLE_DAILY_STEPS = "last_stable_daily_steps"
        const val KEY_LAST_STABLE_DAILY_ACTIVE_KCAL = "last_stable_daily_active_kcal"
        const val MAX_VISIBLE_NUTRITION_WEEK_OFFSET = 5

        @Volatile
        private var inMemoryStableDailySnapshot: DailyStableSnapshot? = null
    }

    private fun restoreLastStableDailySnapshot(): DailyStableSnapshot? {
        inMemoryStableDailySnapshot?.let { return it }

        val rawStatus = savedStateHandle.get<String>(KEY_LAST_STABLE_DAILY_STATUS)
        val status = rawStatus?.let { runCatching { DailyActivityStatus.valueOf(it) }.getOrNull() }
            ?: return null

        val steps = savedStateHandle.get<Long>(KEY_LAST_STABLE_DAILY_STEPS)
        val activeKcal = savedStateHandle.get<Int>(KEY_LAST_STABLE_DAILY_ACTIVE_KCAL)

        return DailyStableSnapshot(
            status = status,
            steps = steps,
            activeKcal = activeKcal
        )
    }

    private fun persistLastStableDailySnapshot(snapshot: DailyStableSnapshot) {
        inMemoryStableDailySnapshot = snapshot
        savedStateHandle[KEY_LAST_STABLE_DAILY_STATUS] = snapshot.status.name
        savedStateHandle[KEY_LAST_STABLE_DAILY_STEPS] = snapshot.steps
        savedStateHandle[KEY_LAST_STABLE_DAILY_ACTIVE_KCAL] = snapshot.activeKcal
    }

    private var lastStableDailySnapshot: DailyStableSnapshot? = restoreLastStableDailySnapshot()

    private val _dailyReady = MutableStateFlow(lastStableDailySnapshot != null)
    val dailyReady: StateFlow<Boolean> = _dailyReady.asStateFlow()

    private val _dailyStatus = MutableStateFlow(
        lastStableDailySnapshot?.status ?: DailyActivityStatus.ERROR_RETRYABLE
    )
    val dailyStatus: StateFlow<DailyActivityStatus> = _dailyStatus.asStateFlow()

    private val _dailyStepsToday = MutableStateFlow(lastStableDailySnapshot?.steps)
    val dailyStepsToday: StateFlow<Long?> = _dailyStepsToday.asStateFlow()

    private val _dailyActiveKcalToday = MutableStateFlow(lastStableDailySnapshot?.activeKcal)
    val dailyActiveKcalToday: StateFlow<Int?> = _dailyActiveKcalToday.asStateFlow()

    private fun applyStableDailyState(
        status: DailyActivityStatus,
        steps: Long?,
        activeKcal: Int?
    ) {
        val snapshot = DailyStableSnapshot(
            status = status,
            steps = steps,
            activeKcal = activeKcal
        )
        lastStableDailySnapshot = snapshot
        persistLastStableDailySnapshot(snapshot)

        _dailyStatus.value = status
        _dailyStepsToday.value = steps
        _dailyActiveKcalToday.value = activeKcal
        _dailyReady.value = true
    }

    private fun fallbackToLastStableDailyState() {
        val stable = lastStableDailySnapshot
        if (stable != null) {
            _dailyStatus.value = stable.status
            _dailyStepsToday.value = stable.steps
            _dailyActiveKcalToday.value = stable.activeKcal
            _dailyReady.value = true
            return
        }

        _dailyStatus.value = DailyActivityStatus.ERROR_RETRYABLE
        _dailyStepsToday.value = null
        _dailyActiveKcalToday.value = null
        _dailyReady.value = true
    }

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

    // ====== CalendarStrip 日期切換營養摘要快取 ======
    private val nutritionSummaryByDateCache = mutableMapOf<LocalDate, HomeTodayNutritionSummary>()
    private val loadedNutritionWeekOffsets = mutableSetOf<Int>()
    private var selectedNutritionJob: Job? = null
    private var nutritionPrefetchJob: Job? = null

    //============= recentUploads ==================
    private val _recentUploads = MutableStateFlow<List<HomeRecentUploadUi>>(emptyList())
    val recentUploads: StateFlow<List<HomeRecentUploadUi>> = _recentUploads.asStateFlow()

    private var recentUploadPollJob: Job? = null
    private var recentUploadPollingFoodLogId: String? = null
    private var recentUploadRestoreJob: Job? = null

    init {
        observeFoodLogMutations()
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

    private fun observeFoodLogMutations() {
        viewModelScope.launch {
            foodLogMutationBus.events.collectLatest { event ->
                when (event) {
                    is FoodLogMutationEvent.Upserted -> {
                        onRecentUploadUpdated(
                            env = event.env,
                            previewUri = event.previewUri,
                            fallbackTimeText = event.timeText.orEmpty(),
                            moveExistingToTop = event.moveToTop
                        )
                    }

                    is FoodLogMutationEvent.Deleted -> {
                        onFoodLogDeletedFromMutation(
                            foodLogId = event.foodLogId,
                            capturedLocalDate = event.capturedLocalDate
                        )
                    }
                }
            }
        }
    }

    private fun onFoodLogDeletedFromMutation(
        foodLogId: String,
        capturedLocalDate: String?
    ) {
        if (recentUploadPollingFoodLogId == foodLogId) {
            recentUploadPollJob?.cancel()
            recentUploadPollJob = null
            recentUploadPollingFoodLogId = null
        }

        removeRecentUpload(foodLogId)
        deleteRecentUploadPreviewCache(foodLogId)

        val parsedDate = capturedLocalDate
            ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }

        if (parsedDate != null) {
            invalidateNutritionWeekForDate(parsedDate)
            refresh()
        } else {
            refreshAfterFoodLogMutation(null)
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

    fun clearRecentUpload() {
        recentUploadPollJob?.cancel()
        recentUploadPollJob = null
        recentUploadPollingFoodLogId = null
        _recentUploads.value = emptyList()
    }

    private fun upsertRecentUpload(
        item: HomeRecentUploadUi,
        moveExistingToTop: Boolean = false
    ) {
        _recentUploads.update { current ->
            val existingIndex = current.indexOfFirst { it.foodLogId == item.foodLogId }
            if (existingIndex >= 0 && !moveExistingToTop) {
                current
                    .map { existing ->
                        if (existing.foodLogId == item.foodLogId) item else existing
                    }
                    .take(10)
            } else {
                buildList {
                    add(item)
                    current
                        .filterNot { it.foodLogId == item.foodLogId }
                        .take(9)
                        .forEach(::add)
                }
            }
        }
    }

    private fun removeRecentUpload(foodLogId: String) {
        _recentUploads.update { current ->
            current.filterNot { it.foodLogId == foodLogId }
        }
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

            if (deletingPendingLike && recentUploadPollingFoodLogId == foodLogId) {
                recentUploadPollJob?.cancel()
                recentUploadPollJob = null
                recentUploadPollingFoodLogId = null
            }

            try {
                withContext(Dispatchers.IO) {
                    foodLogsRepository.delete(foodLogId)
                }

                foodLogMutationBus.publishDeleted(foodLogId = foodLogId)

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
        recentUploadPollingFoodLogId = foodLogId

        recentUploadPollJob = viewModelScope.launch {
            var enteredDelayedState = false

            try {
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
                                        refreshAfterFoodLogMutation(result.env)
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
            } finally {
                if (recentUploadPollingFoodLogId == foodLogId) {
                    recentUploadPollingFoodLogId = null
                    recentUploadPollJob = null
                }
            }
        }
    }

    override fun onCleared() {
        recentUploadPollJob?.cancel()
        recentUploadPollingFoodLogId = null
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

    private fun resolveRecentUploadPreviewUri(
        foodLogId: String,
        incomingPreviewUri: String?
    ): String? {
        val incoming = incomingPreviewUri?.takeIf { it.isNotBlank() }
        if (incoming != null) return incoming

        val existing = _recentUploads.value
            .firstOrNull { it.foodLogId == foodLogId }
            ?.previewUri
            ?.takeIf { it.isNotBlank() }
        if (existing != null) return existing

        val cachedFile = File(
            appContext.cacheDir,
            "foodlog_recent_upload_preview/foodlog_$foodLogId.img"
        )

        return cachedFile
            .takeIf { it.exists() && it.length() > 0L }
            ?.let { Uri.fromFile(it).toString() }
    }

    fun onRecentUploadUpdated(
        env: FoodLogEnvelopeDto,
        previewUri: String?,
        fallbackTimeText: String = "",
        moveExistingToTop: Boolean = false
    ) {
        if (recentUploadPollingFoodLogId == env.foodLogId) {
            recentUploadPollJob?.cancel()
            recentUploadPollJob = null
            recentUploadPollingFoodLogId = null
        }

        val resolvedTimeText = resolveRecentUploadTimeText(
            env = env,
            fallbackTimeText = fallbackTimeText
        )
        val resolvedPreviewUri = resolveRecentUploadPreviewUri(
            foodLogId = env.foodLogId,
            incomingPreviewUri = previewUri
        )

        when (env.status) {
            FoodLogStatus.DRAFT,
            FoodLogStatus.SAVED -> {
                upsertRecentUpload(
                    item = HomeRecentUploadMapper.success(
                        foodLogId = env.foodLogId,
                        previewUri = resolvedPreviewUri,
                        timeText = resolvedTimeText,
                        env = env
                    ),
                    moveExistingToTop = moveExistingToTop
                )
                refreshAfterFoodLogMutation(env)
                recentUploadRestoreJob?.cancel()
                recentUploadRestoreJob = null
            }

            FoodLogStatus.PENDING -> {
                upsertRecentUpload(
                    HomeRecentUploadMapper.pending(
                        foodLogId = env.foodLogId,
                        previewUri = resolvedPreviewUri,
                        timeText = resolvedTimeText
                    )
                )
                startRecentUploadPolling(
                    foodLogId = env.foodLogId,
                    previewUri = resolvedPreviewUri,
                    timeText = resolvedTimeText
                )
            }

            FoodLogStatus.FAILED,
            FoodLogStatus.DELETED -> {
                removeRecentUpload(env.foodLogId)
                deleteRecentUploadPreviewCache(env.foodLogId)
                refreshAfterFoodLogMutation(env)
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


    private fun refreshAfterFoodLogMutation(env: FoodLogEnvelopeDto?) {
        invalidateNutritionCacheForFoodLog(env)
        refresh()
    }

    private fun invalidateNutritionCacheForFoodLog(env: FoodLogEnvelopeDto?) {
        val capturedDate = env?.capturedLocalDate
            ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }

        if (capturedDate != null) {
            invalidateNutritionWeekForDate(capturedDate)
        } else {
            invalidateVisibleNutritionCache()
        }
    }

    private fun invalidateNutritionWeekForDate(localDate: LocalDate) {
        val safeDate = safeSelectableDate(localDate)
        val weekOffset = foodLogsRepository.weekOffsetForDate(safeDate, zoneId)

        if (weekOffset == null) {
            nutritionSummaryByDateCache.remove(safeDate)
            publishCalendarNutritionCache()
            return
        }

        val weekStart = safeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        repeat(7) { dayOffset ->
            nutritionSummaryByDateCache.remove(weekStart.plusDays(dayOffset.toLong()))
        }
        loadedNutritionWeekOffsets.remove(weekOffset)
        publishCalendarNutritionCache()
    }

    private fun invalidateVisibleNutritionCache() {
        nutritionSummaryByDateCache.clear()
        loadedNutritionWeekOffsets.clear()
        publishCalendarNutritionCache()
    }


    private fun todayLocalDate(): LocalDate = LocalDate.now(zoneId)

    private fun safeSelectableDate(date: LocalDate): LocalDate {
        val today = todayLocalDate()
        return if (date.isAfter(today)) today else date
    }

    private fun cacheNutritionWeek(
        weekOffset: Int,
        values: Map<LocalDate, HomeTodayNutritionSummary>
    ) {
        nutritionSummaryByDateCache.putAll(values)
        loadedNutritionWeekOffsets.add(weekOffset)
        publishCalendarNutritionCache()
    }

    private fun publishCalendarNutritionCache() {
        val snapshot = nutritionSummaryByDateCache.toMap()
        _ui.update { current ->
            if (current.calendarNutritionByDate == snapshot) {
                current
            } else {
                current.copy(calendarNutritionByDate = snapshot)
            }
        }
    }

    private suspend fun loadNutritionSummaryForDate(
        localDate: LocalDate
    ): HomeTodayNutritionSummary {
        val safeDate = safeSelectableDate(localDate)
        nutritionSummaryByDateCache[safeDate]?.let { return it }

        val weekOffset = foodLogsRepository.weekOffsetForDate(safeDate, zoneId)
            ?: return HomeTodayNutritionSummary()

        return runCatching {
            withContext(Dispatchers.IO) {
                foodLogsRepository.getNutritionSummariesForWeek(weekOffset)
            }
        }.map { weekValues ->
            cacheNutritionWeek(weekOffset, weekValues)
            weekValues[safeDate] ?: HomeTodayNutritionSummary()
        }.getOrElse { t ->
            Log.w(
                TAG,
                "loadNutritionSummaryForDate failed date=$safeDate: ${t.javaClass.simpleName}: ${t.message}",
                t
            )
            HomeTodayNutritionSummary()
        }
    }

    private fun prefetchVisibleCalendarNutritionSummaries() {
        if (nutritionPrefetchJob?.isActive == true) return

        nutritionPrefetchJob = viewModelScope.launch {
            for (weekOffset in 0..MAX_VISIBLE_NUTRITION_WEEK_OFFSET) {
                if (weekOffset in loadedNutritionWeekOffsets) continue

                runCatching {
                    withContext(Dispatchers.IO) {
                        foodLogsRepository.getNutritionSummariesForWeek(weekOffset)
                    }
                }.onSuccess { values ->
                    cacheNutritionWeek(weekOffset, values)
                }.onFailure { t ->
                    Log.w(
                        TAG,
                        "prefetchVisibleCalendarNutritionSummaries failed weekOffset=$weekOffset: ${t.javaClass.simpleName}: ${t.message}",
                        t
                    )
                }
            }
        }
    }

    private fun applyTodayNutrition(todayNutrition: HomeTodayNutritionSummary) {
        _ui.update { current ->
            if (current.todayNutrition == todayNutrition) {
                current.copy(loading = false, error = null)
            } else {
                current.copy(
                    loading = false,
                    todayNutrition = todayNutrition,
                    error = null
                )
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        // Keep the existing Home content visible during foreground / back-navigation refreshes.
        // Clearing or visually re-entering loading state while a summary already exists is what makes
        // Home look like it flashes for one frame when returning from Settings / detail pages.
        _ui.update { current ->
            current.copy(
                loading = current.summary == null,
                error = null
            )
        }

        // ✅ 自動觸發：不 force（會被 1.5 秒 debounce 擋）
        refreshDailyActivity(force = false)

        val first = runCatching {
            withContext(Dispatchers.IO) { repo.loadSummaryFromServer().getOrThrow() }
        }

        if (first.isSuccess) {
            applySummary(first.getOrThrow())
            applyTodayNutrition(loadNutritionSummaryForDate(_ui.value.selectedDate))
            prefetchVisibleCalendarNutritionSummaries()
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
                    applyTodayNutrition(loadNutritionSummaryForDate(_ui.value.selectedDate))
                    prefetchVisibleCalendarNutritionSummaries()
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

    fun onCalendarDateSelected(localDate: LocalDate) {
        val today = todayLocalDate()
        if (localDate.isAfter(today)) return

        val safeDate = safeSelectableDate(localDate)
        val current = _ui.value
        if (current.selectedDate == safeDate) return

        selectedNutritionJob?.cancel()

        val cached = nutritionSummaryByDateCache[safeDate]
        _ui.update {
            it.copy(
                selectedDate = safeDate,
                todayNutrition = cached ?: HomeTodayNutritionSummary(),
                error = null,
                selectedDayOffset = (safeDate.toEpochDay() - today.toEpochDay()).toInt()
            )
        }

        if (cached != null) return

        selectedNutritionJob = viewModelScope.launch {
            val summary = loadNutritionSummaryForDate(safeDate)
            nutritionSummaryByDateCache[safeDate] = summary
            publishCalendarNutritionCache()

            if (_ui.value.selectedDate == safeDate) {
                applyTodayNutrition(summary)
            }
        }
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
                    fallbackToLastStableDailyState()
                }

                r.onSuccess { result ->
                    if (result.status != DailyActivityStatus.AVAILABLE_GRANTED) {
                        applyStableDailyState(
                            status = result.status,
                            steps = null,
                            activeKcal = null
                        )
                        return@onSuccess
                    }

                    val today = LocalDate.now(zoneId)
                    val todayRow = result.days.lastOrNull { it.localDate == today }

                    if (todayRow == null) {
                        applyStableDailyState(
                            status = DailyActivityStatus.NO_DATA,
                            steps = 0L,
                            activeKcal = 0
                        )
                    } else {
                        applyStableDailyState(
                            status = DailyActivityStatus.AVAILABLE_GRANTED,
                            steps = todayRow.steps,
                            activeKcal = todayRow.activeKcal
                        )
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                fallbackToLastStableDailyState()
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

    fun onRequestHealthPermissions() {
        refreshDailyActivity(force = true)
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
    // ✅ activeKcal 可能為 null：用 -1 當 key 的穩定 sentinel（不影響 UI 顯示）
    val activeKcalSafe = this.todayActivity.activeKcal?.toDouble()?.roundToInt() ?: -1

    return SummaryUiKey(
        avatarUrl = this.avatarUrl?.toString(),
        tdee = this.tdee,
        proteinG = this.proteinG,
        carbsG = this.carbsG,
        fatG = this.fatG,
        fiberG = this.fiberG,
        sugarG = this.sugarG,
        sodiumMg = this.sodiumMg,
        waterTodayMl = this.waterTodayMl,
        waterGoalMl = this.waterGoalMl,
        steps = this.todayActivity.steps,
        exerciseMinutes = this.todayActivity.exerciseMinutes,
        activeKcalInt = activeKcalSafe,
        fastingPlan = this.fastingPlan,
        weightDiffSigned = this.weightDiffSigned,
        weightDiffUnit = this.weightDiffUnit
    )
}
