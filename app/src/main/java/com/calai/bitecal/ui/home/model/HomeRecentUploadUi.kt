package com.calai.bitecal.ui.home.model

import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogListItemDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
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

    data class Delayed(
        override val foodLogId: String,
        override val previewUri: String?,
        override val timeText: String,
        val title: String,
        val subtitle: String
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

    fun delayed(
        foodLogId: String,
        previewUri: String?,
        timeText: String,
        title: String = "分析時間較久",
        subtitle: String = "完成後會自動更新"
    ): HomeRecentUploadUi.Delayed =
        HomeRecentUploadUi.Delayed(
            foodLogId = foodLogId,
            previewUri = previewUri,
            timeText = timeText,
            title = title,
            subtitle = subtitle
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
            title = result?.foodName?.takeIf { it.isNotBlank() }.orEmpty(),
            kcal = nutrients?.kcal?.roundToInt() ?: 0,
            proteinG = nutrients?.protein?.roundToInt() ?: 0,
            carbsG = nutrients?.carbs?.roundToInt() ?: 0,
            fatG = nutrients?.fat?.roundToInt() ?: 0
        )
    }

    fun fromListItem(
        previewUri: String?,
        timeText: String,
        item: FoodLogListItemDto
    ): HomeRecentUploadUi? {
        return when (item.status) {
            FoodLogStatus.PENDING -> delayed(
                foodLogId = item.foodLogId,
                previewUri = previewUri,
                timeText = timeText,
                title = "正在分析食物...",
                subtitle = "完成後會自動更新"
            )

            FoodLogStatus.DRAFT,
            FoodLogStatus.SAVED -> {
                val nutrition = item.nutrition
                HomeRecentUploadUi.Success(
                    foodLogId = item.foodLogId,
                    previewUri = previewUri,
                    timeText = timeText,
                    title = nutrition?.foodName?.takeIf { it.isNotBlank() }.orEmpty(),
                    kcal = nutrition?.kcal?.roundToInt() ?: 0,
                    proteinG = nutrition?.protein?.roundToInt() ?: 0,
                    carbsG = nutrition?.carbs?.roundToInt() ?: 0,
                    fatG = nutrition?.fat?.roundToInt() ?: 0
                )
            }

            FoodLogStatus.FAILED,
            FoodLogStatus.DELETED -> null
        }
    }
}
