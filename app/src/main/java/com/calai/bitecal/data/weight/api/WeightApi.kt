package com.calai.bitecal.data.weight.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import kotlinx.serialization.Serializable

interface WeightApi {
    @Multipart
    @POST("/api/v1/weights")
    suspend fun logWeight(
        @Part("weightKg")  weightKg: RequestBody?,   // ★ 改成 nullable
        @Part("weightLbs") weightLbs: RequestBody?,  // ★ 新增
        @Part("logDate")   logDate: RequestBody?,    // "YYYY-MM-DD"
        @Part              photo: MultipartBody.Part?
    ): WeightItemDto

    @GET("/api/v1/weights/history")
    suspend fun recent7(): List<WeightItemDto>

    @GET("/api/v1/weights/summary")
    suspend fun summary(@Query("range") range: String): SummaryDto

    @POST("/api/v1/weights/baseline")
    suspend fun ensureBaseline()
}

@Serializable
data class WeightItemDto(
    val logDate: String,
    val weightKg: Double,
    val weightLbs: Double? = null,   // ★ 若不想顯示小數，也可以維持 Int?，但 CURRENT 建議 Double
    val photoUrl: String? = null
)

@Serializable
data class SummaryDto(
    val goalKg: Double? = null,
    val goalLbs: Double? = null,
    val currentKg: Double? = null,
    val currentLbs: Double? = null,  // ★ 改成 Double?
    val firstWeightKgAllTime: Double? = null,
    val profileWeightKg: Double? = null,
    val profileWeightLbs: Double? = null,
    val achievedPercent: Double = 0.0,
    val series: List<WeightItemDto> = emptyList()
)
