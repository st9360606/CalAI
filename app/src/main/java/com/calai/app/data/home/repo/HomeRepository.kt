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
import kotlin.math.floor
import kotlin.math.roundToInt

// ======== 體重換算常數與工具 ========

// 比 1kg = 2.2lbs 更精準的常數
private const val KG_PER_LB = 0.45359237
private const val LBS_PER_KG = 1.0 / KG_PER_LB

/** 無條件捨去到一位小數 */
private fun floor1(v: Double): Double =
    floor(v * 10.0 + 1e-8) / 10.0

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
        "sedentary"                  -> 0
        "light"                      -> 2
        "moderate"                   -> 4
        "active"                     -> 6
        "very_active", "very-active" -> 7
        else                         -> null
    }

    private fun defaultWaterGoalMl(weightKg: Double?): Int =
        if (isValidWeight(weightKg)) (weightKg!!.times(35.0)).roundToInt() else 0

    suspend fun loadSummaryFromServer(): Result<HomeSummary> = runCatching {
        // 1) 以 Server 為事實來源
        val p = profileApi.getMyProfile()

        // 2) 取本機快照（含體重單位、目標體重、斷食方案等）
        val local = store.snapshot()

        // 3) 使用者頭像（失敗不致命）
        val avatarUrl: Uri? = runCatching { usersApi.me() }
            .getOrNull()
            ?.picture
            ?.takeIf { !it.isNullOrBlank() }
            ?.let { Uri.parse(it) }

        // 4) 驗證與轉換（只使用 Server 值；缺失就丟錯）
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

        // 5) 飲水目標與當日飲水
        val waterGoal = defaultWaterGoalMl(weightKg)
        val waterNow = runCatching { store.waterTodayFlow.first() }.getOrDefault(0)

        // 6) 體重差：依「使用者當前選擇的單位」計算 Δ = target - current
        val weightUnitPref = local.weightUnit ?: UserProfileStore.WeightUnit.KG
        val targetWeightUnitPref = local.targetWeightUnit ?: weightUnitPref

        // ---- 先決定 current 的「精確 kg」 ----
        val currentKgBase: Double = when (weightUnitPref) {
            UserProfileStore.WeightUnit.KG -> {
                // 優先用本機 kg（使用者剛輸入），沒有再用 Server 的 kg，再不行用 Server 的 lbs 換算
                local.weightKg?.toDouble()
                    ?: p.weightKg
                    ?: p.weightLbs?.times(KG_PER_LB)
                    ?: error("current weight missing for diff")
            }
            UserProfileStore.WeightUnit.LBS -> {
                // 優先用本機 lbs，沒有再用 Server lbs，再不行用 Server kg 換算成 lbs
                val lbs: Double = local.weightLbs?.toDouble()
                    ?: p.weightLbs
                    ?: p.weightKg?.times(LBS_PER_KG)
                    ?: error("current weight missing for diff")
                lbs * KG_PER_LB
            }
        }

        // ---- 再決定 target 的「精確 kg」（可能為 null） ----
        val targetKgBase: Double? = when (targetWeightUnitPref) {
            UserProfileStore.WeightUnit.KG -> {
                local.targetWeightKg?.toDouble()
                    ?: p.targetWeightKg
                    ?: p.targetWeightLbs?.times(KG_PER_LB)
            }
            UserProfileStore.WeightUnit.LBS -> {
                val lbs: Double? = local.targetWeightLbs?.toDouble()
                    ?: p.targetWeightLbs
                    ?: p.targetWeightKg?.times(LBS_PER_KG)
                lbs?.times(KG_PER_LB)
            }
        }

        val diffKgRaw = (targetKgBase?.minus(currentKgBase)) ?: 0.0

        val (weightDiffSigned, weightDiffUnit) =
            if (weightUnitPref == UserProfileStore.WeightUnit.KG) {
                // 顯示 kg：直接對 kg 差值做 0.1 無條件捨去
                floor1(diffKgRaw) to "kg"
            } else {
                // 顯示 lbs：先把 kg 差值換成 lbs，再做 0.1 無條件捨去
                floor1(diffKgRaw * LBS_PER_KG) to "lbs"
            }

        // 7) 今日活動（Health Connect）
        val activity = if (runCatching { hc.hasPermissions() }.getOrDefault(false)) {
            runCatching { hc.readToday() }.getOrDefault(TodayActivity(0, 0.0, 0))
        } else TodayActivity(0, 0.0, 0)

        // 8) 最近餐點
        val recent = runCatching { meals.loadRecent(10) }.getOrDefault(emptyList())

        // 9) 將部分 Server 值回寫 DataStore 作為快取（但 SSOT 仍是 Server）
        runCatching {
            store.setHeightCm(heightCm.toFloat())
            store.setWeightKg(weightKg.toFloat())
            targetWeightKg?.let { store.setTargetWeightKg(it.toFloat()) }
            levelToBucket(p.exerciseLevel)?.let { store.setExerciseFreqPerWeek(it) }
        }

        // 10) 組裝 HomeSummary
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
            fastingPlan = local.fastingPlan,
            todayActivity = activity,
            recentMeals = recent,
            avatarUrl = avatarUrl
        )
    }

    /** 新增飲水（會處理跨日重置，由 UserProfileStore 內部保證） */
    suspend fun addWater(ml: Int) {
        store.addWaterToday(ml)
    }

    /** 直接設定當日飲水值（非遞增） */
    suspend fun setWater(ml: Int) {
        store.setWaterToday(ml)
    }
}
