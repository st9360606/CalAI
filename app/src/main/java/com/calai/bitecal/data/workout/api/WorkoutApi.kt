package com.calai.bitecal.data.workout.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Workout / Activity tracking API
 */
interface WorkoutApi {

    @POST("/api/v1/workouts/estimate")
    suspend fun estimate(
        @Body body: EstimateRequest
    ): EstimateResponse

    @POST("/api/v1/workouts/log")
    suspend fun log(
        @Body body: LogWorkoutRequest,
        @Header("X-Client-Timezone") tz: String
    ): LogWorkoutResponse

    @GET("/api/v1/workouts/presets")
    suspend fun presets(): PresetListResponse

    @GET("/api/v1/workouts/today")
    suspend fun today(
        @Header("X-Client-Timezone") tz: String
    ): TodayWorkoutResponse

    // ★ 取得目前使用者體重(kg)，供 fallback 計算使用
    @GET("/api/v1/workouts/me/weight")
    suspend fun myWeight(): WeightDto
}

/** 使用者自由輸入的句子 */
@Serializable
data class EstimateRequest(val text: String)

/** 後端估算結果 */
@Serializable
data class EstimateResponse(
    val status: String,
    val activityId: Long? = null,
    val activityDisplay: String? = null,
    val minutes: Int? = null,
    val kcal: Int? = null
)

/** 寫入實際運動紀錄。 */
@Serializable
data class LogWorkoutRequest(
    val activityId: Long,
    val minutes: Int,
    val kcal: Int? = null
)

/** 建檔後回傳本次 session & 今日累積 */
@Serializable
data class LogWorkoutResponse(
    val savedSession: WorkoutSessionDto,
    val today: TodayWorkoutResponse
)

/** 給列表下半部（Walking / Running ...） */
@Serializable
data class PresetListResponse(
    val presets: List<PresetWorkoutDto>
)

@Serializable
data class PresetWorkoutDto(
    val activityId: Long,
    val name: String,
    val kcalPer30Min: Int,
    val iconKey: String
)

/** 今日紀錄 + 總和，for History(4.jpg) & Home card */
@Serializable
data class TodayWorkoutResponse(
    val totalKcalToday: Int,
    val sessions: List<WorkoutSessionDto>
)

@Serializable
data class WorkoutSessionDto(
    val id: Long,
    val name: String,
    val minutes: Int,
    val kcal: Int,
    val timeLabel: String
)

// ★ 新增：使用者體重（kg）
@Serializable
data class WeightDto(
    val kg: Double
)
