package com.calai.app.data.auth.api.model

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInExchangeRequest(
    val idToken: String,
    val clientId: String? = null
)

@Serializable data class StartEmailReq(val email: String)
@Serializable data class StartEmailRes(val sent: Boolean)
@Serializable data class VerifyEmailReq(val email: String, val code: String)

/** 後端升級版，App 端欄位皆可為 null 以保持相容 */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String? = "Bearer",
    val accessExpiresInSec: Long? = null,
    val refreshExpiresInSec: Long? = null,
    val serverTimeEpochSec: Long? = null
)
