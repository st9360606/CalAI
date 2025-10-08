package com.calai.app.data.auth.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        val HEIGHT = intPreferencesKey("height_cm")
        val HEIGHT_UNIT = stringPreferencesKey("height_unit")
        val WEIGHT = floatPreferencesKey("weight_kg")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val TARGET_WEIGHT = floatPreferencesKey("target_weight_kg")
        val TARGET_WEIGHT_UNIT = stringPreferencesKey("target_weight_unit")
        val EXERCISE_FREQ_PER_WEEK = intPreferencesKey("exercise_freq_per_week")
        val GOAL = stringPreferencesKey("goal")
        val LOCALE_TAG = stringPreferencesKey("locale_tag")                  // 語言
        val HAS_SERVER_PROFILE = booleanPreferencesKey("has_server_profile") // 是否已有雲端檔
    }

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
    val ageFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.AGE_YEARS] }
    suspend fun setAge(years: Int) {
        context.userProfileDataStore.edit { it[Keys.AGE_YEARS] = years }
    }

    // ======= 身高 =======
    val heightCmFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.HEIGHT] }
    suspend fun setHeightCm(cm: Int) {
        context.userProfileDataStore.edit { it[Keys.HEIGHT] = cm }
    }
    val heightUnitFlow: Flow<HeightUnit?> =
        context.userProfileDataStore.data.map { prefs ->
            prefs[Keys.HEIGHT_UNIT]?.let { runCatching { HeightUnit.valueOf(it) }.getOrNull() }
        }
    suspend fun setHeightUnit(unit: HeightUnit) {
        context.userProfileDataStore.edit { it[Keys.HEIGHT_UNIT] = unit.name }
    }

    // ======= 現在體重 =======
    val weightKgFlow: Flow<Float?> = context.userProfileDataStore.data.map { it[Keys.WEIGHT] }
    suspend fun setWeightKg(kg: Float) {
        context.userProfileDataStore.edit { it[Keys.WEIGHT] = kg }
    }
    val weightUnitFlow: Flow<WeightUnit?> =
        context.userProfileDataStore.data.map { prefs ->
            prefs[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
        }
    suspend fun setWeightUnit(unit: WeightUnit) {
        context.userProfileDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    // ======= 目標體重 =======
    val targetWeightKgFlow: Flow<Float?> =
        context.userProfileDataStore.data.map { it[Keys.TARGET_WEIGHT] }
    suspend fun setTargetWeightKg(kg: Float) {
        context.userProfileDataStore.edit { it[Keys.TARGET_WEIGHT] = kg }
    }
    val targetWeightUnitFlow: Flow<WeightUnit?> =
        context.userProfileDataStore.data.map { prefs ->
            prefs[Keys.TARGET_WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
        }
    suspend fun setTargetWeightUnit(unit: WeightUnit) {
        context.userProfileDataStore.edit { it[Keys.TARGET_WEIGHT_UNIT] = unit.name }
    }

    // ======= 鍛鍊頻率 =======
    val exerciseFreqPerWeekFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.EXERCISE_FREQ_PER_WEEK] }
    suspend fun setExerciseFreqPerWeek(v: Int) {
        context.userProfileDataStore.edit { it[Keys.EXERCISE_FREQ_PER_WEEK] = v.coerceIn(0, 7) }
    }

    // ======= 目標 =======
    suspend fun setGoal(value: String) {
        context.userProfileDataStore.edit { it[Keys.GOAL] = value }
    }
    suspend fun goal(): String? =
        context.userProfileDataStore.data.map { it[Keys.GOAL] }.first()
    val goalFlow: Flow<String?> =
        context.userProfileDataStore.data.map { it[Keys.GOAL] }

    // ======= 語言（localeTag） =======
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

    suspend fun hasServerProfile(): Boolean = hasServerProfileFlow.first()

    suspend fun clearHasServerProfile() {
        context.userProfileDataStore.edit { it.remove(Keys.HAS_SERVER_PROFILE) }
    }

    // ======= 快照（登入後上傳 & 冷啟檢查） =======
    data class LocalProfileSnapshot(
        val gender: String?,
        val referralSource: String?,
        val ageYears: Int?,
        val heightCm: Int?,
        val heightUnit: HeightUnit?,
        val weightKg: Float?,
        val weightUnit: WeightUnit?,
        val targetWeightKg: Float?,
        val targetWeightUnit: WeightUnit?,
        val exerciseFreqPerWeek: Int?,
        val goal: String?,
        val locale: String?
    )

    suspend fun snapshot(): LocalProfileSnapshot {
        return LocalProfileSnapshot(
            gender = gender(),
            referralSource = referralSource(),
            ageYears = ageFlow.first(),
            heightCm = heightCmFlow.first(),
            heightUnit = heightUnitFlow.first(),
            weightKg = weightKgFlow.first(),
            weightUnit = weightUnitFlow.first(),
            targetWeightKg = targetWeightKgFlow.first(),
            targetWeightUnit = targetWeightUnitFlow.first(),
            exerciseFreqPerWeek = exerciseFreqPerWeekFlow.first(),
            goal = goalFlow.first(),
            locale = localeTag()
        )
    }

    suspend fun clearOnboarding() {
        context.userProfileDataStore.edit { p ->
            p.remove(Keys.GENDER)
            p.remove(Keys.REFERRAL_SOURCE)
            p.remove(Keys.AGE_YEARS)
            p.remove(Keys.HEIGHT)
            p.remove(Keys.HEIGHT_UNIT)
            p.remove(Keys.WEIGHT)
            p.remove(Keys.WEIGHT_UNIT)
            p.remove(Keys.TARGET_WEIGHT)
            p.remove(Keys.TARGET_WEIGHT_UNIT)
            p.remove(Keys.EXERCISE_FREQ_PER_WEEK)
            p.remove(Keys.GOAL)
            // 不清 LOCALE_TAG、HAS_SERVER_PROFILE
        }
    }
}
