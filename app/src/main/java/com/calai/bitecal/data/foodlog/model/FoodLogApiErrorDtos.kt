package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.Serializable

@Serializable
data class CooldownActiveDto(
    val errorCode: String,
    val nextAllowedAtUtc: String? = null,
    val cooldownSeconds: Long? = null,
    val cooldownLevel: Int? = null,
    val cooldownReason: String? = null,
    val suggestedTier: String? = null,
    val requestId: String? = null
)

@Serializable
data class ModelRefusedDto(
    val errorCode: String,
    val refuseReason: String,
    val userMessageKey: String,
    val hint: String? = null,
    val suggestedActions: List<String> = emptyList(),
    val requestId: String? = null
)
