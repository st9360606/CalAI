package com.calai.app.data.profile.repo

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")

@Singleton
class UserProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class HeightUnit { CM, FT_IN }
    enum class WeightUnit { KG, LBS }

    private object Keys {
        val GENDER = stringPreferencesKey("gender")
        val REFERRAL_SOURCE = stringPreferencesKey("referral_source")
        val AGE_YEARS = intPreferencesKey("age_years")

        // Height
        val HEIGHT = floatPreferencesKey("height_cm")
        val HEIGHT_UNIT = stringPreferencesKey("height_unit")
        val HEIGHT_FEET = intPreferencesKey("height_feet")
        val HEIGHT_INCHES = intPreferencesKey("height_inches")

        // Weight (metric + imperial)
        val WEIGHT = floatPreferencesKey("weight_kg")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val WEIGHT_LBS = floatPreferencesKey("weight_lbs")

        // Goal Weight (metric + imperial)
        val GOAL_WEIGHT = floatPreferencesKey("goal_weight_kg")
        val GOAL_WEIGHT_UNIT = stringPreferencesKey("goal_weight_unit")
        val GOAL_WEIGHT_LBS = floatPreferencesKey("goal_weight_lbs")

        val EXERCISE_FREQ_PER_WEEK = intPreferencesKey("exercise_freq_per_week")
        val GOAL = stringPreferencesKey("goal")
        val LOCALE_TAG = stringPreferencesKey("locale_tag")
        val HAS_SERVER_PROFILE = booleanPreferencesKey("has_server_profile")

        // Fasting / Water
        val FASTING_PLAN = stringPreferencesKey("fasting_plan")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val WATER_TODAY_DATE = stringPreferencesKey("water_today_date")
        val WATER_TODAY_ML = intPreferencesKey("water_today_ml")

        // Daily Step Goal
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
    }

    private companion object {
        const val DEFAULT_DAILY_STEP_GOAL = 10000
        const val MAX_DAILY_STEP_GOAL = 200000

        // ✅ NEW：UI 預設值（不代表 DB 真實值）
        const val DEFAULT_WATER_GOAL_ML = 2000
        const val MAX_WATER_GOAL_ML = 20000
    }

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private fun todayStr(): String = LocalDate.now().format(dateFmt)

    // 本機 LBS 防呆（避免太誇張的值）
    private fun clampLbsForStore(v: Float): Float =
        v.coerceIn(0f, 900f)

    // ======= 性別 =======
    suspend fun setGender(value: String) {
        context.userProfileDataStore.edit { it[Keys.GENDER] = value }
    }

    suspend fun gender(): String? =
        context.userProfileDataStore.data.map { it[Keys.GENDER] }.first()

    val genderFlow: Flow<String?> =
        context.userProfileDataStore.data.map { it[Keys.GENDER] }

    // ======= 推薦來源 =======
    suspend fun setReferralSource(value: String) {
        context.userProfileDataStore.edit { it[Keys.REFERRAL_SOURCE] = value }
    }

    suspend fun referralSource(): String? =
        context.userProfileDataStore.data.map { it[Keys.REFERRAL_SOURCE] }.first()

    val referralSourceFlow: Flow<String?> =
        context.userProfileDataStore.data.map { it[Keys.REFERRAL_SOURCE] }

    // ======= 年齡 =======
    val ageFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.AGE_YEARS] }

    suspend fun setAge(years: Int) {
        context.userProfileDataStore.edit { it[Keys.AGE_YEARS] = years }
    }

    // ======= 身高（cm + ft/in） =======
    val heightCmFlow: Flow<Float?> =
        context.userProfileDataStore.data.map { it[Keys.HEIGHT] }

    suspend fun setHeightCm(cm: Float) {
        context.userProfileDataStore.edit { it[Keys.HEIGHT] = cm }
    }

    val heightUnitFlow: Flow<HeightUnit?> =
        context.userProfileDataStore.data.map { p ->
            p[Keys.HEIGHT_UNIT]?.let { runCatching { HeightUnit.valueOf(it) }.getOrNull() }
        }

    suspend fun setHeightUnit(unit: HeightUnit) {
        context.userProfileDataStore.edit { it[Keys.HEIGHT_UNIT] = unit.name }
    }

    val heightFeetFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.HEIGHT_FEET] }

    val heightInchesFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.HEIGHT_INCHES] }

    suspend fun setHeightImperial(feet: Int, inches: Int) {
        context.userProfileDataStore.edit {
            it[Keys.HEIGHT_FEET] = feet.coerceIn(0, 8)
            it[Keys.HEIGHT_INCHES] = inches.coerceIn(0, 11)
        }
    }

    suspend fun clearHeightImperial() {
        context.userProfileDataStore.edit {
            it.remove(Keys.HEIGHT_FEET)
            it.remove(Keys.HEIGHT_INCHES)
        }
    }

    // ======= 現在體重（kg + lbs） =======
    val weightKgFlow: Flow<Float?> =
        context.userProfileDataStore.data.map { it[Keys.WEIGHT] }

    suspend fun setWeightKg(kg: Float) {
        context.userProfileDataStore.edit { it[Keys.WEIGHT] = kg }
    }

    val weightUnitFlow: Flow<WeightUnit?> =
        context.userProfileDataStore.data.map { p ->
            p[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
        }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.userProfileDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    // ======= 現在體重 LBS =======
    val weightLbsFlow: Flow<Float?> =
        context.userProfileDataStore.data.map { it[Keys.WEIGHT_LBS] }

    suspend fun setWeightLbs(lbs: Float) {
        context.userProfileDataStore.edit {
            it[Keys.WEIGHT_LBS] = clampLbsForStore(lbs)
        }
    }

    suspend fun clearWeightLbs() {
        context.userProfileDataStore.edit { it.remove(Keys.WEIGHT_LBS) }
    }

    // ======= 目標體重（kg + lbs） =======
    val goalWeightKgFlow: Flow<Float?> =
        context.userProfileDataStore.data.map { it[Keys.GOAL_WEIGHT] }

    suspend fun setGoalWeightKg(kg: Float) {
        context.userProfileDataStore.edit { it[Keys.GOAL_WEIGHT] = kg }
    }

    val goalWeightUnitFlow: Flow<WeightUnit?> =
        context.userProfileDataStore.data.map { p ->
            p[Keys.GOAL_WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
        }

    suspend fun setGoalWeightUnit(unit: WeightUnit) {
        context.userProfileDataStore.edit { it[Keys.GOAL_WEIGHT_UNIT] = unit.name }
    }

    // ======= 目標體重 LBS =======
    val goalWeightLbsFlow: Flow<Float?> =
        context.userProfileDataStore.data.map { it[Keys.GOAL_WEIGHT_LBS] }

    suspend fun setGoalWeightLbs(lbs: Float) {
        context.userProfileDataStore.edit {
            it[Keys.GOAL_WEIGHT_LBS] = clampLbsForStore(lbs)
        }
    }

    suspend fun clearGoalWeightLbs() {
        context.userProfileDataStore.edit { it.remove(Keys.GOAL_WEIGHT_LBS) }
    }

    // ======= 鍛鍊頻率 =======
    val exerciseFreqPerWeekFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.EXERCISE_FREQ_PER_WEEK] }

    suspend fun setExerciseFreqPerWeek(v: Int) {
        context.userProfileDataStore.edit {
            it[Keys.EXERCISE_FREQ_PER_WEEK] = v.coerceIn(0, 7)
        }
    }

    // ======= 目標 =======
    suspend fun setGoal(value: String) {
        context.userProfileDataStore.edit { it[Keys.GOAL] = value }
    }

    suspend fun goal(): String? =
        context.userProfileDataStore.data.map { it[Keys.GOAL] }.first()

    val goalFlow: Flow<String?> =
        context.userProfileDataStore.data.map { it[Keys.GOAL] }

    // ======= 語言 =======
    suspend fun setLocaleTag(tag: String) {
        context.userProfileDataStore.edit { it[Keys.LOCALE_TAG] = tag }
    }

    suspend fun localeTag(): String? =
        context.userProfileDataStore.data.map { it[Keys.LOCALE_TAG] }.first()

    val localeTagFlow: Flow<String?> =
        context.userProfileDataStore.data.map { it[Keys.LOCALE_TAG] }

    // ======= 回訪旗標 =======
    val hasServerProfileFlow: Flow<Boolean> =
        context.userProfileDataStore.data.map { it[Keys.HAS_SERVER_PROFILE] ?: false }

    suspend fun setHasServerProfile(value: Boolean) {
        context.userProfileDataStore.edit { it[Keys.HAS_SERVER_PROFILE] = value }
    }

    suspend fun hasServerProfile(): Boolean =
        hasServerProfileFlow.first()

    suspend fun clearHasServerProfile() {
        context.userProfileDataStore.edit { it.remove(Keys.HAS_SERVER_PROFILE) }
    }

    // ======= 斷食方案 / 飲水 =======
    val fastingPlanFlow: Flow<String?> =
        context.userProfileDataStore.data.map { it[Keys.FASTING_PLAN] }

    suspend fun setFastingPlan(plan: String) {
        context.userProfileDataStore.edit { it[Keys.FASTING_PLAN] = plan }
    }

    val waterGoalFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.WATER_GOAL_ML] }

    suspend fun setWaterGoalMl(ml: Int) {
        context.userProfileDataStore.edit {
            it[Keys.WATER_GOAL_ML] = ml.coerceAtLeast(0)
        }
    }

    // === 讀一次目前單位（可能為 null） ===
    suspend fun getWeightUnitOnce(): WeightUnit? {
        val p = context.userProfileDataStore.data.first()
        return p[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
    }

    /** 確保今日容器（若跨日則自動歸零） */
    private suspend fun ensureTodayWater() {
        context.userProfileDataStore.edit { p ->
            val today = todayStr()
            val stored = p[Keys.WATER_TODAY_DATE]
            if (stored != today) {
                p[Keys.WATER_TODAY_DATE] = today
                p[Keys.WATER_TODAY_ML] = 0
            }
        }
    }

    val waterTodayFlow: Flow<Int> =
        context.userProfileDataStore.data.map { p -> p[Keys.WATER_TODAY_ML] ?: 0 }

    suspend fun addWaterToday(ml: Int) {
        ensureTodayWater()
        context.userProfileDataStore.edit { p ->
            val cur = p[Keys.WATER_TODAY_ML] ?: 0
            p[Keys.WATER_TODAY_ML] = (cur + ml).coerceAtLeast(0)
        }
    }

    suspend fun setWaterToday(ml: Int) {
        ensureTodayWater()
        context.userProfileDataStore.edit {
            it[Keys.WATER_TODAY_ML] = ml.coerceAtLeast(0)
        }
    }

    // ======= 每日步數目標 =======
    /**
     * UI 用：永遠有值（沒設定就顯示 10000）
     * 注意：這個「10000」是 UI 預設值，不代表 DB/Server 的真實值。
     */
    val dailyStepGoalFlow: Flow<Int> =
        context.userProfileDataStore.data.map { p ->
            p[Keys.DAILY_STEP_GOAL] ?: DEFAULT_DAILY_STEP_GOAL
        }

    /** 上傳/同步用：raw nullable（避免把 default 10000 當 정말已設定） */
    suspend fun dailyStepGoalRaw(): Int? =
        context.userProfileDataStore.data.map { it[Keys.DAILY_STEP_GOAL] }.first()

    /** 本地寫入（包含 clamp） */
    suspend fun setDailyStepGoal(v: Int) {
        context.userProfileDataStore.edit { p ->
            p[Keys.DAILY_STEP_GOAL] = v.coerceIn(0, MAX_DAILY_STEP_GOAL)
        }
    }

    /** 清除本地步數目標 */
    suspend fun clearDailyStepGoal() {
        context.userProfileDataStore.edit { it.remove(Keys.DAILY_STEP_GOAL) }
    }

    /**
     * ✅ 建議：從 Server/DB 同步回寫時用這個
     * - remote = null：代表 server 沒資料，不寫入（避免覆蓋你本地 onboarding/暫存）
     */
    suspend fun applyRemoteDailyStepGoal(remote: Int?) {
        if (remote == null) return
        setDailyStepGoal(remote)
    }

    /**
     * ✅ 可選：只有在本地「完全沒存過」時，才用 remote 填進來
     * 用在第一次登入同步很順手。
     */
    suspend fun ensureDailyStepGoalIfMissing(remote: Int?) {
        if (remote == null) return
        val cur = dailyStepGoalRaw()
        if (cur == null) setDailyStepGoal(remote)
    }

    /** UI 用：永遠有值（沒設定就顯示 2000） */
    val waterGoalUiFlow: Flow<Int> =
        context.userProfileDataStore.data.map { p ->
            p[Keys.WATER_GOAL_ML] ?: DEFAULT_WATER_GOAL_ML
        }

    /** 上傳/同步用：raw nullable（避免把 default 當 DB 真值） */
    suspend fun waterGoalMlRaw(): Int? =
        context.userProfileDataStore.data.map { it[Keys.WATER_GOAL_ML] }.first()

    /** 從 server 回寫時用：remote=null 不覆蓋 */
    suspend fun applyRemoteWaterGoal(remote: Int?) {
        if (remote == null) return
        setWaterGoalMl(remote.coerceIn(0, MAX_WATER_GOAL_ML))
    }

    /** 可選：只有在本地完全沒存過才補 remote */
    suspend fun ensureWaterGoalIfMissing(remote: Int?) {
        if (remote == null) return
        val cur = waterGoalMlRaw()
        if (cur == null) setWaterGoalMl(remote.coerceIn(0, MAX_WATER_GOAL_ML))
    }

    // ======= 快照（登入後上傳 & 冷啟檢查） =======
    data class LocalProfileSnapshot(
        val gender: String?,
        val referralSource: String?,
        val ageYears: Int?,
        val heightCm: Float?,
        val heightUnit: HeightUnit?,
        val heightFeet: Int?,
        val heightInches: Int?,
        val weightKg: Float?,
        val weightUnit: WeightUnit?,
        val weightLbs: Float?,
        val goalWeightKg: Float?,
        val goalWeightUnit: WeightUnit?,
        val goalWeightLbs: Float?,
        val exerciseFreqPerWeek: Int?,
        val goal: String?,
        val dailyStepGoal: Int?, // raw nullable
        val locale: String?,
        val fastingPlan: String?,
        val waterGoalMl: Int?,
    )

    suspend fun snapshot(): LocalProfileSnapshot {
        ensureTodayWater()
        val p = context.userProfileDataStore.data.first()
        return LocalProfileSnapshot(
            gender = p[Keys.GENDER],
            referralSource = p[Keys.REFERRAL_SOURCE],
            ageYears = p[Keys.AGE_YEARS],
            heightCm = p[Keys.HEIGHT],
            heightUnit = p[Keys.HEIGHT_UNIT]?.let { runCatching { HeightUnit.valueOf(it) }.getOrNull() },
            heightFeet = p[Keys.HEIGHT_FEET],
            heightInches = p[Keys.HEIGHT_INCHES],
            weightKg = p[Keys.WEIGHT],
            weightUnit = p[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() },
            weightLbs = p[Keys.WEIGHT_LBS],
            goalWeightKg = p[Keys.GOAL_WEIGHT],
            goalWeightUnit = p[Keys.GOAL_WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() },
            goalWeightLbs = p[Keys.GOAL_WEIGHT_LBS],
            exerciseFreqPerWeek = p[Keys.EXERCISE_FREQ_PER_WEEK],
            goal = p[Keys.GOAL],
            locale = p[Keys.LOCALE_TAG],
            fastingPlan = p[Keys.FASTING_PLAN],
            waterGoalMl = p[Keys.WATER_GOAL_ML],
            dailyStepGoal = p[Keys.DAILY_STEP_GOAL] // ✅ raw nullable
        )
    }

    suspend fun clearOnboarding() {
        context.userProfileDataStore.edit { p ->
            p.remove(Keys.GENDER)
            p.remove(Keys.REFERRAL_SOURCE)
            p.remove(Keys.AGE_YEARS)
            p.remove(Keys.HEIGHT)
            p.remove(Keys.HEIGHT_UNIT)
            p.remove(Keys.HEIGHT_FEET)
            p.remove(Keys.HEIGHT_INCHES)
            p.remove(Keys.WEIGHT)
            // p.remove(Keys.WEIGHT_UNIT)
            p.remove(Keys.WEIGHT_LBS)
            p.remove(Keys.GOAL_WEIGHT)
            // p.remove(Keys.GOAL_WEIGHT_UNIT)
            p.remove(Keys.GOAL_WEIGHT_LBS)
            p.remove(Keys.EXERCISE_FREQ_PER_WEEK)
            p.remove(Keys.GOAL)

            // ✅ 建議加：daily step goal 也清掉（你已經加了）
            p.remove(Keys.DAILY_STEP_GOAL)

            // 不清 LOCALE_TAG、HAS_SERVER_PROFILE、FASTING_PLAN、WATER_*
        }
    }
}
