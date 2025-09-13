package com.calai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.ui.MainViewModel
import com.calai.app.ui.UiState
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { DualApiScreen() } }
    }
}


@Composable
fun DualApiScreen(vm: MainViewModel = hiltViewModel()) {     // ★ 改用 hiltViewModel()
    val state by vm.state.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                //enabled = state !is UiState.Loading：載入中時禁用按鈕，避免連點。
                Button(onClick = vm::callHello, enabled = state !is UiState.Loading) { Text("呼叫 hello()") }
                Button(onClick = vm::callInfo,  enabled = state !is UiState.Loading) { Text("呼叫 info()") }
            }
            Spacer(Modifier.height(20.dp))
            /**
             * 把 畫面要顯示的字串用 when 依 UiState 決定：
             * Idle：初始提示
             * Loading：載入中文字樣
             * Hello：顯示字串結果
             * Info：從 JSON DTO 取兩個欄位
             * Error：顯示錯誤訊息
             */
            val text = when (val s = state) {
                UiState.Idle    -> "點按上方按鈕測試"
                UiState.Loading -> "請稍候…"
                is UiState.Hello -> s.text
                is UiState.Info  -> "message=${s.data.message}\nserverTime=${s.data.serverTime}"
                is UiState.Error -> "錯誤：${s.msg}"
            }
            //Text(text) 是 Jetpack Compose 的一個 UI 元件（Composable），用來在畫面上「畫出一段字串」。
            Text(text)
        }
    }
}

//@Composable
//fun DualApiScreen() {
//    val scope = rememberCoroutineScope()
//    var text by remember { mutableStateOf("點上面的按鈕測試 API") }
//    var loading by remember { mutableStateOf(false) }
//
//    Surface(Modifier.fillMaxSize()) {
//        Column(
//            Modifier.fillMaxSize().padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                Button(enabled = !loading, onClick = {
//                    loading = true
//                    scope.launch {
//                        text = runCatching { ApiClient.api.hello() }
//                            .getOrElse { "hello() 失敗：${it.message}" }
//                        loading = false
//                    }
//                }) { Text(if (loading) "請稍候…" else "呼叫 hello()") }
//
//                Button(enabled = !loading, onClick = {
//                    loading = true
//                    scope.launch {
//                        text = runCatching {
//                            val info = ApiClient.api.info()
//                            "message=${info.message}\nserverTime=${info.serverTime}"
//                        }.getOrElse { "info() 失敗：${it.message}" }
//                        loading = false
//                    }
//                }) { Text(if (loading) "請稍候…" else "呼叫 info()") }
//            }
//
//            Spacer(Modifier.height(20.dp))
//            Text(text)
//        }
//    }
//}
