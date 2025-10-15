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
import kotlin.math.round

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

        // Height (metric + imperial)
        val HEIGHT = intPreferencesKey("height_cm")
        val HEIGHT_UNIT = stringPreferencesKey("height_unit")
        val HEIGHT_FEET = intPreferencesKey("height_feet")
        val HEIGHT_INCHES = intPreferencesKey("height_inches")

        // Weight (metric + imperial)
        val WEIGHT = floatPreferencesKey("weight_kg")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val WEIGHT_LBS = intPreferencesKey("weight_lbs")

        // Target Weight (metric + imperial)
        val TARGET_WEIGHT = floatPreferencesKey("target_weight_kg")
        val TARGET_WEIGHT_UNIT = stringPreferencesKey("target_weight_unit")
        val TARGET_WEIGHT_LBS = intPreferencesKey("target_weight_lbs")

        val EXERCISE_FREQ_PER_WEEK = intPreferencesKey("exercise_freq_per_week")
        val GOAL = stringPreferencesKey("goal")
        val LOCALE_TAG = stringPreferencesKey("locale_tag")
        val HAS_SERVER_PROFILE = booleanPreferencesKey("has_server_profile")

        // Fasting / Water
        val FASTING_PLAN = stringPreferencesKey("fasting_plan")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val WATER_TODAY_DATE = stringPreferencesKey("water_today_date")
        val WATER_TODAY_ML = intPreferencesKey("water_today_ml")
    }

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private fun todayStr(): String = LocalDate.now().format(dateFmt)

    // ======= 性別 =======
    suspend fun setGender(value: String) { context.userProfileDataStore.edit { it[Keys.GENDER] = value } }
    suspend fun gender(): String? = context.userProfileDataStore.data.map { it[Keys.GENDER] }.first()
    val genderFlow: Flow<String?> = context.userProfileDataStore.data.map { it[Keys.GENDER] }

    // ======= 推薦來源 =======
    suspend fun setReferralSource(value: String) { context.userProfileDataStore.edit { it[Keys.REFERRAL_SOURCE] = value } }
    suspend fun referralSource(): String? = context.userProfileDataStore.data.map { it[Keys.REFERRAL_SOURCE] }.first()
    val referralSourceFlow: Flow<String?> = context.userProfileDataStore.data.map { it[Keys.REFERRAL_SOURCE] }

    // ======= 年齡 =======
    val ageFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.AGE_YEARS] }
    suspend fun setAge(years: Int) { context.userProfileDataStore.edit { it[Keys.AGE_YEARS] = years } }

    // ======= 身高（cm + ft/in） =======
    val heightCmFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.HEIGHT] }
    suspend fun setHeightCm(cm: Int) { context.userProfileDataStore.edit { it[Keys.HEIGHT] = cm } }

    val heightUnitFlow: Flow<HeightUnit?> = context.userProfileDataStore.data.map { p ->
        p[Keys.HEIGHT_UNIT]?.let { runCatching { HeightUnit.valueOf(it) }.getOrNull() }
    }
    suspend fun setHeightUnit(unit: HeightUnit) { context.userProfileDataStore.edit { it[Keys.HEIGHT_UNIT] = unit.name } }

    val heightFeetFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.HEIGHT_FEET] }
    val heightInchesFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.HEIGHT_INCHES] }
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
    val weightKgFlow: Flow<Float?> = context.userProfileDataStore.data.map { it[Keys.WEIGHT] }
    suspend fun setWeightKg(kg: Float) { context.userProfileDataStore.edit { it[Keys.WEIGHT] = kg } }

    val weightUnitFlow: Flow<WeightUnit?> = context.userProfileDataStore.data.map { p ->
        p[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
    }
    suspend fun setWeightUnit(unit: WeightUnit) { context.userProfileDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name } }

    val weightLbsFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.WEIGHT_LBS] }
    suspend fun setWeightLbs(lbs: Int) { context.userProfileDataStore.edit { it[Keys.WEIGHT_LBS] = lbs.coerceAtLeast(0) } }
    suspend fun clearWeightLbs() { context.userProfileDataStore.edit { it.remove(Keys.WEIGHT_LBS) } }

    // ======= 目標體重（kg + lbs） =======
    val targetWeightKgFlow: Flow<Float?> = context.userProfileDataStore.data.map { it[Keys.TARGET_WEIGHT] }
    suspend fun setTargetWeightKg(kg: Float) { context.userProfileDataStore.edit { it[Keys.TARGET_WEIGHT] = kg } }

    val targetWeightUnitFlow: Flow<WeightUnit?> = context.userProfileDataStore.data.map { p ->
        p[Keys.TARGET_WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
    }
    suspend fun setTargetWeightUnit(unit: WeightUnit) { context.userProfileDataStore.edit { it[Keys.TARGET_WEIGHT_UNIT] = unit.name } }

    val targetWeightLbsFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.TARGET_WEIGHT_LBS] }
    suspend fun setTargetWeightLbs(lbs: Int) { context.userProfileDataStore.edit { it[Keys.TARGET_WEIGHT_LBS] = lbs.coerceAtLeast(0) } }
    suspend fun clearTargetWeightLbs() { context.userProfileDataStore.edit { it.remove(Keys.TARGET_WEIGHT_LBS) } }

    // ======= 鍛鍊頻率 =======
    val exerciseFreqPerWeekFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.EXERCISE_FREQ_PER_WEEK] }
    suspend fun setExerciseFreqPerWeek(v: Int) {
        context.userProfileDataStore.edit { it[Keys.EXERCISE_FREQ_PER_WEEK] = v.coerceIn(0, 7) }
    }

    // ======= 目標 =======
    suspend fun setGoal(value: String) { context.userProfileDataStore.edit { it[Keys.GOAL] = value } }
    suspend fun goal(): String? = context.userProfileDataStore.data.map { it[Keys.GOAL] }.first()
    val goalFlow: Flow<String?> = context.userProfileDataStore.data.map { it[Keys.GOAL] }

    // ======= 語言 =======
    suspend fun setLocaleTag(tag: String) { context.userProfileDataStore.edit { it[Keys.LOCALE_TAG] = tag } }
    suspend fun localeTag(): String? = context.userProfileDataStore.data.map { it[Keys.LOCALE_TAG] }.first()
    val localeTagFlow: Flow<String?> = context.userProfileDataStore.data.map { it[Keys.LOCALE_TAG] }

    // ======= 回訪旗標 =======
    val hasServerProfileFlow: Flow<Boolean> =
        context.userProfileDataStore.data.map { it[Keys.HAS_SERVER_PROFILE] ?: false }
    suspend fun setHasServerProfile(value: Boolean) { context.userProfileDataStore.edit { it[Keys.HAS_SERVER_PROFILE] = value } }
    suspend fun hasServerProfile(): Boolean = hasServerProfileFlow.first()
    suspend fun clearHasServerProfile() { context.userProfileDataStore.edit { it.remove(Keys.HAS_SERVER_PROFILE) } }

    // ======= 斷食方案 / 飲水 =======
    val fastingPlanFlow: Flow<String?> = context.userProfileDataStore.data.map { it[Keys.FASTING_PLAN] }
    suspend fun setFastingPlan(plan: String) { context.userProfileDataStore.edit { it[Keys.FASTING_PLAN] = plan } }

    val waterGoalFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.WATER_GOAL_ML] }
    suspend fun setWaterGoalMl(ml: Int) { context.userProfileDataStore.edit { it[Keys.WATER_GOAL_ML] = ml.coerceAtLeast(0) } }

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
    val waterTodayFlow: Flow<Int> = context.userProfileDataStore.data.map { p -> p[Keys.WATER_TODAY_ML] ?: 0 }
    suspend fun addWaterToday(ml: Int) {
        ensureTodayWater()
        context.userProfileDataStore.edit { p ->
            val cur = p[Keys.WATER_TODAY_ML] ?: 0
            p[Keys.WATER_TODAY_ML] = (cur + ml).coerceAtLeast(0)
        }
    }
    suspend fun setWaterToday(ml: Int) {
        ensureTodayWater()
        context.userProfileDataStore.edit { it[Keys.WATER_TODAY_ML] = ml.coerceAtLeast(0) }
    }

    // ======= 快照（登入後上傳 & 冷啟檢查） =======
    data class LocalProfileSnapshot(
        val gender: String?,
        val referralSource: String?,
        val ageYears: Int?,
        val heightCm: Int?,
        val heightUnit: HeightUnit?,
        val heightFeet: Int?,
        val heightInches: Int?,
        val weightKg: Float?,
        val weightUnit: WeightUnit?,
        val weightLbs: Int?,
        val targetWeightKg: Float?,
        val targetWeightUnit: WeightUnit?,
        val targetWeightLbs: Int?,
        val exerciseFreqPerWeek: Int?,
        val goal: String?,
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
            targetWeightKg = p[Keys.TARGET_WEIGHT],
            targetWeightUnit = p[Keys.TARGET_WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() },
            targetWeightLbs = p[Keys.TARGET_WEIGHT_LBS],
            exerciseFreqPerWeek = p[Keys.EXERCISE_FREQ_PER_WEEK],
            goal = p[Keys.GOAL],
            locale = p[Keys.LOCALE_TAG],
            fastingPlan = p[Keys.FASTING_PLAN],
            waterGoalMl = p[Keys.WATER_GOAL_ML]
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
            p.remove(Keys.WEIGHT_UNIT)
            p.remove(Keys.WEIGHT_LBS)
            p.remove(Keys.TARGET_WEIGHT)
            p.remove(Keys.TARGET_WEIGHT_UNIT)
            p.remove(Keys.TARGET_WEIGHT_LBS)
            p.remove(Keys.EXERCISE_FREQ_PER_WEEK)
            p.remove(Keys.GOAL)
            // 不清 LOCALE_TAG、HAS_SERVER_PROFILE、FASTING_PLAN、WATER_*
        }
    }
}

/* ======= 換算（與畫面一致）======= */
fun kgToLbsInt(v: Double): Int = round(v * 2.2).toInt()
fun cmToFeetInches(cm: Int): Pair<Int, Int> {
    val totalInches = (cm / 2.54).toInt() // floor
    val feet = totalInches / 12
    val inch = totalInches % 12
    return feet to inch
}
