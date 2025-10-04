package com.calai.app.data.auth.repo

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val KEY_ACCESS = stringPreferencesKey("access")
    private val KEY_REFRESH = stringPreferencesKey("refresh")

    // 新增（選用）：存 access 到期的 epoch 秒數
    private val KEY_ACCESS_EXPIRES_AT = longPreferencesKey("access_expires_at")

    val accessTokenFlow: Flow<String?> = appContext.dataStore.data.safeMap { it[KEY_ACCESS] }
    val refreshTokenFlow: Flow<String?> = appContext.dataStore.data.safeMap { it[KEY_REFRESH] }
    val accessExpiresAtFlow: Flow<Long?> =
        appContext.dataStore.data.safeMap { it[KEY_ACCESS_EXPIRES_AT] }

    /** 你原本的兩參數版（保留相容） */
    suspend fun save(access: String, refresh: String?) {
        appContext.dataStore.edit {
            it[KEY_ACCESS] = access
            refresh?.let { rt -> it[KEY_REFRESH] = rt }
            it.remove(KEY_ACCESS_EXPIRES_AT) // 兩參數版不寫到期（保險）
        }
    }

    /** 新增四參數版（可帶到期秒數與伺服器時間秒數；沒有就忽略） */
    suspend fun save(
        access: String,
        refresh: String?,
        accessExpiresInSec: Long?,
        serverTimeEpochSec: Long?
    ) {
        appContext.dataStore.edit {
            it[KEY_ACCESS] = access
            refresh?.let { rt -> it[KEY_REFRESH] = rt }

            if (accessExpiresInSec != null) {
                val base = serverTimeEpochSec ?: (System.currentTimeMillis() / 1000)
                it[KEY_ACCESS_EXPIRES_AT] = base + accessExpiresInSec
            } else {
                it.remove(KEY_ACCESS_EXPIRES_AT)
            }
        }
    }

    suspend fun clear() {
        appContext.dataStore.edit { it.clear() }
    }

    // 給 OkHttp Authenticator 等同步取值
    fun getAccessBlocking(): String? = runBlockingNoCrash { accessTokenFlow.first() }
    fun getRefreshBlocking(): String? = runBlockingNoCrash { refreshTokenFlow.first() }
    fun getAccessExpiresAtBlocking(): Long? = runBlockingNoCrash { accessExpiresAtFlow.first() }

    fun saveBlocking(access: String, refresh: String?) =
        runBlockingNoCrash { save(access, refresh) }

    private fun <T> Flow<Preferences>.safeMap(transform: (Preferences) -> T): Flow<T> =
        catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.map(transform)

    private fun <T> runBlockingNoCrash(block: suspend () -> T): T? =
        try {
            runBlocking { block() }
        } catch (_: Exception) {
            null
        }
}
