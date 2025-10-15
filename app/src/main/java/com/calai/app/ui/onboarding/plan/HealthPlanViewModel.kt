package com.calai.app.ui.onboarding.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.core.health.Gender
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.HealthInputs
import com.calai.app.core.health.MacroPlan
import com.calai.app.core.health.toCalcGender // ★ 共用的性別對應：只有 "MALE" 算 Male，其餘視為 Female
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

data class HealthPlanUiState(
    val loading: Boolean = true,
    val inputs: HealthInputs? = null,
    val plan: MacroPlan? = null,
    // 額外提供給 UI 顯示單位/目標差
    val weightUnit: UserProfileStore.WeightUnit? = null,
    val targetWeightKg: Float? = null,
    val targetWeightUnit: UserProfileStore.WeightUnit? = null
)

@HiltViewModel
class HealthPlanViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _ui = MutableStateFlow(HealthPlanUiState())
    val ui: StateFlow<HealthPlanUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // 先把五個欄位組成 HealthInputs（有型別化的 5 參數版本）
            val inputsFlow = combine(
                store.genderFlow,
                store.ageFlow,
                store.heightCmFlow,
                store.weightKgFlow,
                store.exerciseFreqPerWeekFlow
            ) { g, age, h, w, ex ->
                if (g == null || age == null || h == null || w == null || ex == null) {
                    null
                } else {
                    HealthInputs(
                        // ★ 性別統一轉換：只有 "MALE" 算男性，其餘 (FEMALE/OTHER/null) 一律算女性
                        gender = toCalcGender(g),
                        age = age,
                        heightCm = h.toFloat(),
                        weightKg = w,
                        workoutsPerWeek = ex
                    )
                }
            }.filterNotNull()

            // 依序把其他 Flow 串上（避免 6+ 參數的 combine vararg 型別不推斷）
            inputsFlow
                .combine(store.goalFlow) { inputs, goalKey ->
                    Pair(inputs, goalKey)
                }
                .combine(store.weightUnitFlow) { pair, weightUnit ->
                    Triple(pair.first, pair.second, weightUnit)
                }
                .combine(store.targetWeightKgFlow) { triple, targetKg ->
                    Combined(
                        inputs = triple.first,
                        goalKey = triple.second,
                        weightUnit = triple.third,
                        targetWeightKg = targetKg,
                        targetWeightUnit = null
                    )
                }
                .combine(store.targetWeightUnitFlow) { combined, targetUnit ->
                    combined.copy(targetWeightUnit = targetUnit)
                }
                .collect { combined ->
                    val split = HealthCalc.splitForGoalKey(combined.goalKey)
                    val plan = HealthCalc.macroPlanBySplit(combined.inputs, split)

                    _ui.value = HealthPlanUiState(
                        loading = false,
                        inputs = combined.inputs,
                        plan = plan,
                        weightUnit = combined.weightUnit,
                        targetWeightKg = combined.targetWeightKg,
                        targetWeightUnit = combined.targetWeightUnit
                    )
                }
        }
    }
}

private data class Combined(
    val inputs: HealthInputs,
    val goalKey: String?,
    val weightUnit: UserProfileStore.WeightUnit?,
    val targetWeightKg: Float?,
    val targetWeightUnit: UserProfileStore.WeightUnit?
)
