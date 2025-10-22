package com.calai.app

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.calai.app.data.fasting.notifications.FastingReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.util.Log

@HiltAndroidApp
class BiteCal : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // ✅ WorkManager 2.9.x 以屬性覆寫
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO) // 可調成 VERBOSE 以除錯
            .build()

    override fun onCreate() {
        super.onCreate()
        // 提前建立通知頻道（避免第一次發通知前沒有頻道）
        FastingReceiver.ensureChannel(this)
    }
}
