package com.calai.bitecal.data.net

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseHeadersInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    private val prefs by lazy {
        context.getSharedPreferences("base_headers_prefs", Context.MODE_PRIVATE)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val lang = runCatching { Locale.getDefault().toLanguageTag() }
            .getOrDefault("en")
            .ifBlank { "en" }

        val tz = runCatching { ZoneId.systemDefault().id }
            .getOrDefault("UTC")

        val deviceId = stableDeviceId()

        val req = chain.request().newBuilder()
            .header("X-Client-Timezone", tz)
            .header("X-Device-Id", deviceId)
            .header("X-App-Lang", lang)
            .header("Accept-Language", lang)
            .build()

        return chain.proceed(req)
    }

    private fun stableDeviceId(): String {
        val cached = prefs.getString("device_id", null)
        if (!cached.isNullOrBlank()) return cached

        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()

        val generated = androidId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        prefs.edit().putString("device_id", generated).apply()
        return generated
    }
}
