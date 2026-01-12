package com.calai.app.ui.home.ui.foodlog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calai.app.data.foodlog.model.FoodLogStatus
import com.calai.app.ui.home.ui.foodlog.model.FoodLogFlowViewModel

@Composable
fun FoodLogDetailScreen(
    foodLogId: String,
    vm: FoodLogFlowViewModel,
    onBack: () -> Unit,
    onOpenEditor: (foodLogId: String) -> Unit, // DRAFT → 你的「可編輯結果頁」
) {
    val st by vm.state.collectAsState()

    LaunchedEffect(foodLogId) {
        vm.startPolling(foodLogId)
    }

    val env = st.envelope

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("FoodLog: $foodLogId", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onBack) { Text("關閉") }
        }

        Spacer(Modifier.height(12.dp))

        if (st.loading && (env == null || env.status == FoodLogStatus.PENDING)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        if (st.error != null) {
            Text("錯誤：${st.error}", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        if (env == null) return@Column

        when (env.status) {
            FoodLogStatus.PENDING -> {
                Text("處理中…")
                Text("建議輪詢秒數：${env.task?.pollAfterSec ?: 2}")
            }
            FoodLogStatus.FAILED -> {
                Text("辨識失敗：${env.error?.errorCode ?: "UNKNOWN"}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.retry(foodLogId) }) { Text("重試") }
            }
            FoodLogStatus.DRAFT -> {
                Text("已完成（可編輯）")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onOpenEditor(foodLogId) }) { Text("編輯結果") }
            }
            FoodLogStatus.SAVED -> Text("已存入歷史")
            FoodLogStatus.DELETED -> Text("已刪除")
        }
    }
}
