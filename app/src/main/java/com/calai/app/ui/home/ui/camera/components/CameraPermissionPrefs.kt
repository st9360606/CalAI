package com.calai.app.ui.home.ui.camera.components

import android.content.Context
import androidx.core.content.edit

object CameraPermissionPrefs {
    private const val PREF_NAME = "permission_prefs"

    // ✅ 新 key：拒絕次數
    private const val KEY_CAMERA_DENIED_COUNT = "camera_denied_count"

    // ✅ 舊 key：你之前用過的 boolean
    private const val KEY_CAMERA_DENIED_ONCE = "camera_denied_once"

    /**
     * ✅ 重要：若偵測到舊的 denied_once，直接視為「0 次」並清掉舊 key。
     * 目的：讓新規格確保「至少問兩次」，不要被舊資料污染造成第二次就跳設定。
     */
    fun getCameraDeniedCount(ctx: Context): Int {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // --- 遷移：發現舊 key 就重置 ---
        if (sp.contains(KEY_CAMERA_DENIED_ONCE) && !sp.contains(KEY_CAMERA_DENIED_COUNT)) {
            sp.edit {
                remove(KEY_CAMERA_DENIED_ONCE)
                putInt(KEY_CAMERA_DENIED_COUNT, 0)
            }
            return 0
        }

        return sp.getInt(KEY_CAMERA_DENIED_COUNT, 0).coerceAtLeast(0)
    }

    fun setCameraDeniedCount(ctx: Context, count: Int) {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit {
            putInt(KEY_CAMERA_DENIED_COUNT, count.coerceAtLeast(0))
            remove(KEY_CAMERA_DENIED_ONCE) // ✅ 永遠移除舊 key，避免回退污染
        }
    }

    fun incrementCameraDeniedCount(ctx: Context): Int {
        val next = getCameraDeniedCount(ctx) + 1
        setCameraDeniedCount(ctx, next)
        return next
    }

    fun resetCameraDeniedCount(ctx: Context) {
        setCameraDeniedCount(ctx, 0)
    }
}
