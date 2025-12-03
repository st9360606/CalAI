// app/src/main/java/com/calai/app/data/profile/repo/PlanMetricsRepository.kt
package com.calai.app.data.profile.repo

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.api.UpsertPlanMetricsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.planMetricsStore by preferencesDataStore(name = "plan_metrics_pending")

@Singleton
class PlanMetricsRepository @Inject constructor(
    private val api: ProfileApi,
    @ApplicationContext private val ctx: Context,
) {
    sealed class SaveResult {
        data object Sent : SaveResult()
        data object Pending : SaveResult()
        data class Failed(val code: Int?, val message: String?) : SaveResult()
    }

    private object Keys { val PENDING_JSON = stringPreferencesKey("pending_json") }

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
    private val flushMutex = Mutex()

    suspend fun upsertBestEffort(req: UpsertPlanMetricsRequest): SaveResult {
        return try {
            api.upsertPlanMetrics(req)
            clearPending()
            SaveResult.Sent
        } catch (e: HttpException) {
            if (e.code() == 401) {
                setPending(req)
                SaveResult.Pending
            } else {
                Log.e("PlanMetricsRepo", "upsertPlanMetrics failed: ${e.code()} ${e.message()}")
                SaveResult.Failed(e.code(), e.message())
            }
        } catch (t: Throwable) {
            Log.e("PlanMetricsRepo", "upsertPlanMetrics failed (non-http): ${t.message}", t)
            SaveResult.Failed(null, t.message)
        }
    }

    suspend fun flushPendingIfAny(): SaveResult {
        return flushMutex.withLock {
            val pending = getPending() ?: return@withLock SaveResult.Sent
            upsertBestEffort(pending)
        }
    }

    private suspend fun setPending(req: UpsertPlanMetricsRequest) {
        ctx.planMetricsStore.edit { it[Keys.PENDING_JSON] = json.encodeToString(req) }
    }

    private suspend fun getPending(): UpsertPlanMetricsRequest? {
        val raw = ctx.planMetricsStore.data.first()[Keys.PENDING_JSON] ?: return null
        return runCatching { json.decodeFromString(UpsertPlanMetricsRequest.serializer(), raw) }.getOrNull()
    }

    private suspend fun clearPending() {
        ctx.planMetricsStore.edit { it.remove(Keys.PENDING_JSON) }
    }
}
