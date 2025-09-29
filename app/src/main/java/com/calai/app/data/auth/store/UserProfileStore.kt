package com.calai.app.data.store

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calai.app.data.store.UserProfileStore.Keys.AGE_YEARS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

private val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")

@Singleton
class UserProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GENDER = stringPreferencesKey("gender")
        val REFERRAL_SOURCE = stringPreferencesKey("referral_source")
        val AGE_YEARS = intPreferencesKey("age_years")
        val HEIGHT = intPreferencesKey("height")
    }

    //=======性別=========
    suspend fun setGender(value: String) {
        context.userProfileDataStore.edit { it[Keys.GENDER] = value }
    }
    suspend fun gender(): String? {
        return context.userProfileDataStore.data.map { it[Keys.GENDER] }.first()
    }

    //=======推薦來源=========
    suspend fun setReferralSource(value: String) {
        context.userProfileDataStore.edit { it[Keys.REFERRAL_SOURCE] = value }
    }

    suspend fun referralSource(): String? {
        return context.userProfileDataStore.data
            .map { it[Keys.REFERRAL_SOURCE] }
            .first()
    }

    //=======年齡=========
    val ageFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[AGE_YEARS] }

    suspend fun setAge(years: Int) {
        context.userProfileDataStore.edit { it[AGE_YEARS] = years }
    }

    //=======身高=========
    val heightCmFlow: Flow<Int?> = context.userProfileDataStore.data.map { it[Keys.HEIGHT] }
    suspend fun setHeightCm(cm: Int) { context.userProfileDataStore.edit { it[Keys.HEIGHT] = cm } }

}
