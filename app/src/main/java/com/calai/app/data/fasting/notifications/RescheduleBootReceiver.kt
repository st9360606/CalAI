package com.calai.app.data.fasting.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

class RescheduleBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action
        ) {
            val req = OneTimeWorkRequestBuilder<RescheduleWorker>()
                .setInitialDelay(Duration.ofMinutes(1))
                .build()

            // ✅ 去重：boot / 更新 可能連續觸發，避免堆多個 worker
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "fasting_reschedule"
    }
}
