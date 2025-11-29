package com.calai.app.ui.onboarding.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.HealthInputs
import com.calai.app.core.health.MacroPlan
import com.calai.app.core.health.toCalcGender // ★ 共用的性別對應：只有 "MALE" 算 Male，其餘視為 Female
import com.calai.app.data.healthplan.api.SaveHealthPlanRequest
import com.calai.app.data.healthplan.repo.HealthPlanRepository
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.kgToLbs1
import com.calai.app.data.profile.repo.lbsToKg1
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.calai.app.core.health.Gender
import kotlin.math.roundToInt

data class HealthPlanUiState(
    val loading: Boolean = true,
    val inputs: HealthInputs? = null,
    val plan: MacroPlan? = null,

    // 原本就有的欄位（保留）
    val weightUnit: UserProfileStore.WeightUnit? = null,
    val targetWeightKg: Float? = null,
    val targetWeightUnit: UserProfileStore.WeightUnit? = null,

    // ★ 新增：給 UI 顯示用（已依 displayUnit 轉成 kg 或 lbs）
    val weightDisplay: Float? = null,
    val targetWeightDisplay: Float? = null,
    val displayUnit: UserProfileStore.WeightUnit? = null,

    // ✅ 新增：存後端用
    val goalKey: String? = null
)

@HiltViewModel
class HealthPlanViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val repo: HealthPlanRepository
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
                    inputs to goalKey
                }
                .combine(store.weightUnitFlow) { (inputs, goalKey), weightUnit ->
                    Triple(inputs, goalKey, weightUnit)
                }
                .combine(store.weightKgFlow) { triple, weightKg ->
                    // 這裡 weightKg 就是 DataStore 裡的 kg buffer
                    CombinedInputs(
                        inputs = triple.first,
                        goalKey = triple.second,
                        weightUnit = triple.third,
                        weightKg = weightKg
                    )
                }
                .combine(store.weightLbsFlow) { combined, weightLbs ->
                    combined.copy(weightLbs = weightLbs)
                }
                .combine(store.targetWeightKgFlow) { combined, targetKg ->
                    combined.copy(targetWeightKg = targetKg)
                }
                .combine(store.targetWeightLbsFlow) { combined, targetLbs ->
                    combined.copy(targetWeightLbs = targetLbs)
                }
                .combine(store.targetWeightUnitFlow) { combined, targetUnit ->
                    combined.copy(targetWeightUnit = targetUnit)
                }
                .collect { c ->
                    // === 決定顯示單位 ===
                    val unit = c.weightUnit ?: UserProfileStore.WeightUnit.KG
                    val displayUnit = unit // 目前直接沿用，可依需求改成「若 current null 就看 targetUnit」

                    // === 目前體重顯示值（已依 displayUnit 換算） ===
                    val weightDisplay: Float? = when (displayUnit) {
                        UserProfileStore.WeightUnit.LBS ->
                            c.weightLbs
                                ?: c.weightKg?.let { kg ->
                                    kgToLbs1(kg.toDouble()).toFloat()
                                }

                        UserProfileStore.WeightUnit.KG ->
                            c.weightKg
                                ?: c.weightLbs?.let { lbs ->
                                    lbsToKg1(lbs.toDouble()).toFloat()
                                }
                    }

                    // === 目標體重顯示值（已依 displayUnit 換算） ===
                    val targetDisplay: Float? = when (displayUnit) {
                        UserProfileStore.WeightUnit.LBS ->
                            c.targetWeightLbs
                                ?: c.targetWeightKg?.let { kg ->
                                    kgToLbs1(kg.toDouble()).toFloat()
                                }

                        UserProfileStore.WeightUnit.KG ->
                            c.targetWeightKg
                                ?: c.targetWeightLbs?.let { lbs ->
                                    lbsToKg1(lbs.toDouble()).toFloat()
                                }
                    }

                    val split = HealthCalc.splitForGoalKey(c.goalKey)
                    val plan = HealthCalc.macroPlanBySplit(c.inputs, split)

                    _ui.value = HealthPlanUiState(
                        loading = false,
                        inputs = c.inputs,
                        plan = plan,
                        weightUnit = unit,
                        targetWeightKg = c.targetWeightKg,
                        targetWeightUnit = c.targetWeightUnit,
                        weightDisplay = weightDisplay,
                        targetWeightDisplay = targetDisplay,
                        displayUnit = displayUnit
                    )
                }
        }
    }

    /**
     * ✅ Start 先存再跳頁：失敗/未登入都不擋（會降級存 pending）
     */
    suspend fun savePlanBestEffort(source: String = "ONBOARDING") {
        val snapshot = ui.value
        val inputs = snapshot.inputs ?: return
        val plan = snapshot.plan ?: return

        // ✅ 關鍵：goalKey 用 snapshot → 不行就用 store 再撈一次
        val goalKeySafe: String? =
            snapshot.goalKey
                ?: runCatching { store.goal() }.getOrNull()

        val waterMl = (35f * inputs.weightKg).roundToInt()
        val unitPref = (snapshot.displayUnit ?: snapshot.weightUnit ?: UserProfileStore.WeightUnit.KG).name

        val genderStr = when (inputs.gender) {
            Gender.Male -> "MALE"
            Gender.Female -> "FEMALE"
        }

        android.util.Log.d("HealthPlan", "savePlanBestEffort goalKeySafe=$goalKeySafe")

        val req = SaveHealthPlanRequest(
            source = source,
            calcVersion = "healthcalc_v1",
            goalKey = goalKeySafe, // ✅ 改這裡
            gender = genderStr,
            age = inputs.age,
            heightCm = inputs.heightCm.toDouble(),
            weightKg = inputs.weightKg.toDouble(),
            targetWeightKg = snapshot.targetWeightKg?.toDouble(),
            unitPreference = unitPref,
            workoutsPerWeek = inputs.workoutsPerWeek,
            kcal = plan.kcal,
            carbsG = plan.carbsGrams,
            proteinG = plan.proteinGrams,
            fatG = plan.fatGrams,
            waterMl = waterMl,
            bmi = plan.bmi,
            bmiClass = plan.bmiClass.name
        )
        repo.upsertBestEffort(req)
    }
}

/**
 * 用來把所有相關 Flow 串起來的中繼資料結構。
 * - weightKg / weightLbs：目前體重（兩種單位）
 * - targetWeightKg / targetWeightLbs：目標體重（兩種單位）
 * - weightUnit / targetWeightUnit：使用者偏好的單位
 */
private data class CombinedInputs(
    val inputs: HealthInputs,
    val goalKey: String?,
    val weightUnit: UserProfileStore.WeightUnit?,
    val weightKg: Float?,

    val weightLbs: Float? = null,
    val targetWeightKg: Float? = null,
    val targetWeightLbs: Float? = null,
    val targetWeightUnit: UserProfileStore.WeightUnit? = null
)
