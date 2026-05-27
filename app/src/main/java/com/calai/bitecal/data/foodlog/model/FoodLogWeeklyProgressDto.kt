package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodLogWeeklyProgressDto(
    val period: ProgressPeriodDto,
    val summary: ProgressSummaryDto,
    val days: List<ProgressDayDto>
)

@Serializable
data class ProgressPeriodDto(
    val weekOffset: Int,
    val label: String,
    val startDate: String,
    val endDate: String
)

@Serializable
data class ProgressSummaryDto(
    val totalCalories: Double,
    val deltaPercent: Double? = null,
    val deltaDirection: String,
    val compareBasis: String,
    val average7Calories: Double = 0.0,
    val average15Calories: Double = 0.0,
    val average7FiberG: Double = 0.0,
    val average7SugarG: Double = 0.0,
    val average7SodiumMg: Double = 0.0
)

@Serializable
data class ProgressDayDto(
    val date: String,
    val dayOfWeek: String,
    val totalKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatsG: Double,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val avgHealthScore: Double = 0.0
)


@Serializable
data class ProgressAveragesDto(
    val ranges: List<ProgressAverageRangeDto> = emptyList()
)

@Serializable
data class ProgressAverageRangeDto(
    val days: Int,
    val caloriesKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatsG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val workoutKcal: Double = 0.0,
    val waterMl: Double = 0.0,
    val healthScore: Double = 0.0,
    val steps: Double = 0.0
)
