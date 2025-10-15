package com.calai.app.data.profile.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {
    @GET("/api/v1/users/me/profile")
    suspend fun getMyProfile(): UserProfileDto

    @PUT("/api/v1/users/me/profile")
    suspend fun upsertMyProfile(@Body body: UpsertProfileRequest): UserProfileDto
}

@Serializable
data class UserProfileDto(
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val heightFeet: Int? = null,
    val heightInches: Int? = null,
    val weightKg: Double? = null,
    val weightLbs: Int? = null,
    val exerciseLevel: String? = null,
    val goal: String? = null,
    val targetWeightKg: Double? = null,
    val targetWeightLbs: Int? = null,
    val referralSource: String? = null,
    val locale: String? = null
)

@Serializable
data class UpsertProfileRequest(
    val gender: String?,
    val age: Int?,
    val heightCm: Double?,
    val heightFeet: Int?,
    val heightInches: Int?,
    val weightKg: Double?,
    val weightLbs: Int?,
    val exerciseLevel: String?,
    val goal: String?,
    val targetWeightKg: Double?,
    val targetWeightLbs: Int?,
    val referralSource: String?,
    val locale: String?
)
