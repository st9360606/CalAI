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
    val compareBasis: String
)

@Serializable
data class ProgressDayDto(
    val date: String,
    val dayOfWeek: String,
    val totalKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatsG: Double
)
