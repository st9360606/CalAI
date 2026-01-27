 package com.calai.bitecal.data.foodlog.api

import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import okhttp3.MultipartBody
import retrofit2.http.*

interface FoodLogsApi {

    @Multipart
    @POST("/v1/food-logs/photo")
    suspend fun postPhoto(
        @Part file: MultipartBody.Part
    ): FoodLogEnvelopeDto

    @Multipart
    @POST("/v1/food-logs/album")
    suspend fun postAlbum(
        @Part file: MultipartBody.Part
    ): FoodLogEnvelopeDto

    @POST("/v1/food-logs/barcode")
    suspend fun postBarcode(@Body req: BarcodeReq): FoodLogEnvelopeDto

    @Multipart
    @POST("/v1/food-logs/label")
    suspend fun postLabel(
        @Part file: MultipartBody.Part
    ): FoodLogEnvelopeDto

    @GET("/v1/food-logs/{id}")
    suspend fun getOne(@Path("id") id: String): FoodLogEnvelopeDto

    @POST("/v1/food-logs/{id}/retry")
    suspend fun retry(@Path("id") id: String): FoodLogEnvelopeDto

    @POST("/v1/food-logs/{id}/save")
    suspend fun save(@Path("id") id: String): FoodLogEnvelopeDto

    @DELETE("/v1/food-logs/{id}")
    suspend fun delete(@Path("id") id: String): FoodLogEnvelopeDto
}

@kotlinx.serialization.Serializable
data class BarcodeReq(val barcode: String)
