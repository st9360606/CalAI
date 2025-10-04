package com.calai.app.ui.onboarding.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.core.health.Gender
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.HealthInputs
import com.calai.app.core.health.MacroPlan
import com.calai.app.data.auth.store.UserProfileStore
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
    val plan: MacroPlan? = null
)

@HiltViewModel
class HealthPlanViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _ui = MutableStateFlow(HealthPlanUiState())
    val ui: StateFlow<HealthPlanUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // 先把五個欄位組成 HealthInputs（這個 combine 有型別化的 5 參數版本）
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
                        gender = if (g.equals("female", true)) Gender.Female else Gender.Male,
                        age = age,
                        heightCm = h.toFloat(),
                        weightKg = w,
                        workoutsPerWeek = ex
                    )
                }
            }.filterNotNull()

            // 再與 goalFlow 做第二次 combine → 任何一邊變更都即時重算
            inputsFlow
                .combine(store.goalFlow) { inputs, goalKey ->
                    val split = HealthCalc.splitForGoalKey(goalKey)
                    val plan = HealthCalc.macroPlanBySplit(inputs, split)
                    HealthPlanUiState(
                        loading = false,
                        inputs = inputs,
                        plan = plan
                    )
                }
                .collect { state -> _ui.value = state }
        }
    }
}
