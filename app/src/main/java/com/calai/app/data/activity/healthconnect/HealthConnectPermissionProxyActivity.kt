// app/src/main/java/com/calai/app/data/activity/healthconnect/HealthConnectPermissionProxyActivity.kt
package com.calai.app.data.activity.healthconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.health.connect.client.PermissionController

/**
 * 用來「保證」打開 Health Connect 的 App-specific 權限頁（你要的 8170）。
 * 不依賴 Compose 的 LocalActivityResultRegistryOwner。
 */
class HealthConnectPermissionProxyActivity : ComponentActivity() {

    private val requestPerms =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            Log.d(TAG, "HC permission result: granted=${granted.size} $granted")
            // 這裡不要做太多事；回到 Home 後用 onResume/refresh 再抓資料
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = intent.getStringArrayListExtra(EXTRA_PERMS)
        if (perms.isNullOrEmpty()) {
            finish()
            return
        }
        requestPerms.launch(perms.toSet())
    }

    companion object {
        private const val TAG = "HC_PROXY"
        private const val EXTRA_PERMS = "extra_hc_perms"

        fun start(ctx: Context, permissions: Set<String>) {
            val i = Intent(ctx, HealthConnectPermissionProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putStringArrayListExtra(EXTRA_PERMS, ArrayList(permissions))
            }
            ctx.startActivity(i)
        }
    }
}
