package com.calai.app.data.auth

import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.auth.api.model.AuthResponse
import com.calai.app.data.auth.api.model.GoogleSignInExchangeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi
) {
    suspend fun loginWithGoogle(
        idToken: String,
        clientId: String? = null // 若需要可從 BuildConfig 或 strings.xml 帶入
    ): AuthResponse {
        return api.googleLogin(
            GoogleSignInExchangeRequest(
                idToken = idToken,
                clientId = clientId
            )
        )
    }
}
