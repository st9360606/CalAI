package com.calai.app.data.profile.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface ProfileApi {
    @GET("/api/v1/users/me/profile")
    suspend fun getMyProfile(): UserProfileDto

    /**
     * ✅ 新增 header：X-Profile-Source
     * - ONBOARDING：後端允許重算宏量
     * - null：一般更新，不重算宏量
     */
    @PUT("/api/v1/users/me/profile")
    suspend fun upsertMyProfile(
        @Body body: UpsertProfileRequest,
        @Header("X-Profile-Source") source: String? = null
    ): UserProfileDto

    @PUT("/api/v1/users/me/profile/goal-weight")
    suspend fun updateGoalWeight(@Body body: UpdateGoalWeightRequest): UserProfileDto
}

@Serializable
data class UserProfileDto(
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val heightFeet: Int? = null,
    val heightInches: Int? = null,
    val weightKg: Double? = null,
    val weightLbs: Double? = null,
    val exerciseLevel: String? = null,
    val goal: String? = null,
    val goalWeightKg: Double? = null,
    val goalWeightLbs: Double? = null,
    val unitPreference: String? = null,
    val workoutsPerWeek: Int? = null,
    val dailyStepGoal: Int? = null,
    val referralSource: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class UpsertProfileRequest(
    val gender: String?,
    val age: Int?,
    val heightCm: Double?,
    val heightFeet: Int?,
    val heightInches: Int?,
    val weightKg: Double?,
    val weightLbs: Double?,
    val exerciseLevel: String?,
    val goal: String?,
    val goalWeightKg: Double?,
    val goalWeightLbs: Double?,
    val dailyStepGoal: Int?,
    val referralSource: String?,
    val locale: String?,
    val unitPreference: String? = null,
    val workoutsPerWeek: Int? = null
)

@Serializable
data class UpdateGoalWeightRequest(
    val value: Double,
    val unit: String
)
