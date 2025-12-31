package com.calai.app.data.fasting.notifications

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class TriggerTimes(
    val nextStart: Instant,
    val nextEnd: Instant,
    val endSoon: Instant
)

object NextTriggerCalculator {

    /**
     * 用本地時區計算下一次 start/end（DST-safe：用 ZonedDateTime）
     *
     * 規則：
     * - 若今天的 startTime 還沒到 → nextStart=今天 startTime
     * - 否則 → nextStart=明天 startTime
     * - nextEnd = nextStart + eatingHours
     * - endSoon = nextEnd - 1 hour
     */
    fun compute(
        startTime: LocalTime,
        eatingHours: Int,
        zoneId: ZoneId,
        now: Instant = Instant.now()
    ): TriggerTimes {
        val nowZ = now.atZone(zoneId)

        var startZ: ZonedDateTime = nowZ.toLocalDate()
            .atTime(startTime)
            .atZone(zoneId)

        // ✅ isBefore 才加一天（若剛好等於 startTime，仍應排「現在」）
        if (startZ.isBefore(nowZ)) {
            startZ = startZ.plusDays(1)
        }

        val endZ = startZ.plusHours(eatingHours.toLong())
        val endSoonZ = endZ.minusHours(1)

        return TriggerTimes(
            nextStart = startZ.toInstant(),
            nextEnd = endZ.toInstant(),
            endSoon = endSoonZ.toInstant()
        )
    }
}
