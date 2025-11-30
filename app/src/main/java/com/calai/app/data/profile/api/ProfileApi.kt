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

    // ★ 新增：更新目標體重（只接受 value + unit）
    @PUT("/api/v1/users/me/profile/target-weight")
    suspend fun updateTargetWeight(@Body body: UpdateTargetWeightRequest): UserProfileDto
}

@Serializable
data class UserProfileDto(
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val heightFeet: Int? = null,
    val heightInches: Int? = null,
    val weightKg: Double? = null,
    val weightLbs: Double? = null,      // ★ Int? -> Double?
    val exerciseLevel: String? = null,
    val goal: String? = null,
    val targetWeightKg: Double? = null,
    val targetWeightLbs: Double? = null, // ★ Int? -> Double?
    val dailyStepGoal: Int? = null,          // ✅ NEW
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
    val weightLbs: Double?,            // ★
    val exerciseLevel: String?,
    val goal: String?,
    val targetWeightKg: Double?,
    val targetWeightLbs: Double?,      // ★
    val dailyStepGoal: Int?,                 // ✅ NEW
    val referralSource: String?,
    val locale: String?
)

@Serializable
data class UpdateTargetWeightRequest(
    val value: Double, // 使用者輸入數值（KG 或 LBS）
    val unit: String   // "KG" or "LBS"
)
