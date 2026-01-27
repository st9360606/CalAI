package com.calai.bitecal.data.fasting.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

interface FastingApi {
    @GET("/api/v1/fasting-plan/me")
    suspend fun getMine(): Response<FastingPlanDto>   // 用 Response 包起來，好辨識 404

    @PUT("/api/v1/fasting-plan/me")
    suspend fun upsert(@Body req: UpsertFastingPlanReq): FastingPlanDto

    @GET("/api/v1/fasting-plan/next-triggers")
    suspend fun nextTriggers(
        @Query("planCode") planCode: String,
        @Query("startTime") startTime: String,   // "HH:mm"
        @Query("timeZone") timeZone: String
    ): NextTriggersResp
}

@Serializable
data class FastingPlanDto(
    val planCode: String,
    val startTime: String,   // "HH:mm"
    val endTime: String,     // "HH:mm"（僅顯示用）
    val enabled: Boolean,
    val timeZone: String
)

@Serializable
data class UpsertFastingPlanReq(
    val planCode: String,
    val startTime: String,    // "HH:mm"
    val enabled: Boolean,
    val timeZone: String
)

@Serializable
data class NextTriggersResp(
    val nextStartUtc: String, // ISO-8601 Instant
    val nextEndUtc: String
)

