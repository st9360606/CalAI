package com.calai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.ui.*
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var tab by remember { mutableStateOf(0) }
                val titles = listOf("API 測試", "設定（Debug）")

                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("Cal AI") }) },
                    bottomBar = {
                        TabRow(selectedTabIndex = tab) {
                            titles.forEachIndexed { i, title ->
                                Tab(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    text = { Text(title) }   // ✅ 用位置參數，別用 Text(text = ...)
                                )
                            }
                        }
                    }
                ) { inner ->
                    Box(Modifier.padding(inner)) {
                        when (tab) {
                            0 -> DualApiScreen()
                            1 -> DebugSettingsScreen()
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DualApiScreen(vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = vm::callHello, enabled = state !is UiState.Loading) { Text("呼叫 hello()") }
            Button(onClick = vm::callInfo,  enabled = state !is UiState.Loading) { Text("呼叫 info()") }
        }
        Spacer(Modifier.height(20.dp))

        when (val s = state) {
            UiState.Idle     -> Text("點按上方按鈕測試")
            UiState.Loading  -> CircularProgressIndicator()
            is UiState.Hello -> Text(s.text)
            is UiState.Info  -> Text("message=${s.message}\nserverTime=${s.serverTime}")
            is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("錯誤：${s.msg}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = s.retry) { Text("重試") }
            }
        }
    }
}
