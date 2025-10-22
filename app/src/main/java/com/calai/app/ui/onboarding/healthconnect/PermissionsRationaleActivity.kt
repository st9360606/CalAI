package com.calai.app.ui.onboarding.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 供 Health Connect 在授權畫面連結「為何需要權限」時呼叫的頁面（rationale）。
 * 只要顯示用途說明，按下「了解」或返回即可 finish()。
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RationaleScreen(onClose = { finish() })
        }
    }
}

@Composable
private fun RationaleScreen(onClose: () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)) {
                Text(
                    text = "為何需要 Health Connect 權限",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "我們會讀取「步數、運動、睡眠」用於計算每日消耗、活動時段與睡眠分析，" +
                            "用來更新卡路里儀表板與歷史圖表。未經你的同意不會上傳雲端；你可在 Health Connect 隨時撤銷授權。"
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onClose) {
                    Text("了解")
                }
            }
        }
    }
}
