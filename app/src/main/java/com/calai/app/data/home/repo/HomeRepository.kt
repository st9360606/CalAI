package com.calai.app.data.home.repo

import android.net.Uri
import com.calai.app.core.health.Gender
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.HealthInputs
import com.calai.app.core.health.toCalcGender   // ★ 共用對應：ONLY "MALE" 為 Male，其餘視為 Female
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.health.TodayActivity
import com.calai.app.data.meals.api.MealItemDto
import com.calai.app.data.meals.repo.MealRepository
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.repo.UserProfileStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

data class HomeSummary(
    val tdee: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val bmi: Double,
    val bmiLabel: String,
    val waterGoalMl: Int,
    val waterTodayMl: Int,

    // ↓↓↓ 目標體重差（依你需求：若有 weight_lbs 則顯示 lbs；否則顯示 kg）
    // diffSigned：採「current - target」的有號差值（正=距離目標還需要減重；負=低於目標）
    val weightDiffSigned: Double,
    val weightDiffUnit: String, // "lbs" or "kg"

    val fastingPlan: String?,
    val todayActivity: TodayActivity,
    val recentMeals: List<MealItemDto>,
    val avatarUrl: Uri?
)

@Singleton
class HomeRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val store: UserProfileStore,
    private val hc: HealthConnectRepository,
    private val meals: MealRepository
) {
    // =============== 驗證 / 轉換工具 ===============
    private fun isValidAge(v: Int?) = v != null && v in 10..150
    private fun isValidHeight(v: Double?) = v != null && v in 80.0..350.0
    private fun isValidWeight(v: Double?) = v != null && v in 20.0..800.0
    private fun isValidTargetWeight(v: Double?) = v != null && v in 20.0..800.0

    private fun <T> preferValid(
        primary: T?,
        valid: (T?) -> Boolean,
        fallback: T?,
        default: T
    ): T = when {
        valid(primary) -> primary!!
        valid(fallback) -> fallback!!
        else -> default
    }

    // ★ 移除舊的 toGender，改成共用 toCalcGender（見 import）

    private fun levelToBucket(level: String?): Int? = when (level?.lowercase()) {
        "sedentary"     -> 0
        "light"         -> 2
        "moderate"      -> 4
        "active"        -> 6
        "very_active", "very-active" -> 7
        else            -> 0
    }

    /** 預設飲水：35 ml/kg，可被使用者自訂覆蓋 */
    private fun defaultWaterGoalMl(weightKg: Double?): Int =
        if (isValidWeight(weightKg)) (weightKg!!.times(35.0)).roundToInt() else 0

    // 換算：跟你 Onboarding 的邏輯一致（lbs 取整數）
    private fun kgToLbsInt(v: Double): Int = round(v * 2.2).toInt()
    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0

    // =============== 主流程 ===============
    suspend fun loadSummary(): HomeSummary {
        // 以 Server 為主，但僅在「有效」才採用；否則回落 Store，再不行用安全預設
        val server = runCatching { profileApi.getMyProfile() }.getOrNull()
        val snap = store.snapshot()

        // ★ 統一性別口徑：只有 "MALE" 算男性，其餘（含 OTHER）算女性
        val gender: Gender = toCalcGender(server?.gender ?: snap.gender)

        val age = preferValid(
            primary = server?.age,
            valid = ::isValidAge,
            fallback = snap.ageYears,
            default = 25
        )

        val heightCm = preferValid(
            primary = server?.heightCm,
            valid = ::isValidHeight,
            fallback = snap.heightCm?.toDouble(),
            default = 170.0
        )

        val weightKg = preferValid(
            primary = server?.weightKg,
            valid = ::isValidWeight,
            fallback = snap.weightKg?.toDouble(),
            default = 60.0
        )

        val targetWeightKg = run {
            val cand = server?.targetWeightKg ?: snap.targetWeightKg?.toDouble()
            if (isValidTargetWeight(cand)) cand else null
        }

        val workouts = snap.exerciseFreqPerWeek
            ?: levelToBucket(server?.exerciseLevel)
            ?: 0

        val goalKey = server?.goal?.takeIf { !it.isNullOrBlank() } ?: snap.goal

        val inputs = HealthInputs(
            gender = gender,
            age = age,
            heightCm = heightCm.toFloat(),
            weightKg = weightKg.toFloat(),
            workoutsPerWeek = workouts
        )

        val split = HealthCalc.splitForGoalKey(goalKey)
        val plan = HealthCalc.macroPlanBySplit(inputs, split)

        val bmiLabel = when (plan.bmiClass) {
            com.calai.app.core.health.BmiClass.Underweight -> "Underweight"
            com.calai.app.core.health.BmiClass.Normal -> "Normal"
            com.calai.app.core.health.BmiClass.Overweight -> "Overweight"
            com.calai.app.core.health.BmiClass.Obesity -> "Obesity"
        }

        // 飲水目標：優先使用自訂，否則用體重推算
        val waterGoal = snap.waterGoalMl ?: defaultWaterGoalMl(weightKg)
        if (waterGoal > 0) runCatching { store.setWaterGoalMl(waterGoal) }

        val waterNow = runCatching { store.waterTodayFlow.first() }.getOrDefault(0)

        // ===== 體重差（依你規則：若有 weight_lbs 就顯示 lbs；否則顯示 kg） =====
        // diffSigned = current - target（正=距離目標還需要減，負=低於目標）
        val (weightDiffSigned, weightDiffUnit) = run {
            val currentLbs = server?.weightLbs
            if (currentLbs != null) {
                // LBS 路徑
                val targetLbs = server.targetWeightLbs
                    ?: server.targetWeightKg?.let { kgToLbsInt(it) }
                val diff = if (targetLbs != null) {
                    (currentLbs - targetLbs).toDouble()
                } else {
                    0.0 // 沒有 target → 顯示 0 lbs
                }
                diff to "lbs"
            } else {
                // KG 路徑
                val currentKg = server?.weightKg ?: snap.weightKg?.toDouble() ?: weightKg
                val targetKg2 = server?.targetWeightKg ?: snap.targetWeightKg?.toDouble()
                val diff = if (targetKg2 != null) round1(currentKg - targetKg2) else 0.0
                diff to "kg"
            }
        }

        val activity = if (runCatching { hc.hasPermissions() }.getOrDefault(false)) {
            runCatching { hc.readToday() }.getOrDefault(TodayActivity(0, 0.0, 0))
        } else TodayActivity(0, 0.0, 0)

        val recent = runCatching { meals.loadRecent(10) }.getOrDefault(emptyList())

        // ★ 回寫本機（僅在數值有效時），確保下次啟動一致
        runCatching {
            if (isValidHeight(heightCm)) store.setHeightCm(heightCm.toInt())
            if (isValidWeight(weightKg)) store.setWeightKg(weightKg.toFloat())
            targetWeightKg?.let { if (isValidTargetWeight(it)) store.setTargetWeightKg(it.toFloat()) }
            if (snap.exerciseFreqPerWeek == null) {
                levelToBucket(server?.exerciseLevel)?.let { store.setExerciseFreqPerWeek(it) }
            }
        }

        return HomeSummary(
            tdee = plan.kcal,
            proteinG = plan.proteinGrams,
            carbsG = plan.carbsGrams,
            fatG = plan.fatGrams,
            bmi = plan.bmi,
            bmiLabel = bmiLabel,
            waterGoalMl = waterGoal,
            waterTodayMl = waterNow,
            weightDiffSigned = weightDiffSigned,
            weightDiffUnit = weightDiffUnit,
            fastingPlan = snap.fastingPlan,
            todayActivity = activity,
            recentMeals = recent,
            avatarUrl = null // 如要顯示 Google 頭像，從你的 auth 狀態帶入 Uri
        )
    }

    suspend fun addWater(ml: Int) { store.addWaterToday(ml) }
    suspend fun setWater(ml: Int) { store.setWaterToday(ml) }
}
