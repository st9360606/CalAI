package com.calai.app.data.profile.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AutoGoalsApi {

    @POST("/api/v1/users/me/profile/auto-goals/commit")
    suspend fun commit(@Body body: AutoGoalsCommitRequest): UserProfileDto
}

@Serializable
data class AutoGoalsCommitRequest(
    val workoutsPerWeek: Int? = null,
    val heightCm: Double? = null,
    val heightFeet: Int? = null,
    val heightInches: Int? = null,
    val weightKg: Double? = null,
    val weightLbs: Double? = null,
    val goalKey: String? = null
)
