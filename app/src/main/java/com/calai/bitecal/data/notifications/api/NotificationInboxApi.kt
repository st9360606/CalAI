package com.calai.bitecal.data.notifications.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface NotificationInboxApi {
    @GET("/api/v1/notifications")
    suspend fun list(): List<NotificationItemDto>
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
