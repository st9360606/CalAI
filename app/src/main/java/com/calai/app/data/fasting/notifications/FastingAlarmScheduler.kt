// app/src/main/java/com/calai/app/data/fasting/notifications/FastingAlarmScheduler.kt
package com.calai.app.data.fasting.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import java.time.Instant

class FastingAlarmScheduler(private val context: Context) {

    private val alarm: AlarmManager = context.getSystemService()!!

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(startUtc: Instant, endUtc: Instant) {
        cancel()
        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startUtc.toEpochMilli(),
            pending("FASTING_START")
        )
        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            endUtc.toEpochMilli(),
            pending("FASTING_END")
        )
    }

    fun cancel() {
        alarm.cancel(pending("FASTING_START"))
        alarm.cancel(pending("FASTING_END"))
    }

    // 別名，容許舊呼叫
    fun cancelAll() = cancel()

    private fun pending(action: String): PendingIntent {
        val intent = Intent(action).setClassName(
            context.packageName,
            "com.calai.app.data.fasting.notifications.FastingReceiver"
        )
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
