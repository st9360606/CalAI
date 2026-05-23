package com.calai.bitecal.core.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Centralized formatter for UTC instants returned by the backend.
 *
 * Backend fields ending with *Utc are stored and transported as UTC instants. The app should
 * always convert those instants into the user's current device time zone before showing them.
 */
object UtcTimeFormatter {

    fun parseBackendUtcInstantOrNull(raw: String?): Instant? {
        val normalized = raw
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.replace(" ", "T")
            ?: return null

        runCatching {
            return Instant.parse(normalized)
        }

        runCatching {
            return OffsetDateTime.parse(normalized).toInstant()
        }

        return runCatching {
            LocalDateTime
                .parse(normalized)
                .toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }

    fun formatUtcDateOrNull(
        raw: String?,
        zoneId: ZoneId = ZoneId.systemDefault(),
        pattern: String = "yyyy/M/d"
    ): String? {
        val instant = parseBackendUtcInstantOrNull(raw) ?: return null
        return DateTimeFormatter
            .ofPattern(pattern, Locale.getDefault())
            .withZone(zoneId)
            .format(instant)
    }

    fun formatUtcTimeOrNull(
        raw: String?,
        zoneId: ZoneId = ZoneId.systemDefault(),
        pattern: String = "HH:mm"
    ): String? {
        val instant = parseBackendUtcInstantOrNull(raw) ?: return null
        return DateTimeFormatter
            .ofPattern(pattern, Locale.getDefault())
            .withZone(zoneId)
            .format(instant)
    }

    fun formatUtcDateTimeOrNull(
        raw: String?,
        zoneId: ZoneId = ZoneId.systemDefault(),
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
        timeStyle: FormatStyle = FormatStyle.SHORT
    ): String? {
        val instant = parseBackendUtcInstantOrNull(raw) ?: return null
        return DateTimeFormatter
            .ofLocalizedDateTime(dateStyle, timeStyle)
            .withLocale(Locale.getDefault())
            .withZone(zoneId)
            .format(instant)
    }
}
