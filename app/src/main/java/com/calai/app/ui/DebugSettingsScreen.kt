package com.calai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.BuildConfig

@Composable
fun DebugSettingsScreen(
    vm: SettingsViewModel = hiltViewModel()
) {
    val token by vm.tokenState.collectAsState()

    var input by remember(token) { mutableStateOf(token.orEmpty()) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("設定（Debug）", style = MaterialTheme.typography.titleLarge)

        // 環境資訊
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Base URL：${BuildConfig.BASE_URL}")
                Text("Variant：${BuildConfig.BUILD_TYPE} / ${BuildConfig.FLAVOR.ifEmpty { "noFlavor" }}")
                val tokenPreview = token?.let { if (it.length > 6) it.take(3) + "•••" + it.takeLast(3) else it }
                Text("Token 狀態：${tokenPreview ?: "（未設定）"}")
            }
        }

        // Token 編輯
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Auth Token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                vm.saveToken(input.trim())
                savedMsg = "已儲存 Token（請看 OkHttp Log 驗證 Header）"
            }) { Text("儲存 Token") }

            OutlinedButton(onClick = {
                vm.clearToken()
                input = ""
                savedMsg = "已清除 Token"
            }) { Text("清除 Token") }
        }

        if (savedMsg != null) {
            Text(savedMsg!!, color = MaterialTheme.colorScheme.primary)
        }

        Divider(Modifier.padding(vertical = 8.dp))

        Text("小提醒：", style = MaterialTheme.typography.titleMedium)
        Text("1) 設定 Token 後，對後端的請求會自動帶 Authorization: Bearer <token>")
        Text("2) 清除 Token 可模擬未登入/過期行為（看 401 處理是否運作）")
    }
}
