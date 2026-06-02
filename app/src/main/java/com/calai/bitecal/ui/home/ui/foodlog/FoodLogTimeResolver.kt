package com.calai.bitecal.ui.home.ui.foodlog

import com.calai.bitecal.core.time.UtcTimeFormatter
import java.time.ZoneId

object FoodLogTimeResolver {

    fun resolveDisplayTimeText(
        zoneId: ZoneId,
        createdAtUtc: String?,
        serverReceivedAtUtc: String?,
        capturedAtUtc: String?,
        capturedLocalDate: String?
    ): String {
        parseUtcToLocalHm(createdAtUtc, zoneId)?.let { return it }
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

        return UtcTimeFormatter.formatUtcTimeOrNull(
            raw = value,
            zoneId = zoneId,
            pattern = "HH:mm"
        )
    }
}
