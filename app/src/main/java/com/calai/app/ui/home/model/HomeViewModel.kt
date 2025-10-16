package com.calai.app.ui.home.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.home.repo.HomeRepository
import com.calai.app.data.home.repo.HomeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val steps: Long,                 // ← Long（修正）
    val exerciseMinutes: Long,       // ← Long（修正）
    val activeKcalInt: Int,          // 仍量化成 Int，避免小數級抖動
    val fastingPlan: String?,
    val weightDiffSigned: Double,    // ← Double（修正）
    val weightDiffUnit: String,
    val recentMealsSig: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val hc: HealthConnectRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    // 上一次已送出的 summary 簽章（用於去重）
    private var lastSummaryKey: SummaryUiKey? = null

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)

        runCatching {
            withContext(Dispatchers.IO) { repo.loadSummary() }
        }.onSuccess { summary ->
            val newKey = summary.toUiKey()
            val firstTime = _ui.value.summary == null
            val changed = newKey != lastSummaryKey

            if (firstTime || changed) {
                lastSummaryKey = newKey
                _ui.value = HomeUiState(
                    loading = false,
                    summary = summary,
                    error = null,
                    selectedDayOffset = _ui.value.selectedDayOffset
                )
            } else {
                if (_ui.value.loading) {
                    _ui.value = _ui.value.copy(loading = false)
                }
            }
        }.onFailure { t ->
            _ui.value = _ui.value.copy(loading = false, error = t.message)
        }
    }

    fun onAddWater(ml: Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.addWater(ml) }
        refresh()
    }

    fun onRequestHealthPermissions() = viewModelScope.launch {
        refresh()
    }

    init { refresh() }
}

/* -------------------- 私有工具 -------------------- */

private fun HomeSummary.toUiKey(): SummaryUiKey {
    val rm = this.recentMeals
    val recentSig = buildString {
        append(rm.size)
        rm.take(3).forEach { m ->
            append('|').append(m.hashCode())
        }
    }
    return SummaryUiKey(
        avatarUrl = this.avatarUrl?.toString(),
        tdee = this.tdee,
        proteinG = this.proteinG,
        carbsG = this.carbsG,
        fatG = this.fatG,
        waterTodayMl = this.waterTodayMl,
        waterGoalMl = this.waterGoalMl,
        steps = this.todayActivity.steps,                       // Long
        exerciseMinutes = this.todayActivity.exerciseMinutes,   // Long
        activeKcalInt = this.todayActivity.activeKcal.toInt(),  // 量化
        fastingPlan = this.fastingPlan,
        weightDiffSigned = this.weightDiffSigned,               // Double
        weightDiffUnit = this.weightDiffUnit,
        recentMealsSig = recentSig
    )
}
