package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FoodLogStatus { PENDING, DRAFT, SAVED, FAILED, DELETED }

@Serializable
enum class DegradeLevel {
    @SerialName("DG-0") DG0,
    @SerialName("DG-1") DG1,
    @SerialName("DG-2") DG2,
    @SerialName("DG-3") DG3,
    @SerialName("DG-4") DG4
}

@Serializable
data class NutritionResultDto(
    val foodName: String? = null,
    val quantity: QuantityDto? = null,
    val nutrients: NutrientsDto? = null,
    val healthScore: Int? = null,
    val confidence: Double? = null,
    val source: SourceDto? = null
)

@Serializable data class QuantityDto(val value: Double? = null, val unit: String? = null)
@Serializable data class NutrientsDto(
    val kcal: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null
)
@Serializable data class SourceDto(val method: String? = null, val provider: String? = null)

@Serializable
data class TaskDto(val taskId: String? = null, val pollAfterSec: Int? = null)

@Serializable
data class ApiErrorDto(
    val errorCode: String? = null,
    val clientAction: String? = null,
    val retryAfterSec: Int? = null
)

@Serializable
data class TraceDto(val requestId: String? = null)

@Serializable
data class FoodLogEnvelopeDto(
    val foodLogId: String,
    val status: FoodLogStatus,
    val degradeLevel: DegradeLevel? = null,
    val nutritionResult: NutritionResultDto? = null,
    val task: TaskDto? = null,
    val error: ApiErrorDto? = null,
    val trace: TraceDto? = null
)
