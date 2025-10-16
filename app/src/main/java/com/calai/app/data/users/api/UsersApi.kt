package com.calai.app.data.users.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface UsersApi {
    // 對應後端的 GET /api/v1/users/me
    @GET("/api/v1/users/me")
    suspend fun me(): MeDto
}

@Serializable
data class MeDto(
    val id: Long,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null
)