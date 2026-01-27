package com.calai.bitecal.core.device

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun get(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )
        return androidId ?: "bc-" + UUID.randomUUID().toString()
    }
}
