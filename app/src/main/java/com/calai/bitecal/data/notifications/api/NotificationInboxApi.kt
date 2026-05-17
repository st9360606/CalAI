package com.calai.bitecal.data.notifications.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationInboxApi {
    @GET("/api/v1/notifications")
    suspend fun list(): List<NotificationItemDto>

    @PATCH("/api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): NotificationMarkReadResponseDto
}

@Serializable
data class NotificationItemDto(
    val id: Long,
    val type: String,
    val title: String,
    val message: String,
    val deepLink: String? = null,
    val createdAtUtc: String,
    val read: Boolean = false
)

@Serializable
data class NotificationMarkReadResponseDto(
    val id: Long,
    val read: Boolean
)
