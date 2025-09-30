package com.calai.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calai.app.data.store.UserProfileStore.Keys.AGE_YEARS
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
    enum class WeightUnit { KG, LBS } // ← 新增：體重單位
    private object Keys {
        val GENDER = stringPreferencesKey("gender")
        val REFERRAL_SOURCE = stringPreferencesKey("referral_source")
        val AGE_YEARS = intPreferencesKey("age_years")
        val HEIGHT = intPreferencesKey("height_cm")
        val HEIGHT_UNIT = stringPreferencesKey("height_unit")   // ← 新增
        val WEIGHT = floatPreferencesKey("weight_kg")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit") // ← 新增：體重單位 key
        val EXERCISE_FREQ_PER_WEEK = intPreferencesKey("exercise_freq_per_week")
    }

    //=======性別=========
    suspend fun setGender(value: String) {
        context.userProfileDataStore.edit { it[Keys.GENDER] = value }
    }
    suspend fun gender(): String? =
        context.userProfileDataStore.data.map { it[Keys.GENDER] }.first()

    //=======推薦來源=========
    suspend fun setReferralSource(value: String) {
        context.userProfileDataStore.edit { it[Keys.REFERRAL_SOURCE] = value }
    }
    suspend fun referralSource(): String? =
        context.userProfileDataStore.data.map { it[Keys.REFERRAL_SOURCE] }.first()

    //=======年齡=========
    val ageFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[AGE_YEARS] }
    suspend fun setAge(years: Int) {
        context.userProfileDataStore.edit { it[AGE_YEARS] = years }
    }

    //=======身高（數值）=========
    val heightCmFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.HEIGHT] }
    suspend fun setHeightCm(cm: Int) {
        context.userProfileDataStore.edit { it[Keys.HEIGHT] = cm }
    }

    //=======身高（單位）=========
    val heightUnitFlow: Flow<HeightUnit?> =
        context.userProfileDataStore.data.map { prefs ->
            prefs[Keys.HEIGHT_UNIT]?.let { runCatching { HeightUnit.valueOf(it) }.getOrNull() }
        }

    suspend fun setHeightUnit(unit: HeightUnit) {
        context.userProfileDataStore.edit { it[Keys.HEIGHT_UNIT] = unit.name }
    }

    //=======體重（數值）=========
    val weightKgFlow: Flow<Float?> = context.userProfileDataStore.data.map { it[Keys.WEIGHT] }
    suspend fun setWeightKg(kg: Float) {
        context.userProfileDataStore.edit { it[Keys.WEIGHT] = kg }
    }

    //=======體重（單位）=========  ← 新增：讓返回頁時能還原 lbs / kg
    val weightUnitFlow: Flow<WeightUnit?> =
        context.userProfileDataStore.data.map { prefs ->
            prefs[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() }
        }
    suspend fun setWeightUnit(unit: WeightUnit) {
        context.userProfileDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    //=======鍛鍊頻率=========
    val exerciseFreqPerWeekFlow: Flow<Int?> =
        context.userProfileDataStore.data.map { it[Keys.EXERCISE_FREQ_PER_WEEK] }

    suspend fun setExerciseFreqPerWeek(v: Int) {
        context.userProfileDataStore.edit { it[Keys.EXERCISE_FREQ_PER_WEEK] = v.coerceIn(0, 7) }
    }
}
