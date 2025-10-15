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
    val weightKg: Double? = null,
    val exerciseLevel: String? = null,
    val goal: String? = null,
    val targetWeightKg: Double? = null,
    val referralSource: String? = null,
    val locale: String? = null
)

@Serializable
data class UpsertProfileRequest(
    val gender: String?,
    val age: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val exerciseLevel: String?,
    val goal: String?,
    val targetWeightKg: Double?,
    val referralSource: String?,
    val locale: String?
)