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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.ui.home.ui.workout.components.SuccessTopToast
import com.calai.app.ui.home.ui.workout.model.WorkoutUiState
import kotlinx.coroutines.delay

// 一些固定色
private val Black = Color(0xFF111114)
private val LightGrayBg = Color(0xFFF3F4F6)
private val Gray300 = Color(0xFFE5E7EB)
private val Gray500 = Color(0xFF6B7280)
private val Gray600 = Color(0xFF4B5563)
private val DividerGray = Color(0xFFD1D5DB)
private val TextPrimary = Color(0xFF111114)
private val TextSecondary = Color(0xFF4B5563)
private val HandleGray = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerSheet(
    uiState: WorkoutUiState,
    onClose: () -> Unit,
    onTextChanged: (String) -> Unit,
    onAddWorkout: () -> Unit,
    onClickPresetPlus: (PresetWorkoutDto) -> Unit,
    onToastCleared: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxSheetHeight = (screenHeightDp * 0.93f).dp

    val initialLimit = 20
    val totalCount = uiState.presets.size
    var expanded = rememberSaveable { false }

    val presetsToShow = if (expanded) uiState.presets else uiState.presets.take(initialLimit)
    val remaining = (totalCount - initialLimit).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = { onClose() },
        sheetState = sheetState,
        dragHandle = { /* 自定手把 */ },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        // 讓我們自行處理 insets（保留）
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        // ✅ 移除 navigationBarsPadding()，避免與 LazyColumn 的 contentPadding 重複
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .imePadding() // ✅ 鍵盤出現時頂起內容
        ) {
            // 動態計算底部 padding：導覽列高度 + 少許緩衝
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomPad: Dp = navBottom + 12.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                // ✅ 原本是 200.dp，改成動態 insets，消除大片空白
                contentPadding = PaddingValues(bottom = bottomPad)
            ) {
                item {
                    HeaderSection(
                        title = "Workout Tracker",
                        subtitle = "Describe the Type of Exercise and the duration",
                        onClose = onClose
                    )
                    OutlinedTextField(
                        value = uiState.textInput,
                        onValueChange = { onTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Examples: 45 min Running", color = Gray500) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = TextPrimary,
                            focusedContainerColor = LightGrayBg,
                            unfocusedContainerColor = LightGrayBg,
                            disabledContainerColor = LightGrayBg,
                            focusedBorderColor = Gray300,
                            unfocusedBorderColor = Gray300,
                            focusedPlaceholderColor = Gray500,
                            unfocusedPlaceholderColor = Gray500
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    val isEnabled = uiState.textInput.isNotBlank()
                    Button(
                        onClick = { onAddWorkout() },
                        enabled = isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Black,
                            contentColor = Color.White,
                            disabledContainerColor = Black,
                            disabledContentColor = Color.White
                        )
                    ) { Text("Add Workout", color = Color.White) }

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerGray)
                        Text(
                            text = "or select from the list",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray600
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerGray)
                    }

                    Spacer(Modifier.height(16.dp))
                }

                items(presetsToShow) { preset ->
                    PresetWorkoutRowLight(
                        preset = preset,
                        onClickPlus = { onClickPresetPlus(preset) }
                    )
                    HorizontalDivider(color = Gray300)
                }

                if (totalCount > initialLimit) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { expanded = !expanded }) {
                                val label = if (expanded) "Show less" else "Show more (${remaining})"
                                Text(text = label, color = Black, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // 成功提示（在頂部，不影響底部 padding）
            uiState.toastMessage?.let { msg ->
                SuccessTopToast(message = msg, modifier = Modifier.align(Alignment.TopCenter))
                LaunchedEffect(msg) {
                    delay(2000)
                    onToastCleared()
                }
            }
        }
    }
}

/**
 * Header:
 * - 上方手把
 * - 中間置中的 "Workout Tracker"
 * - 右上角關閉 X（黑圓底 + 白 X），跟 title 同一列
 * - Subtitle 在下面一行
 */
@Composable
private fun HeaderSection(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // 手把
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(
                    color = HandleGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(Modifier.height(12.dp))

        // title + X 同一列
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            // 關閉按鈕：黑圓底 + 白色 X
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "close",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 一行預設運動（Walking / Running / ...）
 *
 * - 左邊圓鈕：現在改成「黑色圓底」+ 白字母
 * - 中間：名稱 + "xxx kcal per 30 min"
 * - 右邊圓鈕：黑底 + 白色＋號（已是黑底）
 */
@Composable
private fun PresetWorkoutRowLight(
    preset: PresetWorkoutDto,
    onClickPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左邊圓鈕：黑圓底（更新需求 #1）
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF84CC16)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${preset.kcalPer30Min} kcal per 30 min",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.width(16.dp))

        // 右邊＋圓鈕：黑圓底（之前就已經做成黑底＋縮小 32dp）
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClickPlus() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "add preset",
                tint = Color.White
            )
        }
    }
}
