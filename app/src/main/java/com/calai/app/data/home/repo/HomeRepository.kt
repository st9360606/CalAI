package com.calai.app.data.home.repo

import android.net.Uri
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

/** UI/DB 的合理範圍防呆（你後端 clamp 到 200000；這裡對齊） */
private const val DEFAULT_DAILY_STEP_GOAL = 10000
private const val MAX_DAILY_STEP_GOAL = 200000

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
    private fun isValidGoalWeight(v: Double?) = v != null && v in 20.0..800.0

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

    /** BMI label：以 DB 回傳 bmiClass 為主 */
    private fun bmiLabelFromDb(bmiClass: String?): String = when (bmiClass?.trim()?.uppercase()) {
        "UNDERWEIGHT" -> "Underweight"
        "NORMAL"      -> "Normal"
        "OVERWEIGHT"  -> "Overweight"
        "OBESITY"     -> "Obesity"
        else          -> "—"
    }

    suspend fun loadSummaryFromServer(): Result<HomeSummary> = runCatching {
        // 1) 以 Server/DB 為事實來源
        val p = profileApi.getMyProfile()

        // ✅ NEW：把 DB 的 dailyStepGoal 同步到 DataStore（讓 Home Steps 圓環吃到 DB）
        runCatching {
            val dbGoal = p.dailyStepGoal
            val safeGoal = when {
                dbGoal == null -> DEFAULT_DAILY_STEP_GOAL
                dbGoal <= 0 -> DEFAULT_DAILY_STEP_GOAL
                else -> dbGoal.coerceAtMost(MAX_DAILY_STEP_GOAL)
            }
            store.setDailyStepGoal(safeGoal)
        }

        // 2) 取本機快照（含體重單位、目標體重、斷食方案等）
        val local = store.snapshot()

        // 3) 使用者頭像（失敗不致命）
        val avatarUrl: Uri? = runCatching { usersApi.me() }
            .getOrNull()
            ?.picture
            ?.takeIf { !it.isNullOrBlank() }
            ?.let { Uri.parse(it) }

        // 4) 驗證（保留你原本 gating 邏輯：缺失就丟錯）
        val age = p.age?.takeIf { isValidAge(it) } ?: error("age missing/invalid")
        val heightCm = p.heightCm?.takeIf { isValidHeight(it) } ?: error("height missing/invalid")
        val weightKg = p.weightKg?.takeIf { isValidWeight(it) } ?: error("weight missing/invalid")

        val goalWeightKg = p.goalWeightKg?.takeIf { isValidGoalWeight(it) } // 可為空

        // =========================================================
        // ✅ 改動重點：Home 的宏量/卡路里以 DB(user_profiles) 為主
        // - 不再用 HealthCalc 在 client 端重算
        // =========================================================
        val tdee = (p.kcal ?: 0).coerceAtLeast(0)
        val proteinG = (p.proteinG ?: 0).coerceAtLeast(0)
        val carbsG = (p.carbsG ?: 0).coerceAtLeast(0)
        val fatG = (p.fatG ?: 0).coerceAtLeast(0)
        val fiberG = (p.fiberG ?: 0).coerceAtLeast(0)
        val sugarG = (p.sugarG ?: 0).coerceAtLeast(0)
        val sodiumMg = (p.sodiumMg ?: 0).coerceAtLeast(0)
        // ✅ BMI 也以 DB 為主（後端已做 timeseries 最新優先）
        val bmi = (p.bmi ?: 0.0)
        val bmiLabel = bmiLabelFromDb(p.bmiClass)

        // 5) 飲水目標與當日飲水（先沿用你的既有規則）
        val waterGoal = (p.waterMl ?: 0).coerceAtLeast(0)
        val waterNow = runCatching { store.waterTodayFlow.first() }.getOrDefault(0)

        // 6) 體重差：依「使用者當前選擇的單位」計算 Δ = goal - current
        val weightUnitPref = local.weightUnit ?: UserProfileStore.WeightUnit.KG
        val goalWeightUnitPref = local.goalWeightUnit ?: weightUnitPref

        // ---- 先決定 current 的「精確 kg」 ----
        val currentKgBase: Double = when (weightUnitPref) {
            UserProfileStore.WeightUnit.KG -> {
                local.weightKg?.toDouble()
                    ?: p.weightKg
                    ?: p.weightLbs?.times(KG_PER_LB)
                    ?: error("current weight missing for diff")
            }
            UserProfileStore.WeightUnit.LBS -> {
                val lbs: Double = local.weightLbs?.toDouble()
                    ?: p.weightLbs
                    ?: p.weightKg?.times(LBS_PER_KG)
                    ?: error("current weight missing for diff")
                lbs * KG_PER_LB
            }
        }

        // ---- 再決定 goal 的「精確 kg」（可能為 null） ----
        val goalKgBase: Double? = when (goalWeightUnitPref) {
            UserProfileStore.WeightUnit.KG -> {
                local.goalWeightKg?.toDouble()
                    ?: p.goalWeightKg
                    ?: p.goalWeightLbs?.times(KG_PER_LB)
            }
            UserProfileStore.WeightUnit.LBS -> {
                val lbs: Double? = local.goalWeightLbs?.toDouble()
                    ?: p.goalWeightLbs
                    ?: p.goalWeightKg?.times(LBS_PER_KG)
                lbs?.times(KG_PER_LB)
            }
        }

        val diffKgRaw = (goalKgBase?.minus(currentKgBase)) ?: 0.0

        val (weightDiffSigned, weightDiffUnit) =
            if (weightUnitPref == UserProfileStore.WeightUnit.KG) {
                floor1(diffKgRaw) to "kg"
            } else {
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
            goalWeightKg?.let { store.setGoalWeightKg(it.toFloat()) }
            levelToBucket(p.exerciseLevel)?.let { store.setExerciseFreqPerWeek(it) }
        }

        // 10) 組裝 HomeSummary（✅ 宏量/卡路里已改為 DB 為主）
        HomeSummary(
            tdee = tdee,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            fiberG = fiberG,
            sugarG = sugarG,
            sodiumMg = sodiumMg,
            bmi = bmi,
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
