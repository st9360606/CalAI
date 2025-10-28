package com.calai.app.data.workout.repo

import com.calai.app.data.workout.api.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val api: WorkoutApi
) {
    suspend fun estimateFreeText(text: String): EstimateResponse {
        return api.estimate(EstimateRequest(text = text))
    }

    suspend fun saveWorkout(activityId: Long, minutes: Int, kcal: Int?): LogWorkoutResponse {
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
}
