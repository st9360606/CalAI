package com.calai.bitecal.data.workout.repo

import com.calai.bitecal.data.workout.api.EstimateRequest
import com.calai.bitecal.data.workout.api.EstimateResponse
import com.calai.bitecal.data.workout.api.LogWorkoutRequest
import com.calai.bitecal.data.workout.api.LogWorkoutResponse
import com.calai.bitecal.data.workout.api.PresetWorkoutDto
import com.calai.bitecal.data.workout.api.TodayWorkoutResponse
import com.calai.bitecal.data.workout.api.WorkoutHistoryResponse
import com.calai.bitecal.data.workout.api.WorkoutApi
import com.calai.bitecal.data.workout.model.WorkoutWeeklyProgressDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val api: WorkoutApi
) {

    suspend fun estimateFreeText(text: String): EstimateResponse {
        return api.estimate(EstimateRequest(text = text))
    }

    suspend fun saveWorkout(
        activityId: Long,
        minutes: Int,
        kcal: Int?
    ): LogWorkoutResponse {
        val req = LogWorkoutRequest(
            activityId = activityId,
            minutes = minutes,
            kcal = kcal
        )
        return api.log(req)
    }

    suspend fun loadPresets(): List<PresetWorkoutDto> {
        return api.presets().presets
    }

    suspend fun loadToday(): TodayWorkoutResponse {
        return api.today()
    }

    suspend fun loadRecentHistory(): WorkoutHistoryResponse {
        return api.recentHistory()
    }

    suspend fun loadMyWeightKg(): Double {
        return api.myWeight().kg
    }

    suspend fun loadWeeklyProgress(): WorkoutWeeklyProgressDto {
        return api.weeklyProgress()
    }
}
