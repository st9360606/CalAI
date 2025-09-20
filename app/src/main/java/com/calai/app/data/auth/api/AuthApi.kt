package com.calai.app.data.auth.api

import com.calai.app.data.auth.api.model.AuthResponse
import com.calai.app.data.auth.api.model.GoogleSignInExchangeRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/google")
    suspend fun googleLogin(
        @Body body: GoogleSignInExchangeRequest
    ): AuthResponse
}
data class GoogleLoginRequest(val idToken: String)

data class SessionResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val name: String?,
    val email: String
)
