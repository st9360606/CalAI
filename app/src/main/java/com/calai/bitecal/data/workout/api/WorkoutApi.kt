package com.calai.bitecal.data.workout.api

import com.calai.bitecal.data.workout.model.WorkoutWeeklyProgressDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Workout / Activity tracking API
 *
 * X-Client-Timezone 一律由 BaseHeadersInterceptor 統一帶。
 */
interface WorkoutApi {

    @POST("/api/v1/workouts/estimate")
    suspend fun estimate(
        @Body body: EstimateRequest
    ): EstimateResponse

    @POST("/api/v1/workouts/log")
    suspend fun log(
        @Body body: LogWorkoutRequest
    ): LogWorkoutResponse

    @GET("/api/v1/workouts/presets")
    suspend fun presets(): PresetListResponse

    @GET("/api/v1/workouts/today")
    suspend fun today(): TodayWorkoutResponse

    @GET("/api/v1/workouts/history/recent")
    suspend fun recentHistory(): WorkoutHistoryResponse

    @DELETE("/api/v1/workouts/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: Long
    ): TodayWorkoutResponse

    @GET("/api/v1/workouts/me/weight")
    suspend fun myWeight(): WeightDto

    @GET("/api/v1/workouts/progress/weekly")
    suspend fun weeklyProgress(
        @Query("weekOffset") weekOffset: Int = 0
    ): WorkoutWeeklyProgressDto
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

/** 寫入實際運動紀錄 */
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

@Serializable
data class WorkoutHistoryResponse(
    val totalKcal: Int,
    val sessions: List<WorkoutHistorySessionDto>
)

@Serializable
data class WorkoutHistorySessionDto(
    val id: Long,
    val name: String,
    val minutes: Int,
    val kcal: Int,
    val localDate: String,
    val dateLabel: String,
    val timeLabel: String
)

@Serializable
data class WeightDto(
    val kg: Double
)
