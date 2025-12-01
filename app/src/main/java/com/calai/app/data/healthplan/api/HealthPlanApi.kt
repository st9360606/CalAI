package com.calai.app.data.healthplan.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HealthPlanApi {
    @POST("/api/v1/health-plan")
    suspend fun upsert(@Body req: SaveHealthPlanRequest): HealthPlanResponse

    @GET("/api/v1/health-plan")
    suspend fun get(): HealthPlanResponse
}

@Serializable
data class SaveHealthPlanRequest(
    val source: String = "ONBOARDING",
    val calcVersion: String = "healthcalc_v1",

    val goalKey: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goalWeightKg: Double? = null,
    val unitPreference: String = "KG",
    val workoutsPerWeek: Int? = null,

    val kcal: Int,
    val carbsG: Int,
    val proteinG: Int,
    val fatG: Int,
    val waterMl: Int,
    val bmi: Double,
    val bmiClass: String
)

@Serializable
data class HealthPlanResponse(
    val userId: Long,
    val source: String,
    val calcVersion: String,
    val kcal: Int,
    val carbsG: Int,
    val proteinG: Int,
    val fatG: Int,
    val waterMl: Int,
    val bmi: Double,
    val bmiClass: String
)
