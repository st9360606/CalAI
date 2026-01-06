package com.calai.app.data.activity.healthconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.health.connect.client.PermissionController

/**
 * 用來「保證」打開 Health Connect 的 App-specific 權限頁。
 * 不依賴 Compose 的 LocalActivityResultRegistryOwner。
 *
 * ✅ 這裡順手維護「拒絕次數」：
 * - 若此次 request 後仍未完整授權 → deniedCount + 1
 * - 若完整授權 → deniedCount reset
 */
class HealthConnectPermissionProxyActivity : ComponentActivity() {

    private var requiredPerms: Set<String> = emptySet()

    private val requestPerms =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            Log.d(TAG, "HC permission result: granted=${granted.size} $granted")

            val allGranted = requiredPerms.isNotEmpty() && granted.containsAll(requiredPerms)
            if (allGranted) {
                HealthConnectPermissionPrefs.resetDeniedCount(this)
            } else {
                // 使用者拒絕 / 取消 / 部分授權，都算一次「未成功授權」
                HealthConnectPermissionPrefs.incrementDeniedCount(this)
            }

            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = intent.getStringArrayListExtra(EXTRA_PERMS)
        if (perms.isNullOrEmpty()) {
            finish()
            return
        }

        requiredPerms = perms.toSet()
        requestPerms.launch(requiredPerms)
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
