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
        val acceptLanguage = "$localeTag,${localeTag.substringBefore('-')};q=0.9,en;q=0.8"
        val tzId = TimeZone.getDefault().id

        val req = chain.request().newBuilder()
            .header("X-Device-Id", deviceIdProvider.get())
            .header("X-App-Lang", localeTag)          // ✅ NEW：業務語系統一用它
            .header("Accept-Language", acceptLanguage) // ✅ 保留標準 header（可做 fallback/除錯）
            .header("X-Client-Timezone", tzId)
            .header("X-Timezone", tzId)
            .build()

        return chain.proceed(req)
    }
}
