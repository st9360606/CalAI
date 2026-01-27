package com.calai.bitecal.data.auth.net

import com.calai.bitecal.data.auth.repo.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val access = tokenStore.getAccessBlocking()
        val req = if (!access.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $access")
                .build()
        } else original
        return chain.proceed(req)
    }
}
