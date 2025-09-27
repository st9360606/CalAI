package com.calai.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")

@Singleton
class UserProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GENDER = stringPreferencesKey("gender")                 // 未來性別可共用
        val REFERRAL_SOURCE = stringPreferencesKey("referral_source")
    }

    suspend fun setReferralSource(value: String) {
        context.userProfileDataStore.edit { it[Keys.REFERRAL_SOURCE] = value }
    }

    suspend fun referralSource(): String? {
        return context.userProfileDataStore.data
            .map { it[Keys.REFERRAL_SOURCE] }
            .first()
    }

    // 若要一併保存性別，可補一組 setter/getter
    suspend fun setGender(value: String) {
        context.userProfileDataStore.edit { it[Keys.GENDER] = value }
    }
    suspend fun gender(): String? {
        return context.userProfileDataStore.data.map { it[Keys.GENDER] }.first()
    }
}
