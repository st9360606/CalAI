package com.calai.bitecal.data.workout.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutWeeklyProgressDto(
    val summary: WorkoutWeeklyProgressSummaryDto,
    val days: List<WorkoutProgressDayDto>
)

@Serializable
data class WorkoutWeeklyProgressSummaryDto(
    val todayBurnedKcal: Double,
    val goalKcal: Int,
    val averageKcal: Int,
    val deltaPercent: Double? = null,
    val deltaDirection: String,
    val compareBasis: String
)

@Serializable
data class WorkoutProgressDayDto(
    val date: String,
    val dayOfWeek: String,
    val totalBurnedKcal: Double,
    val workoutKcal: Double,
    val activeKcal: Double
)
