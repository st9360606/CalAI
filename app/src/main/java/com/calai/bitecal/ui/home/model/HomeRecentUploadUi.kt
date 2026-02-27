package com.calai.bitecal.ui.home.model

import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import kotlin.math.roundToInt

sealed interface HomeRecentUploadUi {
    val foodLogId: String
    val previewUri: String?
    val timeText: String

    data class Pending(
        override val foodLogId: String,
        override val previewUri: String?,
        override val timeText: String
    ) : HomeRecentUploadUi

    data class Success(
        override val foodLogId: String,
        override val previewUri: String?,
        override val timeText: String,
        val title: String,
        val kcal: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int
    ) : HomeRecentUploadUi
}

object HomeRecentUploadMapper {

    fun pending(
        foodLogId: String,
        previewUri: String?,
        timeText: String
    ): HomeRecentUploadUi.Pending =
        HomeRecentUploadUi.Pending(
            foodLogId = foodLogId,
            previewUri = previewUri,
            timeText = timeText
        )

    fun success(
        foodLogId: String,
        previewUri: String?,
        timeText: String,
        env: FoodLogEnvelopeDto
    ): HomeRecentUploadUi.Success {
        val result = env.nutritionResult
        val nutrients = result?.nutrients

        return HomeRecentUploadUi.Success(
            foodLogId = foodLogId,
            previewUri = previewUri,
            timeText = timeText,
            title = result?.foodName?.takeIf { it.isNotBlank() } ?: "已完成分析",
            kcal = nutrients?.kcal?.roundToInt() ?: 0,
            proteinG = nutrients?.protein?.roundToInt() ?: 0,
            carbsG = nutrients?.carbs?.roundToInt() ?: 0,
            fatG = nutrients?.fat?.roundToInt() ?: 0
        )
    }
}
