package com.calai.bitecal.core.device

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("device_id", Context.MODE_PRIVATE)
    }

    fun get(): String {
        val key = "device_id_v1"
        val existing = prefs.getString(key, null)
        if (!existing.isNullOrBlank()) return existing

        val newId = "bc-" + UUID.randomUUID().toString()
        prefs.edit().putString(key, newId).apply()
        return newId
    }
}
