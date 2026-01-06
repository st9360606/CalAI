package com.calai.app.ui.home.ui.camera.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

/**
 * 用來「保證」能彈出 CAMERA 權限請求（不依賴 Compose 的 LocalActivityResultRegistryOwner）。
 */
/**
 * 用來「保證」能彈出 CAMERA 權限請求（不依賴 Compose 的 LocalActivityResultRegistryOwner）。
 */
class CameraPermissionProxyActivity : ComponentActivity() {

    private val requestPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // ✅ 授權成功：清掉 deniedOnce
                CameraPermissionPrefs.setCameraDeniedOnce(this, false)
            } else {
                // ✅ 拒絕：記錄「拒絕過一次」
                CameraPermissionPrefs.setCameraDeniedOnce(this, true)

                // 若使用者拒絕且 shouldShow... == false，通常代表「不再詢問」或政策阻擋 → 直接導設定
                val dontAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
                if (dontAskAgain) {
                    openCameraPermissionSettings(this)
                }
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

