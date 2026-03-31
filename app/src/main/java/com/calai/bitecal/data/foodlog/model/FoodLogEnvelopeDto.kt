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
enum class ModelTier {
    @SerialName("MODEL_TIER_HIGH")
    HIGH,
    @SerialName("MODEL_TIER_LOW")
    LOW,
    @SerialName("BARCODE")
    BARCODE
}

@Serializable
data class NutritionResultDto(
    val foodName: String? = null,
    val quantity: QuantityDto? = null,
    val nutrients: NutrientsDto? = null,
    val healthScore: Int? = null,
    val confidence: Double? = null,
    val warnings: List<String>? = null,
    val degradedReason: String? = null,
    val foodCategory: String? = null,
    val foodSubCategory: String? = null,
    @SerialName("_reasoning")
    val reasoning: String? = null,
    val labelMeta: LabelMetaDto? = null,
    val aiMeta: AiMetaDto? = null,
    val source: SourceDto? = null
)

@Serializable
data class QuantityDto(
    val value: Double? = null,
    val unit: String? = null
)

@Serializable
data class NutrientsDto(
    val kcal: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null
)

@Serializable
data class LabelMetaDto(
    val servingsPerContainer: Double? = null,
    val basis: String? = null
)

@Serializable
data class AiMetaDto(
    val degradedReason: String? = null,
    val degradedAtUtc: String? = null,
    val resultFromCache: Boolean? = null,
    val foodCategory: String? = null,
    val foodSubCategory: String? = null,
    val source: String? = null,
    val basis: String? = null,
    val lang: String? = null
)

@Serializable
data class SourceDto(
    val method: String? = null,
    val provider: String? = null,
    val resolvedBy: String? = null
)

@Serializable
data class TaskDto(
    val taskId: String? = null,
    val pollAfterSec: Int? = null
)

@Serializable
data class ApiErrorDto(
    val errorCode: String? = null,
    val clientAction: ClientAction? = null,
    val retryAfterSec: Int? = null
)

@Serializable
data class HintDto(
    val hintCode: String? = null,
    val clientAction: ClientAction? = null,
    val message: String? = null
)

@Serializable
data class TraceDto(
    val requestId: String? = null
)

@Serializable
data class FoodLogEnvelopeDto(
    val foodLogId: String,
    val status: FoodLogStatus,
    val degradeLevel: DegradeLevel? = null,
    val tierUsed: ModelTier? = null,
    val fromCache: Boolean = false,
    val portionMultiplier: Int = 1, // NEW: 後端回傳目前已保存的份數，預設 1
    val nutritionResult: NutritionResultDto? = null,
    val task: TaskDto? = null,
    val error: ApiErrorDto? = null,
    val hints: List<HintDto>? = null,
    val trace: TraceDto? = null
)
