package com.calai.app.ui.home.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.ui.home.components.ScrollingNumberWheel
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet
import com.calai.app.ui.home.ui.workout.components.FixedModalSheet
import com.calai.app.ui.home.ui.workout.components.trackerSheetHeight
import com.calai.app.ui.home.ui.workout.model.WorkoutUiState
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 色票
private val Black = Color(0xFF111114)
private val Gray300 = Color(0xFFE5E7EB)
private val Gray600 = Color(0xFF4B5563)
private val DividerGray = Color(0xFFD1D5DB)
private val TextPrimary = Color(0xFF111114)
private val TextSecondary = Color(0xFF4B5563)
private val HandleGray = Color(0xFF9CA3AF)
private val DarkSurface = Color(0xFF111114)
private val Green = Color(0xFF4CAF50)
private val GrayBtn = Color(0xFF374151)

/** 面板模式（都在同一顆 Sheet 內切換） */
private sealed interface SheetMode {
    data object Tracker : SheetMode
    data object Estimating : SheetMode
    data class Result(val result: EstimateResponse) : SheetMode
    data object Failed : SheetMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutUnifiedSheet(
    vm: WorkoutViewModel,
    visible: Boolean,
    onClose: () -> Unit,
    onCollapse: () -> Unit // ← 外部控制：只收合 UnifiedSheet
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // 依 VM 狀態決定畫面內容
    val mode: SheetMode = when {
        ui.estimating -> SheetMode.Estimating
        ui.estimateResult != null -> SheetMode.Result(ui.estimateResult!!)
        ui.errorScanFailed -> SheetMode.Failed
        else -> SheetMode.Tracker
    }

    val sheetH = trackerSheetHeight()
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // 控制外部 DurationPickerSheet 顯示
    var showDurationPicker by remember { mutableStateOf(false) }
    var currentPreset by remember { mutableStateOf<PresetWorkoutDto?>(null) }

    val onAddWorkoutClick: () -> Unit = l@{
        if (ui.textInput.isBlank()) return@l
        vm.estimateWithSpinner()
        scope.launch {
            delay(50)
            keyboard?.hide()
        }
    }

    // 點＋：設定 VM 目前的 preset，顯示時長選單，並「只收合」UnifiedSheet
    val onClickPresetPlus: (PresetWorkoutDto) -> Unit = { preset ->
        currentPreset = preset
        vm.openDurationPicker(preset) // 供 savePresetDuration 使用
        showDurationPicker = true     // 顯示外部 ModalBottomSheet
        onCollapse()                  // ★ 僅收合 UnifiedSheet，不清 VM 狀態
    }

    val onSaveDuration: (Int) -> Unit = { minutes ->
        if (minutes > 0) {
            vm.savePresetDuration(minutes) // 寫 DB → todayStore → toastMessage
            showDurationPicker = false
        }
    }

    val onFlowSave: () -> Unit = { vm.confirmSaveFromEstimate() }
    val onFlowTryAgain: () -> Unit = { vm.dismissDialogs() }
    val onFlowCancel: () -> Unit = { vm.dismissDialogs() }

    // 主固定底部面板
    FixedModalSheet(
        visible = visible,
        onDismissRequest = {
            vm.dismissDialogs()
            onClose()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetH)
                .background(Color.White)
                .padding(horizontal = 24.dp)
                .imePadding()
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    val enter = slideInHorizontally(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        initialOffsetX = { it }
                    ) + fadeIn(tween(120))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(140, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it / 2 }
                    ) + fadeOut(tween(120))
                    enter togetherWith exit
                },
                modifier = Modifier.fillMaxSize()
            ) { m ->
                when (m) {
                    is SheetMode.Tracker -> TrackerContent(
                        uiState = ui,
                        onClose = {
                            vm.dismissDialogs()
                            onClose()
                        },
                        onTextChanged = vm::onTextChanged,
                        onAddWorkout = onAddWorkoutClick,
                        onClickPresetPlus = onClickPresetPlus
                    )
                    is SheetMode.Estimating -> EstimatingContent()
                    is SheetMode.Result -> ResultContent(
                        result = m.result,
                        onSave = onFlowSave,
                        onCancel = onFlowCancel
                    )
                    is SheetMode.Failed -> FailedContent(
                        onTryAgain = onFlowTryAgain,
                        onCancel = onFlowCancel
                    )
                }
            }
        }
    }

    // 獨立的 DurationPickerSheet（和 UnifiedSheet 分離顯示）
    if (showDurationPicker && currentPreset != null) {
        DurationPickerSheet(
            presetName = currentPreset!!.name,
            onSaveMinutes = onSaveDuration,
            onCancel = { showDurationPicker = false }
        )
    }
}

/* ==================== 內容區（同一顆 Sheet 內） ==================== */

@Composable
private fun TrackerContent(
    uiState: WorkoutUiState,
    onClose: () -> Unit,
    onTextChanged: (String) -> Unit,
    onAddWorkout: () -> Unit,
    onClickPresetPlus: (PresetWorkoutDto) -> Unit
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad: Dp = navBottom + 12.dp

    var expanded by rememberSaveable { mutableStateOf(false) }
    val initialLimit = 20
    val totalCount = uiState.presets.size
    val presetsToShow = if (expanded) uiState.presets else uiState.presets.take(initialLimit)
    val remaining = (totalCount - initialLimit).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPad)
    ) {
        item {
            HeaderSection(
                title = "Workout Tracker",
                subtitle = "Describe the Type of Exercise and the duration",
                onClose = onClose
            )

            val thinBorder = 0.8.dp
            OutlinedTextField(
                value = uiState.textInput,
                onValueChange = { onTextChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .border(thinBorder, Gray300, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp)),
                placeholder = { Text("Examples: 45 min Running", color = Gray600) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(20.dp))

            val isEnabled = uiState.textInput.isNotBlank()
            Button(
                onClick = onAddWorkout,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                // ✅ 圓角更大（原本 16.dp → 改 28.dp）
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black,
                    contentColor = Color.White,
                    disabledContainerColor = Black,
                    disabledContentColor = Color.White
                )
            ) {
                // ✅ 字體放大（bodyLarge → titleMedium）
                Text(
                    text = "Add Workout",
                    color = Color.White,
                    // ✅ 稍大一點（bodyLarge）但保持 Medium 字重，不會太粗
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                )
            }

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
            PresetWorkoutRow(
                preset = preset,
                onClickPlus = { onClickPresetPlus(preset) }
            )
            HorizontalDivider(color = Gray300)
        }

        if (totalCount > initialLimit) {
            item {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { expanded = !expanded }) {
                        val label = if (expanded) "Show less" else "Show more ($remaining)"
                        Text(text = label, color = Black, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DurationContent(
    presetName: String,
    onSaveMinutes: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    // ✅ 彈窗高度改為螢幕的 45%
    val maxSheetHeight = (screenHeightDp * 0.45f).dp

    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(30) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight() // ✅ 不再撐滿
            .padding(bottom = 24.dp)
    ) {
        // 標題
        Text(
            text = presetName,
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Add this workout time to your activity log",
            color = Gray600,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(24.dp))

        // 時間選擇區
        val rowItemHeight = 48.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .weight(1f, fill = false), // ✅ 不填滿
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(rowItemHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF2F2F2))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollingNumberWheel(
                    value = hours,
                    range = 0..12,
                    onValueChange = { hours = it },
                    textColor = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("hr", color = Gray600, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(24.dp))
                ScrollingNumberWheel(
                    value = minutes,
                    range = 0..59,
                    onValueChange = { minutes = it },
                    textColor = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("min", color = Gray600, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 底部按鈕
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Button(
                onClick = {
                    val total = hours * 60 + minutes
                    if (total > 0) onSaveMinutes(total)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5E7EB),
                    contentColor = Black
                )
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}


@Composable
private fun EstimatingContent() {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Green)
        )
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Estimating effort, calculating calories...", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(6.dp))
            Text("Please do not close the app or lock your device", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultContent(result: EstimateResponse, onSave: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(Green))
            Spacer(Modifier.height(24.dp))
            Text("${result.minutes ?: 0} min ${result.activityDisplay.orEmpty()}", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("${result.kcal ?: 0} kcal", color = Color.White, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkSurface)
            ){ Text("Save Activity", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayBtn, contentColor = Color.White)
            ){ Text("Cancel", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun FailedContent(onTryAgain: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
            Spacer(Modifier.height(16.dp))
            Text("Uh-oh! Scan Failed", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("The activity description may be incorrect, or the internet connection is weak.",
                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkSurface)
            ){ Text("Try Again", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayBtn, contentColor = Color.White)
            ){ Text("Cancel", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/* --- 共用元件 --- */
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
        // 頂部小把手
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(color = HandleGray.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.height(20.dp))

        // 標題與關閉按鈕
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            // ✅ 改用 Box + clickable 完全控制尺寸
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp) // ← 黑圓底更小
                    .clip(CircleShape)
                    .background(Black)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp) // ← X 更大
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // 副標題
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

@Composable
private fun PresetWorkoutRow(
    preset: PresetWorkoutDto,
    onClickPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
        FilledIconButton(
            onClick = onClickPlus,
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Black,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "add preset")
        }
    }
}
