package com.calai.app.data.weight.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import kotlinx.serialization.Serializable

interface WeightApi {
    @Multipart
    @POST("/api/v1/weights")
    suspend fun logWeight(
        @Part("weightKg") weightKg: RequestBody,
        @Part("logDate") logDate: RequestBody?,      // "YYYY-MM-DD"
        @Part photo: MultipartBody.Part?             // 圖片可選
    ): WeightItemDto

    @GET("/api/v1/weights/history")
    suspend fun recent7(): List<WeightItemDto>

    @GET("/api/v1/weights/summary")
    suspend fun summary(@Query("range") range: String): SummaryDto
}

@Serializable
data class WeightItemDto(
    val logDate: String,
    val weightKg: Double,
    val weightLbs: Int? = null,
    val photoUrl: String? = null
)

@Serializable
data class SummaryDto(
    val goalKg: Double? = null,
    val goalLbs: Int? = null,
    val currentKg: Double? = null,
    val currentLbs: Int? = null,
    val firstWeightKgAllTimeKg: Double? = null,   // ★ 新增欄位
    val achievedPercent: Double = 0.0,
    val series: List<WeightItemDto> = emptyList()
)

