package com.calai.bitecal.data.foodlog.api

import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogListResponseDto
import com.calai.bitecal.data.foodlog.model.FoodLogOverrideRequestDto
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface FoodLogsApi {

    @Multipart
    @POST("/api/v1/food-logs/photo")
    suspend fun postPhoto(
        @Part file: MultipartBody.Part,
        @Part("deviceCapturedAtUtc") deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto

    /**
     * ALBUM 模式目前不送 deviceCapturedAtUtc。
     * 若後端規則改變，再補 multipart text part。
     */
    @Multipart
    @POST("/api/v1/food-logs/album")
    suspend fun postAlbum(
        @Part file: MultipartBody.Part
    ): FoodLogEnvelopeDto

    @POST("/api/v1/food-logs/barcode")
    suspend fun postBarcode(
        @Body req: BarcodeReq
    ): FoodLogEnvelopeDto

    @Multipart
    @POST("/api/v1/food-logs/label")
    suspend fun postLabel(
        @Part file: MultipartBody.Part,
        @Part("deviceCapturedAtUtc") deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto

    @GET("/api/v1/food-logs/{id}")
    suspend fun getOne(
        @Path("id") id: String
    ): FoodLogEnvelopeDto

    @POST("/api/v1/food-logs/{id}/retry")
    suspend fun retry(
        @Path("id") id: String
    ): FoodLogEnvelopeDto

    @POST("/api/v1/food-logs/{id}/save")
    suspend fun save(
        @Path("id") id: String
    ): FoodLogEnvelopeDto

    @DELETE("/api/v1/food-logs/{id}")
    suspend fun delete(
        @Path("id") id: String
    ): FoodLogEnvelopeDto

    @GET("/api/v1/food-logs")
    suspend fun listSaved(
        @Query("fromLocalDate") fromLocalDate: String,
        @Query("toLocalDate") toLocalDate: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): FoodLogListResponseDto

    @GET("/api/v1/food-logs/history")
    suspend fun listHistory(
        @Query("status") status: String,
        @Query("fromLocalDate") fromLocalDate: String,
        @Query("toLocalDate") toLocalDate: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): FoodLogListResponseDto

    @GET("/api/v1/food-logs/recent-previews")
    suspend fun listRecentPreviews(
        @Query("lookBackHours") lookBackHours: Int = 72,
        @Query("size") size: Int = 10
    ): FoodLogListResponseDto

    @POST("/api/v1/food-logs/{id}/overrides")
    suspend fun applyOverride(
        @Path("id") id: String,
        @Body req: FoodLogOverrideRequestDto
    ): FoodLogEnvelopeDto

    @Streaming
    @GET("/api/v1/food-logs/{id}/image")
    suspend fun getImage(
        @Path("id") id: String
    ): ResponseBody
}

@Serializable
data class BarcodeReq(
    val barcode: String,
    val locale: String? = null
)
