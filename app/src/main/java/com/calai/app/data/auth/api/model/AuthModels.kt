package com.calai.app.data.auth.api.model

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInExchangeRequest(
    val idToken: String,
    val clientId: String? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String? = null
)
