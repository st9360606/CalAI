package com.calai.app.ui.home.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.activity.model.DailyActivityStatus
import com.calai.app.data.activity.sync.DailyActivitySyncer
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.home.repo.HomeRepository
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.data.profile.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
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
    private val profileRepo: ProfileRepository, // ★ 新增：用於 401/404 自動補救
    private val dailySyncer: DailyActivitySyncer, // ✅ NEW：串 daily activity
    private val zoneId: ZoneId                   // ✅ NEW：用裝置當下 timezone
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    // ====== ✅ NEW：提供給 HomeScreen 的 override ======
    private val _dailyStatus = MutableStateFlow(DailyActivityStatus.ERROR_RETRYABLE)
    val dailyStatus: StateFlow<DailyActivityStatus> = _dailyStatus.asStateFlow()

    private val _dailyStepsToday = MutableStateFlow<Long?>(null)
    val dailyStepsToday: StateFlow<Long?> = _dailyStepsToday.asStateFlow()

    private val _dailyActiveKcalToday = MutableStateFlow<Int?>(null)
    val dailyActiveKcalToday: StateFlow<Int?> = _dailyActiveKcalToday.asStateFlow()


    // ====== Summary key：真的要減少重組就要「沒變就不要 emit」 ======
    private var lastSummaryKey: SummaryUiKey? = null

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }

        // ✅ daily activity 可以並行，不必等 summary
        launch { refreshDailyActivity() }

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

    // ✅ 簡化：避免 withContext 推斷問題
    fun onAddWater(ml: Int) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { repo.addWater(ml) } }
        refresh()
    }

    /**
     * ✅ NEW：今天活動同步（依你規格）
     * - 今天 timezone：用裝置當下 zoneId
     * - 多來源：Google Fit > Samsung Health > on-device steps（你 Syncer 已做）
     * - status 不可用/未授權：UI 顯示降級，不顯示舊 server 值
     */
    fun refreshDailyActivity() = viewModelScope.launch {
        try {
            val r = withContext(Dispatchers.IO) { dailySyncer.syncLast7DaysWithStatus(zoneId) }

            r.onFailure { t ->
                if (t is CancellationException) throw t
                _dailyStatus.value = DailyActivityStatus.ERROR_RETRYABLE
                _dailyStepsToday.value = null
                _dailyActiveKcalToday.value = null
            }

            r.onSuccess { result ->
                _dailyStatus.value = result.status

                // ✅ 只要不是 AVAILABLE_GRANTED，一律不要顯示數字（避免誤導）
                if (result.status != DailyActivityStatus.AVAILABLE_GRANTED) {
                    _dailyStepsToday.value = null
                    _dailyActiveKcalToday.value = null
                    return@onSuccess
                }

                val today = LocalDate.now(zoneId)
                val todayRow = result.days.lastOrNull { it.localDate == today }

                if (todayRow == null) {
                    _dailyStatus.value = DailyActivityStatus.NO_DATA
                    _dailyStepsToday.value = null
                    _dailyActiveKcalToday.value = null
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


    /**
     * ✅ NEW：卡片 CTA 點擊（能跑先行版）
     * - 未安裝：導 Play Store
     * - 未授權/不可用：導到你的 HealthConnectIntroScreen（更好）
     *
     * 目前你 UI 是呼叫 vm.onDailyCtaClick(ctx)，所以先做「直接開連結」最小可用。
     * 後續建議改成：VM emit UiEvent，由 NavHost 導到 Routes.ONBOARD_HEALTH_CONNECT（見風險&改良）
     */
    fun onDailyCtaClick(ctx: Context) {
        val st = _dailyStatus.value
        when (st) {
            DailyActivityStatus.HC_NOT_INSTALLED -> openPlayStore(ctx, "com.google.android.apps.healthdata")
            DailyActivityStatus.PERMISSION_NOT_GRANTED,
            DailyActivityStatus.HC_UNAVAILABLE,
            DailyActivityStatus.ERROR_RETRYABLE,
            DailyActivityStatus.NO_DATA -> {
                // 最小可用：先導到 Play Store（或你也可以導到你自己的 HealthConnectIntro route）
                openPlayStore(ctx, "com.google.android.apps.healthdata")
            }
            DailyActivityStatus.AVAILABLE_GRANTED -> Unit
        }
    }

    private fun openPlayStore(ctx: Context, pkg: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(market) }.recoverCatching { ctx.startActivity(web) }
    }


    fun onRequestHealthPermissions() = viewModelScope.launch { refresh() }


    fun refreshAfterLogin() {
        viewModelScope.launch {
            // TODO: 重新拉 summary / profile / macros
            // repo.refreshSummary()
            // repo.refreshPlanMetrics()
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
