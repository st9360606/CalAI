package com.calai.app.ui.home.ui.camera.components

import android.content.Context
import androidx.core.content.edit

/**
 * 用 SharedPreferences 記住「相機權限曾被拒絕一次」。
 * 目的：第二次點 FAB 時，直接導到系統權限設定頁（你截圖那種）。
 *
 * 之所以用 SP：
 * - ProxyActivity 也能寫入（owner=null 情境）
 * - 不用改 DataStore schema
 */
object CameraPermissionPrefs {
    private const val PREF_NAME = "permission_prefs"
    private const val KEY_CAMERA_DENIED_ONCE = "camera_denied_once"

    fun isCameraDeniedOnce(ctx: Context): Boolean {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAMERA_DENIED_ONCE, false)
    }

    fun setCameraDeniedOnce(ctx: Context, deniedOnce: Boolean) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_CAMERA_DENIED_ONCE, deniedOnce)
            }
    }
}
