package com.calai.bitecal.data.foodlog.api

import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface FoodLogsApi {

    @Multipart
    @POST("/api/v1/food-logs/photo")
    suspend fun postPhoto(
        @Part file: MultipartBody.Part,
        @Part("deviceCapturedAtUtc") deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto

    @Multipart
    @POST("/api/v1/food-logs/album")
    suspend fun postAlbum(
        @Part file: MultipartBody.Part
    ): FoodLogEnvelopeDto

    @POST("/api/v1/food-logs/barcode")
    suspend fun postBarcode(@Body req: BarcodeReq): FoodLogEnvelopeDto

    @Multipart
    @POST("/api/v1/food-logs/label")
    suspend fun postLabel(
        @Part file: MultipartBody.Part,
        @Part("deviceCapturedAtUtc") deviceCapturedAtUtc: RequestBody? = null
    ): FoodLogEnvelopeDto

    @GET("/api/v1/food-logs/{id}")
    suspend fun getOne(@Path("id") id: String): FoodLogEnvelopeDto

    @POST("/api/v1/food-logs/{id}/retry")
    suspend fun retry(@Path("id") id: String): FoodLogEnvelopeDto

    @POST("/api/v1/food-logs/{id}/save")
    suspend fun save(@Path("id") id: String): FoodLogEnvelopeDto

    @DELETE("/api/v1/food-logs/{id}")
    suspend fun delete(@Path("id") id: String): FoodLogEnvelopeDto
}

@Serializable
data class BarcodeReq(val barcode: String)