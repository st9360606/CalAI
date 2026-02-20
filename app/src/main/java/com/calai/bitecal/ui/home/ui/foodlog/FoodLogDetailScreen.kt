package com.calai.bitecal.ui.home.ui.foodlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calai.bitecal.data.foodlog.model.ClientAction
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.foodlog.model.ModelTier
import com.calai.bitecal.ui.home.ui.foodlog.model.FoodLogFlowViewModel
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun FoodLogDetailScreen(
    foodLogId: String,
    vm: FoodLogFlowViewModel,
    onBack: () -> Unit,
    onOpenEditor: (foodLogId: String) -> Unit,
    onGoLabel: () -> Unit,        // 跳回相機並切 LABEL
    onRescanBarcode: () -> Unit,  // 跳回相機並切 BARCODE
) {
    val st by vm.state.collectAsState()

    LaunchedEffect(foodLogId) {
        vm.startPolling(foodLogId)
    }

    // ✅ 離開 Detail 就停掉 polling（避免背景覆寫 state）
    DisposableEffect(foodLogId) {
        onDispose { vm.stopPolling() }
    }

    val env = st.envelope

    // ✅ 建議：回相機切模式前 reset，避免回相機還殘留上一筆 toast/loading
    val goLabelSafe: () -> Unit = {
        vm.reset()
        onGoLabel()
    }
    val rescanBarcodeSafe: () -> Unit = {
        vm.reset()
        onRescanBarcode()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("FoodLog: $foodLogId", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onBack) { Text("關閉") }
        }

        // ✅ Debug badge：tierUsed/fromCache
        if (env != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "模型：${env.tierUsed.toDisplayName()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (env.fromCache) "快取命中" else "非快取",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ✅ 429：冷卻中
        st.cooldown?.let { cd ->
            CooldownPanel(
                cdSeconds = cd.cooldownSeconds,
                nextAllowedAtUtc = cd.nextAllowedAtUtc,
                reason = cd.cooldownReason,
                suggestedTier = cd.suggestedTier,
                onRetry = { vm.startPolling(foodLogId) }
            )
            return@Column
        }

        // ✅ 422：模型拒答（SAFETY/RECITATION/HARM）
        st.refused?.let { rf ->
            ModelRefusedPanel(
                refuseReason = rf.refuseReason,
                hint = rf.hint,
                onBackCamera = onBack,
                onGoLabel = goLabelSafe,
                onRescanBarcode = rescanBarcodeSafe
            )
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
                val retryAfter = env.error?.retryAfterSec
                val actionEnum = env.error?.clientAction

                Text(
                    text = "辨識失敗：${env.error?.errorCode ?: "UNKNOWN"}",
                    color = MaterialTheme.colorScheme.error
                )

                if (retryAfter != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("建議等待：${retryAfter}s")
                }

                Spacer(Modifier.height(10.dp))

                when (actionEnum) {
                    ClientAction.TRY_LABEL -> {
                        Button(onClick = goLabelSafe) { Text("改用營養標示（Label）") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = rescanBarcodeSafe) { Text("重新掃描條碼") }
                    }

                    ClientAction.TRY_BARCODE -> {
                        Button(onClick = rescanBarcodeSafe) { Text("改用條碼掃描（Barcode）") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = goLabelSafe) { Text("改用營養標示（Label）") }
                    }

                    ClientAction.CHECK_NETWORK -> {
                        Button(onClick = { vm.retry(foodLogId) }) { Text("已檢查網路，重試") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onBack) { Text("回到相機") }
                    }

                    ClientAction.RETAKE_PHOTO -> {
                        Button(onClick = onBack) { Text("回到相機重拍") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = goLabelSafe) { Text("改用營養標示（Label）") }
                    }

                    ClientAction.ENTER_MANUALLY -> {
                        Button(onClick = { onOpenEditor(foodLogId) }) { Text("手動輸入/編輯") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onBack) { Text("回到相機") }
                    }

                    ClientAction.CONTACT_SUPPORT -> {
                        Button(onClick = onBack) { Text("回到相機") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { /* TODO: open mailto/support */ }) { Text("聯絡客服") }
                    }

                    else -> {
                        // ✅ fallback：只有 actionEnum == null 才用 errCode 判斷 BARCODE_*
                        val isBarcodeErr = actionEnum == null && (errCode?.startsWith("BARCODE_") == true)

                        if (isBarcodeErr) {
                            Button(onClick = goLabelSafe) { Text("改用營養標示（Label）") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = rescanBarcodeSafe) { Text("重新掃描條碼") }
                        } else {
                            Button(onClick = { vm.retry(foodLogId) }) { Text("重試") }
                        }
                    }
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

@Composable
private fun CooldownPanel(
    cdSeconds: Long?,
    nextAllowedAtUtc: String?,
    reason: String?,
    suggestedTier: String?,
    onRetry: () -> Unit
) {
    val target = remember(nextAllowedAtUtc) {
        nextAllowedAtUtc?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

    fun computeRemainingSec(): Long {
        val now = Instant.now().epochSecond
        val byTarget = target?.epochSecond?.minus(now)
        val byDto = cdSeconds
        val rem = when {
            byTarget != null -> byTarget
            byDto != null -> byDto
            else -> 0L
        }
        return rem.coerceAtLeast(0L)
    }

    var remaining by remember(nextAllowedAtUtc, cdSeconds) { mutableStateOf(computeRemainingSec()) }

    LaunchedEffect(nextAllowedAtUtc, cdSeconds) {
        while (true) {
            remaining = computeRemainingSec()
            if (remaining <= 0L) break
            delay(1_000)
        }
    }

    Text("冷卻中，請稍候再試", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    Text(
        text = buildString {
            append("剩餘時間：")
            append(formatMmSs(remaining))
            if (!reason.isNullOrBlank()) {
                append("\n原因：")
                append(reason)
            }
            if (!suggestedTier.isNullOrBlank()) {
                append("\n建議模型：")
                append(suggestedTier)
            }
            if (!nextAllowedAtUtc.isNullOrBlank()) {
                append("\n解鎖時間(UTC)：")
                append(nextAllowedAtUtc)
            }
        },
        textAlign = TextAlign.Start
    )

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onRetry,
        enabled = remaining <= 0L
    ) {
        Text(if (remaining <= 0L) "再試一次" else "倒數中…")
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ModelRefusedPanel(
    refuseReason: String,
    hint: String?,
    onBackCamera: () -> Unit,
    onGoLabel: () -> Unit,
    onRescanBarcode: () -> Unit
) {
    val reason = refuseReason.uppercase()
    val title = when (reason) {
        "RECITATION" -> "內容不支援"
        "HARM_CATEGORY", "SAFETY" -> "無法識別"
        else -> "無法識別"
    }

    val msg = hint ?: when (reason) {
        "RECITATION" -> "這張內容可能涉及版權/引用限制，建議改拍「食物本體」或使用 Label/Barcode。"
        else -> "請只拍攝食物（避免人臉/不雅內容/螢幕截圖），並確保光線充足。"
    }

    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(8.dp))
    Text(msg)
    Spacer(Modifier.height(12.dp))

    Button(onClick = onBackCamera) { Text("回到相機重新拍") }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onGoLabel) { Text("改用營養標示（Label）") }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onRescanBarcode) { Text("改用條碼掃描（Barcode）") }
    Spacer(Modifier.height(16.dp))
}

private fun ModelTier?.toDisplayName(): String = when (this) {
    ModelTier.HIGH -> "高品質（HIGH）"
    ModelTier.LOW -> "省成本（LOW）"
    ModelTier.BARCODE -> "條碼（BARCODE）"
    null -> "未知"
}

private fun formatMmSs(sec: Long): String {
    val s = sec.coerceAtLeast(0L)
    val mm = (s / 60).toString().padStart(2, '0')
    val ss = (s % 60).toString().padStart(2, '0')
    return "$mm:$ss"
}
