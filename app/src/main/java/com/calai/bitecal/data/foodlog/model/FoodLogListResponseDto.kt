package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodLogListResponseDto(
    val items: List<FoodLogListItemDto> = emptyList(),
    val page: FoodLogPageDto? = null,
    val trace: TraceDto? = null
)

@Serializable
data class FoodLogListItemDto(
    val foodLogId: String,
    val status: FoodLogStatus,
    val capturedLocalDate: String? = null,
    val capturedAtUtc: String? = null,
    val nutrition: FoodLogListNutritionDto? = null
)

@Serializable
data class FoodLogListNutritionDto(
    val foodName: String? = null,
    val kcal: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null,
    val healthScore: Int? = null,
    val confidence: Double? = null,
    val warnings: List<String>? = null,
    val degradedReason: String? = null,
    val foodCategory: String? = null,
    val foodSubCategory: String? = null,
    @SerialName("_reasoning")
    val reasoning: String? = null,
    val labelMeta: LabelMetaDto? = null,
    val aiMeta: AiMetaDto? = null
)

@Serializable
data class FoodLogPageDto(
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)
