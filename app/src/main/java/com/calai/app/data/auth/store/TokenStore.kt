package com.calai.app.data.auth.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val appContext: Context   // ← 加上 @ApplicationContext
) {
    private val KEY_ACCESS = stringPreferencesKey("access")
    private val KEY_REFRESH = stringPreferencesKey("refresh")

    val accessTokenFlow: Flow<String?> = appContext.dataStore.data.safeMap { it[KEY_ACCESS] }
    val refreshTokenFlow: Flow<String?> = appContext.dataStore.data.safeMap { it[KEY_REFRESH] }

    suspend fun save(access: String, refresh: String?) {
        appContext.dataStore.edit {
            it[KEY_ACCESS] = access
            refresh?.let { rt -> it[KEY_REFRESH] = rt }
        }
    }

    suspend fun clear() {
        appContext.dataStore.edit { it.clear() }
    }

    // 給 OkHttp 同步取值用（避免在 Authenticator 裡掛起）
    fun getAccessBlocking(): String? = runBlockingNoCrash { accessTokenFlow.first() }
    fun getRefreshBlocking(): String? = runBlockingNoCrash { refreshTokenFlow.first() }

    // 供 Authenticator 寫回
    fun saveBlocking(access: String, refresh: String?) = runBlockingNoCrash {
        save(access, refresh)
    }

    private fun <T> Flow<Preferences>.safeMap(transform: (Preferences) -> T): Flow<T> =
        catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map(transform)

    private fun <T> runBlockingNoCrash(block: suspend () -> T): T? =
        try {
            kotlinx.coroutines.runBlocking { block() }
        } catch (_: Exception) { null }
}
