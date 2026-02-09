package com.calai.bitecal.ui.home.ui.foodlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.ui.home.ui.foodlog.model.FoodLogFlowViewModel

@Composable
fun FoodLogDetailScreen(
    foodLogId: String,
    vm: FoodLogFlowViewModel,
    onBack: () -> Unit,
    onOpenEditor: (foodLogId: String) -> Unit,
    onGoLabel: () -> Unit,        // ✅ NEW：跳回相機並切 LABEL
    onRescanBarcode: () -> Unit,  // ✅ NEW：跳回相機並切 BARCODE
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

        // 429
        st.cooldown?.let { cd ->
            Text("冷卻中，請稍候再試", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildString {
                    append("剩餘秒數：")
                    append(cd.cooldownSeconds ?: "-")
                    append("\nnextAllowedAtUtc：")
                    append(cd.nextAllowedAtUtc ?: "-")
                    cd.cooldownReason?.let {
                        append("\n原因：")
                        append(it)
                    }
                },
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.startPolling(foodLogId) }) { Text("再試一次") }
            Spacer(Modifier.height(16.dp))
            return@Column
        }

        // 422
        st.refused?.let { rf ->
            Text("無法識別", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(rf.hint ?: "請只拍攝食物（避免人臉/不雅內容/螢幕截圖）")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("回到相機") }
            Spacer(Modifier.height(16.dp))
            return@Column
        }

        if (st.loading && (env == null || env.status == FoodLogStatus.PENDING)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        if (st.error != null) {
            Text("錯誤：${st.error}", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        if (env == null) {
            Text("尚未取得資料…")
            return@Column
        }

        when (env.status) {
            FoodLogStatus.PENDING -> {
                Text("處理中…")
                Text("建議輪詢秒數：${env.task?.pollAfterSec ?: 2}")
            }

            FoodLogStatus.FAILED -> {
                val errCode = env.error?.errorCode?.uppercase()
                val action = env.error?.clientAction?.uppercase()
                val retryAfter = env.error?.retryAfterSec

                Text(
                    "辨識失敗：${env.error?.errorCode ?: "UNKNOWN"}",
                    color = MaterialTheme.colorScheme.error
                )

                if (retryAfter != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("建議等待：${retryAfter}s")
                }

                Spacer(Modifier.height(10.dp))

                // ✅ Barcode：不顯示 vm.retry（避免誤扣 AI quota）
                val isBarcodeErr = errCode?.startsWith("BARCODE_") == true

                if (isBarcodeErr || action == "TRY_LABEL") {
                    Button(onClick = onGoLabel) { Text("改用營養標示（Label）") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRescanBarcode) { Text("重新掃描條碼") }
                } else {
                    // 其他失敗：才允許重試（走你原本 /retry）
                    Button(onClick = { vm.retry(foodLogId) }) { Text("重試") }
                }
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
