package com.calai.bitecal.ui.home.ui.foodlog

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FoodLogTimeResolver {

    fun resolveDisplayTimeText(
        zoneId: ZoneId,
        updatedAtUtc: String?,
        serverReceivedAtUtc: String?,
        capturedAtUtc: String?,
        capturedLocalDate: String?
    ): String {
        parseUtcToLocalHm(updatedAtUtc, zoneId)?.let { return it }
        parseUtcToLocalHm(serverReceivedAtUtc, zoneId)?.let { return it }
        parseUtcToLocalHm(capturedAtUtc, zoneId)?.let { return it }
        return capturedLocalDate.orEmpty()
    }

    private fun parseUtcToLocalHm(
        raw: String?,
        zoneId: ZoneId
    ): String? {
        val value = raw?.trim()
        if (value.isNullOrBlank()) return null

        return runCatching {
            Instant.parse(value)
                .atZone(zoneId)
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }
}
