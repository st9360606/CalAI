package com.calai.bitecal.ui.home.ui.progress

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate

internal object ProgressChartAxisDefaults {
    val GridColor = Color(0xFFBDBDBD)
    val IdleLabelColor = Color(0xFF8A8A8E)
    val TodayLabelColor = Color(0xFF4B5563)

    val IdleLabelWeight = FontWeight.Normal
    val TodayLabelWeight = FontWeight.Bold

    fun isToday(dateIso: String, dayLabel: String): Boolean {
        val today = LocalDate.now()

        if (dateIso == today.toString()) {
            return true
        }

        // 用於空資料 placeholder：date 可能是 ""，但 dayLabel 仍是 Mon/Tue...
        return dateIso.isBlank() &&
                dayLabel.take(3).equals(
                    today.dayOfWeek.name.take(3),
                    ignoreCase = true
                )
    }
}