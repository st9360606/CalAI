package com.calai.app.ui.home.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet
import com.calai.app.ui.home.ui.workout.components.PresetWorkoutRow
import com.calai.app.ui.home.ui.workout.components.WorkoutConfirmDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutEstimatingDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutScanFailedDialog
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import kotlinx.coroutines.delay

/**
 * Workout Tracker 主畫面（白底、黑字）
 * - vm 由外部傳入（NavHost 已用 HiltViewModelFactory 建好）
 * - 整頁只用一個 LazyColumn，避免 nested scroll crash
 */
@Composable
fun WorkoutTrackerScreen(
    onClose: () -> Unit,
    vm: WorkoutViewModel
) {
    val ui by vm.ui.collectAsState()

    // 初始化 (載入 presets / today)
    LaunchedEffect(Unit) {
        vm.init()
    }

    // 用 Box 包住 LazyColumn，讓我們可以在上面疊 Toast/Dialogs
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 白底
    ) {

        // 單一可捲動容器：LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentPadding = PaddingValues(bottom = 200.dp) // 原本你預留底部空間
        ) {

            // === Header + 自由輸入區塊 ===
            item {
                // Header Row: "Workout Tracker" + X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workout Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFF111114),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "✕",
                        color = Color(0xFF111114),
                        modifier = Modifier
                            .clickable { onClose() }
                            .padding(8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 自由輸入多行文字框
                OutlinedTextField(
                    value = ui.textInput,
                    onValueChange = { vm.onTextChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = {
                        Text(
                            "Examples: 45 min cycling",
                            color = Color(0xFF6B7280)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF111114)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6B7280),
                        unfocusedBorderColor = Color(0xFF6B7280),
                        focusedTextColor = Color(0xFF111114),
                        unfocusedTextColor = Color(0xFF111114)
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Add Workout 按鈕
                Button(
                    onClick = { vm.estimate() }, // WS2: POST /estimate
                    enabled = ui.textInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111114),
                        contentColor = Color.White
                    )
                ) {
                    Text("Add Workout")
                }

                Spacer(Modifier.height(24.dp))

                // "or select from the list" 分隔區
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE5E7EB)
                    )
                    Text(
                        text = "or select from the list",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE5E7EB)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // === 預設運動清單（Walking / Running ...） ===
            items(ui.presets) { preset: PresetWorkoutDto ->
                PresetWorkoutRow(
                    preset = preset,
                    onClickPlus = { vm.openDurationPicker(preset) }
                )
                HorizontalDivider(color = Color(0xFF4B5563).copy(alpha = 0.3f))
            }
        }

        // ===== 成功儲存 Toast (3.jpg) 疊在畫面最上方 =====
        ui.toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFF111114),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            LaunchedEffect(msg) {
                delay(2000)
                vm.clearToast()
            }
        }
    }

    // ====== Dialog / BottomSheets Overlay (跟之前一樣) ======

    // Loading (5.jpg)
    if (ui.estimating) {
        WorkoutEstimatingDialog(
            onDismiss = { /* 不允許中斷 loading */ }
        )
    }

    // 成功估算後的確認視窗 (6.jpg)
    ui.estimateResult?.let { r ->
        WorkoutConfirmDialog(
            result = r,
            onSave = { vm.confirmSaveFromEstimate() }, // WS3: /log -> toast + 更新 Home
            onCancel = { vm.dismissDialogs() }
        )
    }

    // 估算失敗 (7.jpg)
    if (ui.errorScanFailed) {
        WorkoutScanFailedDialog(
            onTryAgain = { vm.dismissDialogs() },
            onCancel = { vm.dismissDialogs() }
        )
    }

    // 預設運動 > 選時間 (2.jpg)
    ui.showDurationPickerFor?.let { preset ->
        DurationPickerSheet(
            presetName = preset.name,
            onSave = { minutes -> vm.savePresetDuration(minutes) }, // WS4: 直接 /log 紀錄
            onCancel = { vm.dismissDialogs() }
        )
    }
}

