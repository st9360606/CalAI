package com.calai.bitecal.ui.onboarding.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

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
                    text = stringResource(R.string.health_connect_rationale_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.health_connect_rationale_body)
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = rememberClickWithHaptic(onClick = onClose)) {
                    Text(stringResource(R.string.common_understood))
                }
            }
        }
    }
}
