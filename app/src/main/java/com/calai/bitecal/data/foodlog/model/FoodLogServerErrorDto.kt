package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodLogServerErrorDto(
    val errorCode: String? = null,
    val code: String? = null,
    val message: String? = null,
    val requestId: String? = null,
    val clientAction: String? = null,
    val retryAfterSec: Int? = null,
    val nextAllowedAtUtc: String? = null,
    val cooldownSeconds: Int? = null,
    val cooldownLevel: Int? = null,
    val cooldownReason: String? = null,
    val suggestedTier: String? = null
) {
    fun normalizedCode(): String? = errorCode ?: code

    fun toApiErrorDto(): ApiErrorDto {
        val action = clientAction?.let { raw ->
            runCatching { ClientAction.valueOf(raw) }.getOrNull()
        }

        return ApiErrorDto(
            errorCode = normalizedCode(),
            clientAction = action,
            retryAfterSec = retryAfterSec
        )
    }
}
