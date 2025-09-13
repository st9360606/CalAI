package com.calai.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ⚠️ 這行一定要在「檔案頂層」，不能放在 class 裡
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN = stringPreferencesKey("auth_token")
    }

    /** 讀取 Token（null 代表尚未設定） */
    val tokenFlow: Flow<String?> =
        context.appDataStore.data.map { it[TOKEN] }

    /** 設定/更新 Token */
    suspend fun setToken(token: String) {
        context.appDataStore.edit { it[TOKEN] = token }
    }

    /** 清除 Token */
    suspend fun clearToken() {
        context.appDataStore.edit { it.remove(TOKEN) }
    }
}
