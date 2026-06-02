package com.calai.bitecal.ui.home.ui.foodlog

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class FoodLogTimeResolverTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun resolveDisplayTimeText_usesCreatedAtBeforeUpdatedAt() {
        val result = FoodLogTimeResolver.resolveDisplayTimeText(
            zoneId = utc,
            createdAtUtc = "2026-03-21T07:30:00Z",
            serverReceivedAtUtc = "2026-03-21T07:31:00Z",
            capturedAtUtc = "2026-03-21T07:29:00Z",
            capturedLocalDate = "2026-03-21"
        )

        assertEquals("07:30", result)
    }

    @Test
    fun resolveDisplayTimeText_fallsBackWithoutCreatedAt() {
        val result = FoodLogTimeResolver.resolveDisplayTimeText(
            zoneId = utc,
            createdAtUtc = null,
            serverReceivedAtUtc = "2026-03-21T07:31:00Z",
            capturedAtUtc = "2026-03-21T07:29:00Z",
            capturedLocalDate = "2026-03-21"
        )

        assertEquals("07:31", result)
    }
}
