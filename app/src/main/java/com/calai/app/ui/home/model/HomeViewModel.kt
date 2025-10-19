package com.calai.app.ui.home.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.home.repo.HomeRepository
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.data.profile.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

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
    private val profileRepo: ProfileRepository // ★ 新增：用於 401/404 自動補救
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    private var lastSummaryKey: SummaryUiKey? = null

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)

        // 第一次嘗試：Server 為唯一事實來源
        val first = runCatching {
            withContext(Dispatchers.IO) { repo.loadSummaryFromServer().getOrThrow() }
        }

        if (first.isSuccess) {
            applySummary(first.getOrThrow())
            return@launch
        }

        // 若為 401/404：補救（建立或同步一份 Server Profile），再重試一次
        val e = first.exceptionOrNull()
        val code = (e as? HttpException)?.code()
        val recoverable = code == 401 || code == 404
        if (recoverable) {
            val ok = withContext(Dispatchers.IO) {
                profileRepo.upsertFromLocal().isSuccess
            }
            if (ok) {
                val second = runCatching {
                    withContext(Dispatchers.IO) { repo.loadSummaryFromServer().getOrThrow() }
                }
                if (second.isSuccess) {
                    applySummary(second.getOrThrow())
                    return@launch
                } else {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = second.exceptionOrNull()?.message ?: "Failed after recovery"
                    )
                    return@launch
                }
            }
        }

        // 其它錯誤：顯示錯誤訊息
        _ui.value = _ui.value.copy(
            loading = false,
            error = e?.message ?: "Failed to load profile"
        )
    }

    private fun applySummary(summary: HomeSummary) {
        val newKey = summary.toUiKey()
        val firstTime = _ui.value.summary == null
        val changed = newKey != lastSummaryKey
        lastSummaryKey = newKey

        _ui.value = HomeUiState(
            loading = false,
            summary = summary,
            error = null,
            selectedDayOffset = _ui.value.selectedDayOffset
        )

        // 若只是 Loading 結束，但內容沒變，也要把 loading 關掉
        if (!firstTime && !changed && _ui.value.loading) {
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    // ✅ 簡化：避免 withContext 推斷問題
    fun onAddWater(ml: Int) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { repo.addWater(ml) } }
        refresh()
    }

    fun onRequestHealthPermissions() = viewModelScope.launch { refresh() }

    init { refresh() }
}

/* -------------------- 私有工具 -------------------- */

private fun HomeSummary.toUiKey(): SummaryUiKey {
    val rm = this.recentMeals
    val recentSig = buildString {
        append(rm.size)
        rm.take(3).forEach { m -> append('|').append(m.hashCode()) }
    }
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
        activeKcalInt = this.todayActivity.activeKcal.toInt(),
        fastingPlan = this.fastingPlan,
        weightDiffSigned = this.weightDiffSigned,
        weightDiffUnit = this.weightDiffUnit,
        recentMealsSig = recentSig
    )
}
