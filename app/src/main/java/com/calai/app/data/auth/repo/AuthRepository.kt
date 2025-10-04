package com.calai.app.data.auth.repo

import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.auth.api.model.AuthResponse
import com.calai.app.data.auth.api.model.GoogleSignInExchangeRequest
import com.calai.app.data.auth.api.model.RefreshRequest
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @Named("authApi") private val api: AuthApi,   // ← 指向 auth 專用 Retrofit
    private val tokenStore: TokenStore
) {
    suspend fun loginWithGoogle(idToken: String, clientId: String? = null): AuthResponse {
        val resp = api.googleLogin(
            GoogleSignInExchangeRequest(idToken = idToken, clientId = clientId)
        )
        tokenStore.save(resp.accessToken, resp.refreshToken)
        return resp
    }

    suspend fun logout() {
        val access = tokenStore.accessTokenFlow.firstOrNull()
        val refresh = tokenStore.refreshTokenFlow.firstOrNull()
        runCatching {
            api.logout(
                authorization = access?.let { "Bearer $it" },
                body = refresh?.let { RefreshRequest(it) }
            )
        }
        tokenStore.clear()
    }
}