package com.calai.app.data.healthplan.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calai.app.data.healthplan.api.SaveHealthPlanRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

private val Context.healthPlanDataStore by preferencesDataStore(name = "health_plan_store")

@Singleton
class HealthPlanLocalStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : PendingHealthPlanStore {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private object Keys {
        val PENDING_JSON = stringPreferencesKey("pending_health_plan_json")
    }

    override suspend fun getPending(): SaveHealthPlanRequest? {
        val prefs = ctx.healthPlanDataStore.data.first()
        val raw = prefs[Keys.PENDING_JSON] ?: return null
        return runCatching {
            // 避免版本變更時炸裂：用 JsonElement parse 再 decode
            val el = json.parseToJsonElement(raw)
            json.decodeFromJsonElement<SaveHealthPlanRequest>(el)
        }.getOrNull()
    }

    override suspend fun setPending(req: SaveHealthPlanRequest) {
        val raw = json.encodeToString(req)
        ctx.healthPlanDataStore.edit { it[Keys.PENDING_JSON] = raw }
    }

    override suspend fun clearPending() {
        ctx.healthPlanDataStore.edit { it.remove(Keys.PENDING_JSON) }
    }
}
