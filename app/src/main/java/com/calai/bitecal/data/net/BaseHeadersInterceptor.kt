package com.calai.bitecal.data.net

import com.calai.bitecal.core.device.DeviceIdProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class BaseHeadersInterceptor @Inject constructor(
    private val deviceIdProvider: DeviceIdProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val localeTag = Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
        val primaryLang = localeTag.substringBefore('-').ifBlank { "en" }

        val acceptLanguage = if (primaryLang.equals(localeTag, ignoreCase = true)) {
            "$localeTag,en;q=0.8"
        } else {
            "$localeTag,$primaryLang;q=0.9,en;q=0.8"
        }

        val tzId = TimeZone.getDefault().id

        val req = chain.request().newBuilder()
            .header("X-Device-Id", deviceIdProvider.get())
            .header("X-App-Lang", localeTag)
            .header("Accept-Language", acceptLanguage)
            .header("X-Client-Timezone", tzId)
            .header("X-Timezone", tzId) // 可保留做 fallback / debug
            .build()

        return chain.proceed(req)
    }
}
