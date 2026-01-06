package com.calai.app.ui.home.ui.camera.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class CameraPermissionProxyActivity : ComponentActivity() {

    private val requestPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                CameraPermissionPrefs.resetCameraDeniedCount(this)
            } else {
                // ✅ 只累積次數，不做任何跳設定（由 Home 第三次點擊來做）
                CameraPermissionPrefs.incrementCameraDeniedCount(this)
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPerm.launch(Manifest.permission.CAMERA)
    }

    companion object {
        fun start(ctx: Context) {
            val intent = Intent(ctx, CameraPermissionProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    }
}
