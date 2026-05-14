package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FoodLogOverrideRequestDto(
    val fieldKey: String,
    val newValue: JsonElement,
    val reason: String? = null
)
