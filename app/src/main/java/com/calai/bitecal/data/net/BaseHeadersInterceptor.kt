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
        val tzId = TimeZone.getDefault().id // e.g. Asia/Taipei

        val req = chain.request().newBuilder()
            .header("X-Device-Id", deviceIdProvider.get())   // ✅ 修正
            .header("Accept-Language", acceptLanguage)
            .header("X-Timezone", tzId) // ✅ NEW：讓後端用來算 daily/monthly key
            .build()

        return chain.proceed(req)
    }
}
