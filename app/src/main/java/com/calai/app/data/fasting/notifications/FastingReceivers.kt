// app/src/main/java/com/calai/app/data/fasting/notifications/FastingReceiver.kt
package com.calai.app.data.fasting.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.calai.app.R

class FastingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        val (id, title, text) = when (intent.action) {
            "FASTING_START" -> Triple(
                2001,
                context.getString(R.string.fasting_window_started),
                context.getString(R.string.fasting_window_started_body)
            )
            "FASTING_END" -> Triple(
                2002,
                context.getString(R.string.fasting_window_ended),
                context.getString(R.string.fasting_window_ended_body)
            )
            else -> return
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications) // ← 確保此圖存在
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }

    companion object {
        const val CHANNEL_ID = "fasting_plan"
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            "Fasting",
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                    )
                }
            }
        }
    }
}
