package com.calai.app.data.fasting.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import java.time.Instant

class FastingAlarmScheduler(private val context: Context) {

    private val alarm: AlarmManager = context.getSystemService()!!

    companion object {
        private const val TAG = "FastingAlarm"
        const val ACTION_FASTING_START = "com.calai.app.action.FASTING_START"
        const val ACTION_FASTING_END_SOON = "com.calai.app.action.FASTING_END_SOON"
        const val EXTRA_PLAN_CODE = "extra_plan_code"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
    }

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(
        startUtc: Instant,
        endSoonUtc: Instant,
        planCode: String,
        startTime: String,
        endTime: String
    ) {
        val canExact = canScheduleExact()
        Log.d(TAG, "schedule() canExact=$canExact startUtc=$startUtc endSoonUtc=$endSoonUtc plan=$planCode $startTime-$endTime")

        setAlarm(
            triggerAtMillis = startUtc.toEpochMilli(),
            pi = pending(ACTION_FASTING_START, planCode, startTime, endTime),
            exact = canExact
        )
        setAlarm(
            triggerAtMillis = endSoonUtc.toEpochMilli(),
            pi = pending(ACTION_FASTING_END_SOON, planCode, startTime, endTime),
            exact = canExact
        )
    }

    fun cancel() {
        Log.d(TAG, "cancel()")
        alarm.cancel(pending(ACTION_FASTING_START, "", "", ""))
        alarm.cancel(pending(ACTION_FASTING_END_SOON, "", "", ""))
    }

    private fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarm.canScheduleExactAlarms()
        } else true
    }

    private fun setAlarm(triggerAtMillis: Long, pi: PendingIntent, exact: Boolean) {
        if (exact) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun pending(action: String, planCode: String, startTime: String, endTime: String): PendingIntent {
        val intent = Intent(context, FastingReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PLAN_CODE, planCode)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_END_TIME, endTime)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
