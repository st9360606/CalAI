package com.calai.bitecal.data.notifications.repo

import com.calai.bitecal.data.notifications.api.NotificationInboxApi
import com.calai.bitecal.data.notifications.api.NotificationItemDto
import com.calai.bitecal.data.notifications.api.NotificationMarkReadResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationInboxRepository @Inject constructor(
    private val api: NotificationInboxApi
) {
    suspend fun list(): List<NotificationItemDto> = api.list()

    suspend fun markRead(id: Long): NotificationMarkReadResponseDto = api.markRead(id)
}
