package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodLogServerErrorDto(
    val errorCode: String? = null,
    val code: String? = null,
    val message: String? = null,
    val requestId: String? = null,
    val clientAction: String? = null,
    val retryAfterSec: Int? = null
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
