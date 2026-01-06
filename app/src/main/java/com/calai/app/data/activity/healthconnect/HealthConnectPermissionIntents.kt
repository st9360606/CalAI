package com.calai.app.data.activity.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.health.connect.client.HealthConnectClient

/**
 * 嘗試打開 Health Connect 權限/設定頁：
 * - Android 14+：優先打開「Manage health permissions」(可帶 EXTRA_PACKAGE_NAME 指向本 app)
 * - 其他：打開 Health Connect 設定頁
 * - 若都失敗：fallback 到 Play Store（Android 13↓ 常見）或 App details
 */
object HealthConnectPermissionIntents {

    private const val TAG = "HC_INTENTS"

    // Android 14+ 的健康權限管理頁 action（API 34+）
    private const val ACTION_MANAGE_HEALTH_PERMISSIONS =
        "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"

    /**
     * @return true 表示成功 startActivity；false 表示所有方案都無法打開
     */
    fun openHealthPermissionsSettings(ctx: Context): Boolean {
        val pm = ctx.packageManager

        fun tryStart(i: Intent): Boolean {
            val intent = i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolved = intent.resolveActivity(pm) != null
            Log.d(TAG, "tryStart resolved=$resolved intent=$intent")
            return if (resolved) {
                runCatching {
                    ctx.startActivity(intent)
                    true
                }.getOrElse { e ->
                    Log.e(TAG, "startActivity failed: ${e.javaClass.simpleName}: ${e.message}", e)
                    false
                }
            } else {
                false
            }
        }

        // 先看 SDK 是否可用（Android 13↓ 沒裝 HC 時會不是 AVAILABLE）
        val sdkStatus = HealthConnectClient.getSdkStatus(ctx)
        Log.d(TAG, "sdkStatus=$sdkStatus sdkInt=${Build.VERSION.SDK_INT}")

        // Android 14+：先嘗試直達本 app 的健康權限管理頁（若裝置支援）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manage = Intent(ACTION_MANAGE_HEALTH_PERMISSIONS).apply {
                // 官方/平台文件：可選帶 Intent.EXTRA_PACKAGE_NAME 指向特定 app :contentReference[oaicite:3]{index=3}
                putExtra(Intent.EXTRA_PACKAGE_NAME, ctx.packageName)
            }
            if (tryStart(manage)) return true
        }

        // fallback：打開 Health Connect 設定頁（需 HC 可用）
        val hcSettings = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        if (tryStart(hcSettings)) return true

        // 再 fallback：Android 13↓ 常見是沒裝 Health Connect，導 Play Store
        // 官方文件：Android 13↓ 需要安裝 Health Connect app :contentReference[oaicite:4]{index=4}
        val playStore = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
        if (tryStart(playStore)) return true

        val playWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
        if (tryStart(playWeb)) return true

        // 最後 fallback：至少打開你 app 詳細設定頁（讓使用者有地方可操作）
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
        }
        return tryStart(appDetails)
    }
}
