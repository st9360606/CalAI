package com.calai.app.data.auth.api.model

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String? = null
)
