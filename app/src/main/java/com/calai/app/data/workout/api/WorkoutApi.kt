package com.calai.app.data.workout.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Workout / Activity tracking API
 */
interface WorkoutApi {

    @POST("/api/v1/workouts/estimate")
    suspend fun estimate(@Body body: EstimateRequest): EstimateResponse

    @POST("/api/v1/workouts/log")
    suspend fun log(@Body body: LogWorkoutRequest): LogWorkoutResponse

    @GET("/api/v1/workouts/presets")
    suspend fun presets(): PresetListResponse

    @GET("/api/v1/workouts/today")
    suspend fun today(): TodayWorkoutResponse
}

/** 使用者自由輸入的句子 */
@Serializable
data class EstimateRequest(
    val text: String // ex: "15 min Nằm đẩy tạ"
)

/** 後端估算結果 */
@Serializable
data class EstimateResponse(
    val status: String,           // "ok" | "not_found"
    val activityId: Long? = null, // 後端識別到的標準運動 ID
    val activityDisplay: String? = null, // "Nằm đẩy tạ"
    val minutes: Int? = null,
    val kcal: Int? = null
)

/** 寫入實際運動紀錄。 */
@Serializable
data class LogWorkoutRequest(
    val activityId: Long, // standard workout id (dictionary_id)
    val minutes: Int,
    val kcal: Int? = null // optional；後端可重算，前端只是給參考
)

/** 建檔後回傳本次 session & 今日累積 */
@Serializable
data class LogWorkoutResponse(
    val savedSession: WorkoutSessionDto,
    val totalKcalToday: Int
)

/** 給列表下半部（Walking / Running ...） */
@Serializable
data class PresetListResponse(
    val presets: List<PresetWorkoutDto>
)

@Serializable
data class PresetWorkoutDto(
    val activityId: Long,
    val name: String,            // "Walking"
    val kcalPer30Min: Int,       // 140
    val iconKey: String          // "walk"
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
    val timeLabel: String        // "12:05 PM"
)
