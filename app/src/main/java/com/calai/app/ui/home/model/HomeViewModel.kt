package com.calai.app.ui.home.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.activity.model.DailyActivityStatus
import com.calai.app.data.activity.sync.DailyActivitySyncer
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.home.repo.HomeRepository
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.ZoneId
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
    private val hc: HealthConnectRepository,          // 目前未用到，可保留
    private val profileRepo: ProfileRepository,       // 401/404 自動補救
    private val dailySyncer: DailyActivitySyncer,     // ✅ 串 daily activity
    private val profileStore: UserProfileStore,
    private val zoneId: ZoneId                        // ✅ 用裝置當下 timezone
) : ViewModel() {

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

    // ====== Summary key：沒變就不要 emit，避免整頁重組 ======
    private var lastSummaryKey: SummaryUiKey? = null

    // ✅ NEW：避免重複同步（回前景/refresh 連發）
    private var refreshDailyJob: Job? = null

    // ✅ 1.5 秒 debounce（只擋「自動連發」，手動/授權要能 bypass）
    private val dailyDebounceMs: Long = 1_500L
    private var lastDailyRefreshAtMs: Long = 0L

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

    init {
        refresh()

        viewModelScope.launch {
            profileStore.dailyStepGoalFlow.collectLatest { v ->
                _dailyStepGoal.value = v.toLong()
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
     * - 未授權/不可用：目前也先導 Play Store（你註解：之後換成 app 內 Intro/授權頁更好）
     */
    fun onDailyCtaClick(ctx: Context) {
        when (_dailyStatus.value) {
            DailyActivityStatus.HC_NOT_INSTALLED -> openPlayStore(ctx, "com.google.android.apps.healthdata")

            DailyActivityStatus.NO_DATA -> openHealthConnectOrStore(ctx) // ✅ NEW：比 refresh 更有用

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
            else -> runCatching { ctx.startActivity(market) }.recoverCatching { ctx.startActivity(web) }
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
