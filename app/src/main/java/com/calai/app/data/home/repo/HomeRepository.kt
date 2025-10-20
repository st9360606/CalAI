package com.calai.app.data.home.repo

import android.net.Uri
import com.calai.app.core.health.Gender
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.HealthInputs
import com.calai.app.core.health.toCalcGender
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.health.TodayActivity
import com.calai.app.data.meals.repo.MealRepository
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.users.api.UsersApi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round
import kotlin.math.roundToInt

@Singleton
class HomeRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val usersApi: UsersApi,
    private val store: UserProfileStore,
    private val hc: HealthConnectRepository,
    private val meals: MealRepository
) {

    // ===== 驗證工具 =====
    private fun isValidAge(v: Int?) = v != null && v in 10..150
    private fun isValidHeight(v: Double?) = v != null && v in 80.0..350.0
    private fun isValidWeight(v: Double?) = v != null && v in 20.0..800.0
    private fun isValidTargetWeight(v: Double?) = v != null && v in 20.0..800.0

    private fun levelToBucket(level: String?): Int? = when (level?.lowercase()) {
        "sedentary"               -> 0
        "light"                   -> 2
        "moderate"                -> 4
        "active"                  -> 6
        "very_active", "very-active" -> 7
        else                      -> null
    }

    private fun defaultWaterGoalMl(weightKg: Double?): Int =
        if (isValidWeight(weightKg)) (weightKg!!.times(35.0)).roundToInt() else 0

    private fun kgToLbsInt(v: Double): Int = round(v * 2.2).toInt()
    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0

    /**
     * 以 Server 為唯一事實來源（SSOT）組裝 HomeSummary。
     * - 取不到或資料不完整 → throw（由 caller 決定 UI 表現，不落地預設值）。
     * - 僅在「值來自 Server 且有效」時，才回寫 DataStore 做快取。
     */
    suspend fun loadSummaryFromServer(): Result<HomeSummary> = runCatching {
        // 1) 取 Server Profile（必要）
        val p = profileApi.getMyProfile()

        // 2) 取使用者頭像（失敗不致命）
        val avatarUrl: Uri? = runCatching { usersApi.me() }
            .getOrNull()
            ?.picture
            ?.takeIf { !it.isNullOrBlank() }
            ?.let { Uri.parse(it) }

        // 3) 驗證與轉換（只使用 Server 值；缺失就丟錯）
        val gender: Gender = toCalcGender(p.gender)
        val age = p.age?.takeIf { isValidAge(it) } ?: error("age missing/invalid")
        val heightCm = p.heightCm?.takeIf { isValidHeight(it) } ?: error("height missing/invalid")
        val weightKg = p.weightKg?.takeIf { isValidWeight(it) } ?: error("weight missing/invalid")

        val targetWeightKg = p.targetWeightKg?.takeIf { isValidTargetWeight(it) } // 可為空
        val workouts = levelToBucket(p.exerciseLevel) ?: 0
        val goalKey = p.goal

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
            com.calai.app.core.health.BmiClass.Normal      -> "Normal"
            com.calai.app.core.health.BmiClass.Overweight  -> "Overweight"
            com.calai.app.core.health.BmiClass.Obesity     -> "Obesity"
        }

        val waterGoal = defaultWaterGoalMl(weightKg)
        val waterNow = runCatching { store.waterTodayFlow.first() }.getOrDefault(0)

        // 4) 體重差（優先 lbs，否則 kg）
        // === 體重差（用「當前 - 目標」；正=還需減，負=已低於目標） ===
        val (weightDiffSigned, weightDiffUnit) = run {
            val currentLbs = p.weightLbs
            if (currentLbs != null) {
                val targetLbs = p.targetWeightLbs ?: p.targetWeightKg?.let { kgToLbsInt(it) }
                val diff = if (targetLbs != null) (currentLbs - targetLbs).toDouble() else 0.0
                diff to "lbs"
            } else {
                val targetKg = p.targetWeightKg
                val diff = if (targetKg != null) round1(weightKg - targetKg) else 0.0
                diff to "kg"
            }
        }

        val activity = if (runCatching { hc.hasPermissions() }.getOrDefault(false)) {
            runCatching { hc.readToday() }.getOrDefault(TodayActivity(0, 0.0, 0))
        } else TodayActivity(0, 0.0, 0)

        val recent = runCatching { meals.loadRecent(10) }.getOrDefault(emptyList())

        // 5) 快取：僅在值源自 Server 且有效時回寫
        runCatching {
            store.setHeightCm(heightCm.toInt())
            store.setWeightKg(weightKg.toFloat())
            targetWeightKg?.let { store.setTargetWeightKg(it.toFloat()) }
            levelToBucket(p.exerciseLevel)?.let { store.setExerciseFreqPerWeek(it) }
        }

        HomeSummary(
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
            fastingPlan = store.snapshot().fastingPlan,
            todayActivity = activity,
            recentMeals = recent,
            avatarUrl = avatarUrl
        )
    }

    /** 新增飲水（會處理跨日重置，由 UserProfileStore 內部保證） */
    suspend fun addWater(ml: Int) {
        // 這裡不需要再切 Dispatcher；呼叫端可決定
        store.addWaterToday(ml)
    }

    /** 直接設定當日飲水值（非遞增） */
    suspend fun setWater(ml: Int) {
        store.setWaterToday(ml)
    }
}
