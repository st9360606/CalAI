package com.calai.bitecal.data.net

import com.calai.bitecal.core.device.DeviceIdProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import javax.inject.Inject

class BaseHeadersInterceptor @Inject constructor(
    private val deviceIdProvider: DeviceIdProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val localeTag = Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
        val acceptLanguage = "$localeTag,${localeTag.substringBefore('-')};q=0.9,en;q=0.8"

        val req = chain.request().newBuilder()
            .header("X-Device-Id", deviceIdProvider.get())   // ✅ 修正
            .header("Accept-Language", acceptLanguage)
            .build()

        return chain.proceed(req)
    }
}
