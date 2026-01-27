package com.calai.bitecal.ui.home.ui.camera.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * 嘗試打開你截圖那種「相機權限」設定頁。
 *
 * 注意：不同 OEM / Android 版本支援程度不同。
 * 我們採用「多個候選 Intent」：能 resolve 就用，不能就 fallback 到 App details。
 */
fun openCameraPermissionSettings(ctx: Context) {
    val pkg = ctx.packageName

    val candidates = listOf(
        // ① 一些系統/ROM 可直達 App 的 permission 設定
        Intent("android.settings.APP_PERMISSION_SETTINGS").apply {
            putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
            putExtra("android.intent.extra.PACKAGE_NAME", pkg)
            putExtra("android.intent.extra.PERMISSION_NAME", Manifest.permission.CAMERA)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },

        // ② 某些 ROM 用這個 action
        Intent("android.settings.MANAGE_APP_PERMISSIONS").apply {
            putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
            putExtra("android.intent.extra.PACKAGE_NAME", pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },

        // ③ 最保險 fallback：App 詳細資訊（使用者再點「權限」→「相機」）
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", pkg, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )

    val pm = ctx.packageManager
    val target = candidates.firstOrNull { it.resolveActivity(pm) != null }
        ?: candidates.last()

    runCatching { ctx.startActivity(target) }
}
