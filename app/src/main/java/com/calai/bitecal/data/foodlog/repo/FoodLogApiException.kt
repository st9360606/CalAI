package com.calai.bitecal.data.foodlog.repo

import com.calai.bitecal.data.foodlog.model.CooldownActiveDto
import com.calai.bitecal.data.foodlog.model.FoodLogServerErrorDto
import com.calai.bitecal.data.foodlog.model.ModelRefusedDto

sealed class FoodLogApiException(message: String) : RuntimeException(message) {

    class CooldownActive(val dto: CooldownActiveDto) :
        FoodLogApiException("COOLDOWN_ACTIVE")

    class ModelRefused(val dto: ModelRefusedDto) :
        FoodLogApiException("MODEL_REFUSED")

    class BusinessError(val dto: FoodLogServerErrorDto) :
        FoodLogApiException(dto.normalizedCode() ?: "BUSINESS_ERROR")
}
