package com.calai.bitecal.data.auth.api

import com.calai.bitecal.data.auth.api.model.AuthResponse
import com.calai.bitecal.data.auth.api.model.GoogleSignInExchangeRequest
import com.calai.bitecal.data.auth.api.model.RefreshRequest
import com.calai.bitecal.data.auth.api.model.StartEmailReq
import com.calai.bitecal.data.auth.api.model.StartEmailRes
import com.calai.bitecal.data.auth.api.model.VerifyEmailReq
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

    @POST("auth/email/start")
    suspend fun startEmail(@Body body: StartEmailReq): StartEmailRes

    /** 夾帶 X-Device-Id（可為 null）；後端會記錄在 token 審計 */
    @POST("auth/email/verify")
    suspend fun verifyEmail(
        @Body body: VerifyEmailReq,
        @Header("X-Device-Id") deviceId: String? = null
    ): AuthResponse
}
