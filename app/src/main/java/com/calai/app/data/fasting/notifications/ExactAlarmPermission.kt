package com.calai.app.data.fasting.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ExactAlarmPermission {
    fun isGranted(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = ctx.getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    fun openSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
