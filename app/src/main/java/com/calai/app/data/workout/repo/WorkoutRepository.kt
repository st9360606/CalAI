package com.calai.app.data.workout.repo

import com.calai.app.data.workout.api.*
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val api: WorkoutApi
) {

    // 把目前裝置時區當成「你現在在哪」
    private fun deviceTz(): String = TimeZone.getDefault().id
    // 例: "Asia/Taipei", "America/Los_Angeles"

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
        return api.log(
            body = req,
            tz = deviceTz()            // ★ 把 X-Client-Timezone 帶出去
        )
    }

    suspend fun loadPresets(): List<PresetWorkoutDto> {
        return api.presets().presets
    }

    suspend fun loadToday(): TodayWorkoutResponse {
        return api.today(
            tz = deviceTz()            // ★ 同樣帶出去
        )
    }
}
