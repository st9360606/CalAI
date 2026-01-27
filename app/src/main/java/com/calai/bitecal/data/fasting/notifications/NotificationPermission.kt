package com.calai.bitecal.data.fasting.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationPermission {

    /**
     * 代表「這台裝置上，App 目前有能力送出通知」：
     * - App 通知總開關要開
     * - Android 13+ 還要有 POST_NOTIFICATIONS 權限
     *
     * ⚠️ 注意：這裡不檢查 channel 是否被關（那是更細的層級，可另外做）
     */
    fun isGranted(context: Context): Boolean {
        // 1) App 層級通知總開關
        val appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!appEnabled) return false

        // 2) Android 13+ runtime permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
