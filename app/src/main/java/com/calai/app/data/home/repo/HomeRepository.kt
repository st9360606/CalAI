package com.calai.app.data.home.repo

import android.net.Uri
import com.calai.app.core.health.Gender
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.HealthInputs
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
    val weightDiffKg: Double?, // 現在 - 目標（正=需減）
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

    private fun toGender(s: String?): Gender =
        if (s.equals("MALE", true) || s.equals("male", true)) Gender.Male else Gender.Female

    private fun levelToBucket(level: String?): Int? = when (level?.lowercase()) {
        "sedentary"     -> 0
        "light"         -> 2
        "moderate"      -> 4
        "active"        -> 6
        "very_active",
        "very-active"   -> 7
        else            -> 0
    }

    /** 預設飲水：35 ml/kg，可被使用者自訂覆蓋 */
    private fun defaultWaterGoalMl(weightKg: Double?): Int =
        if (isValidWeight(weightKg)) (weightKg!!.times(35.0)).roundToInt() else 0

    // =============== 主流程 ===============
    suspend fun loadSummary(): HomeSummary {
        // 以 Server 為主，但僅在「有效」才採用；否則回落 Store，再不行用安全預設
        val server = runCatching { profileApi.getMyProfile() }.getOrNull()
        val snap = store.snapshot()

        val gender = toGender(server?.gender ?: snap.gender)

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

        // 體重差：現在 - 目標（正=需減）
        val diff = targetWeightKg?.let { (weightKg - it) }?.let { ((it * 10).roundToInt() / 10.0) }

        val activity = if (runCatching { hc.hasPermissions() }.getOrDefault(false)) {
            runCatching { hc.readToday() }.getOrDefault(TodayActivity(0, 0.0, 0))
        } else TodayActivity(0, 0.0, 0)

        val recent = runCatching { meals.loadRecent(10) }.getOrDefault(emptyList())

        // ★ 回寫本機（僅在數值有效時），確保下次啟動一致
        runCatching {
            if (isValidHeight(heightCm)) store.setHeightCm(heightCm.toInt())
            if (isValidWeight(weightKg)) store.setWeightKg(weightKg.toFloat())
            targetWeightKg?.let { if (isValidTargetWeight(it)) store.setTargetWeightKg(it.toFloat()) }
            snap.goal?.let { /* 保留本機 goal；若你要改以 server 覆蓋，也可以在此同步 */ }
            // 訓練頻率：若本機沒有值但 server 有，就落地為對應 bucket（0/2/4/6/7）
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
            weightDiffKg = diff,
            fastingPlan = snap.fastingPlan,
            todayActivity = activity,
            recentMeals = recent,
            avatarUrl = null // 如要顯示 Google 頭像，從你的 auth 狀態帶入 Uri
        )
    }

    suspend fun addWater(ml: Int) { store.addWaterToday(ml) }
    suspend fun setWater(ml: Int) { store.setWaterToday(ml) }
}
