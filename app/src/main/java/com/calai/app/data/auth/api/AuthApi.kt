package com.calai.app.data.auth.api

import com.calai.app.data.auth.api.model.AuthResponse
import com.calai.app.data.auth.api.model.GoogleSignInExchangeRequest
import com.calai.app.data.auth.api.model.RefreshRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.http.Header

interface AuthApi {
    @POST("auth/google")
    suspend fun googleLogin(@Body body: GoogleSignInExchangeRequest): AuthResponse

    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<AuthResponse> // Authenticator 需要同步 Call

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String?,
        @Body body: RefreshRequest?
    )
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
