package com.calai.app.ui.home.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.calai.app.data.workout.api.PresetWorkoutDto
import androidx.compose.material3.ExperimentalMaterial3Api
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet
import com.calai.app.ui.home.ui.workout.components.PresetWorkoutRowDark
import com.calai.app.ui.home.ui.workout.components.WorkoutConfirmDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutEstimatingDialog
import com.calai.app.ui.home.ui.workout.components.WorkoutScanFailedDialog
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel

/**
 * Workout Tracker bottom sheet
 * - 如 1.jpg：可以上彈/下滑關閉
 * - 標題置中，往下一點，右上圓形深灰 X
 * - 下方有自由輸入 + Add Workout
 * - "or select from the list" + 預設活動列表 (Walking...)
 * - 點每列右邊的「＋」→ 彈出 DurationPickerSheet (2.jpg)
 * - Save → vm.savePresetDuration(minutes) → 寫 DB + 更新今日鍛鍊歷史
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerSheet(
    vm: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val ui by vm.ui.collectAsState()

    // BottomSheet 狀態：支援往下 swipe 關閉
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // 初始化預設活動、今日資料
    LaunchedEffect(Unit) {
        vm.init()
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        // 我們自己畫 handle，不用預設
        dragHandle = {},
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color(0xFF1A1A1A), // 深色底，貼近 1.jpg
        tonalElevation = 0.dp
    ) {
        // 用 Box 包住 LazyColumn，這樣可以把 toast 疊在上面 (不會影響 scroll)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // 避免底部手勢條遮住
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentPadding = PaddingValues(bottom = 200.dp)
            ) {

                // ===== Header + 輸入區 =====
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp) // "往下一點"
                    ) {
                        // 中央區塊：handle + title + desc
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 上方小手把
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        color = Color(0xFF9CA3AF).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Workout Tracker",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Describe the Type of Exercise and the duration",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9CA3AF),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(16.dp))
                        }

                        // 右上角 X：深灰圓底 + 白色 X
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 32.dp)
                                .size(36.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onDismiss() },
                            shape = CircleShape,
                            color = Color(0xFF4B5563)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "close",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // ==== 自由輸入框 (多國語言、使用者自己打 "15 min walking") ====
                    OutlinedTextField(
                        value = ui.textInput,
                        onValueChange = { vm.onTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = {
                            Text(
                                text = "Examples: 45 min cycling",
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4B5563),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedPlaceholderColor = Color(0xFF9CA3AF),
                            unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // ==== Add Workout 按鈕 -> 走估算流程 (WS2) ====
                    Button(
                        onClick = { vm.estimate() },
                        enabled = ui.textInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2A2A),
                            contentColor = Color(0xFF9CA3AF),
                            disabledContainerColor = Color(0xFF2A2A2A),
                            disabledContentColor = Color(0xFF4B5563)
                        )
                    ) {
                        Text("Add Workout")
                    }

                    Spacer(Modifier.height(24.dp))

                    // ==== "or select from the list" 分隔線 ====
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            text = "or select from the list",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4B5563)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // ===== 預設運動清單 (Walking / Running / ...) =====
                items(ui.presets) { preset: PresetWorkoutDto ->
                    PresetWorkoutRowDark(
                        preset = preset,
                        onClickPlus = {
                            // WS4：跳出 (2.jpg) 的時間選擇底板
                            vm.openDurationPicker(preset)
                        }
                    )
                    HorizontalDivider(color = Color(0xFF4B5563))
                }
            }

            // === 儲存完的 Toast (成功訊息) ===
            ui.toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
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
    }

    // === 以下是那些彈出式流程畫面 ===
    // 1. 估算中動畫 (5.jpg)
    if (ui.estimating) {
        WorkoutEstimatingDialog(
            onDismiss = { /* loading 時不允許手動關 */ }
        )
    }

    // 2. 估算成功後顯示卡路里 (6.jpg)
    ui.estimateResult?.let { r ->
        WorkoutConfirmDialog(
            result = r,
            onSave = { vm.confirmSaveFromEstimate() }, // WS3: /log -> 存到DB -> 更新今天
            onCancel = { vm.dismissDialogs() }
        )
    }

    // 3. 找不到/無法解析 → Scan Failed (7.jpg)
    if (ui.errorScanFailed) {
        WorkoutScanFailedDialog(
            onTryAgain = { vm.dismissDialogs() },
            onCancel = { vm.dismissDialogs() }
        )
    }

    // 4. 按預設活動右邊的 + → 時間選擇底板 (2.jpg)
    ui.showDurationPickerFor?.let { preset ->
        DurationPickerSheet(
            presetName = preset.name,
            onSave = { minutes ->
                // WS4: Save -> DB -> 寫入今日鍛鍊歷史
                vm.savePresetDuration(minutes)
            },
            onCancel = { vm.dismissDialogs() }
        )
    }
}

