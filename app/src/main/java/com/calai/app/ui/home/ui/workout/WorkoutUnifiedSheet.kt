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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.ui.home.ui.workout.components.DurationPickerSheet
import com.calai.app.ui.home.ui.workout.components.FixedModalSheet
import com.calai.app.ui.home.ui.workout.components.trackerSheetHeight
import com.calai.app.ui.home.ui.workout.model.WorkoutUiState
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// 色票
private val Black = Color(0xFF111114)
private val Gray300 = Color(0xFFE5E7EB)
private val Gray600 = Color(0xFF4B5563)
private val DividerGray = Color(0xFFD1D5DB)
private val TextPrimary = Color(0xFF111114)
private val TextSecondary = Color(0xFF4B5563)
private val HandleGray = Color(0xFF9CA3AF)
private val DarkSurface = Color(0xFF111114)
private val Green = Color(0xFF84CC16)
private val GrayBtn = Color(0xFF374151)
private val TrackGray = Color(0xFFE6E9EF) // 很淡的灰，近截圖
private val Amber = Color(0xFFF59E0B)
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
                    is SheetMode.Estimating -> Column(Modifier.fillMaxSize()) {
                        SimpleHeaderBar(
                            title = "Workout Tracker",
                            onClose = { vm.dismissDialogs(); onClose() } // ★ 用具名參數
                        )
                        Spacer(Modifier.height(4.dp))
                        EstimatingContent()
                    }

                    is SheetMode.Result -> Column(Modifier.fillMaxSize()) {
                        SimpleHeaderBar(
                            title = "Workout Tracker",
                            onClose = { vm.dismissDialogs(); onClose() } // ★ 用具名參數
                        )
                        Spacer(Modifier.height(4.dp))
                        ResultContent(result = m.result, onSave = onFlowSave, onCancel = onFlowCancel)
                    }

                    is SheetMode.Failed -> Column(Modifier.fillMaxSize()) {
                        SimpleHeaderBar(
                            title = "Workout Tracker",
                            onClose = { vm.dismissDialogs(); onClose() } // ★ 用具名參數
                        )
                        Spacer(Modifier.height(4.dp))
                        FailedContent(onTryAgain = onFlowTryAgain, onCancel = onFlowCancel)
                    }
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

/**
 * 簡單輪播文字：每 intervalMs 切換下一句，使用淡入/淡出轉場。
 * - phrases：要輪播的多句文案（至少 1 句）
 * - intervalMs：每句顯示時間，預設 1600ms
 */
@Composable
fun CyclingEstimatingLine(
    phrases: List<String>,
    modifier: Modifier = Modifier,
    intervalMs: Int = 1600
) {
    // 保底：避免空陣列造成 crash
    val safePhrases = if (phrases.isEmpty()) listOf("Estimating…") else phrases

    var index by remember { mutableStateOf(0) }
    LaunchedEffect(safePhrases) {
        while (true) {
            delay(intervalMs.toLong())
            index = (index + 1) % safePhrases.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = {
            fadeIn(tween(160)) togetherWith fadeOut(tween(120))
        },
        modifier = modifier,
        label = "estimating_cycling"
    ) { i ->
        Text(
            text = safePhrases[i],
            color = Black,
            textAlign = TextAlign.Center,
            // 與你既有風格一致（titleLarge + SemiBold）
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        )
    }
}
@Composable
fun IndeterminateRing(
    modifier: Modifier = Modifier,
    diameter: Dp = 128.dp,   // ★ 預設改大
    ringWidth: Dp = 12.dp,   // ★ 預設改粗
    sweepDegrees: Float = 90f,
    durationMillis: Int = 900,
    color: Color = Green,
    trackColor: Color = TrackGray
) {
    val t = rememberInfiniteTransition(label = "ring")
    val startAngle by t.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepDegrees,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}



@Composable
private fun SimpleHeaderBar(
    title: String,
    onClose: () -> Unit,
    topPadding: Dp = 12.dp,       // ★ 原 8.dp → 12.dp：整體往下
    gapAfterHandle: Dp = 20.dp,   // ★ 原 12.dp → 20.dp：把手到標題更遠
    closeSize: Dp = 32.dp,        // ★ 原 32.dp → 40.dp
    closeIconSize: Dp = 24.dp     // ★ 原 24.dp → 28.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 12.dp)
    ) {
        // 上方小把手
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(color = HandleGray.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.height(gapAfterHandle)) // ★ 拉開距離

        // 標題 + 右上關閉
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Black
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(closeSize)                 // ★ 放大按鈕外徑
                    .clip(CircleShape)
                    .background(Black)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "close",
                    tint = Color.White,
                    modifier = Modifier.size(closeIconSize) // ★ 放大 icon
                )
            }
        }
    }
}

@Composable
fun EstimatingContent(
    // ★ 新增：整組（進度環＋主文案）往上抬高的距離
    centerLift: Dp = 110.dp,
    // ★ 新增：底部提示文字往上抬高的距離
    bottomLift: Dp = 20.dp,
    // 其餘保持你的預設視覺
    ringDiameter: Dp = 128.dp,
    ringWidth: Dp = 12.dp,
    ringSweep: Float = 90f,
    ringDurationMillis: Int = 900
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // 中央：進度環 + 主文案（整組往上）
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -centerLift),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IndeterminateRing(
                diameter = ringDiameter,
                ringWidth = ringWidth,
                sweepDegrees = ringSweep,
                durationMillis = ringDurationMillis
            )
            Spacer(Modifier.height(28.dp))

            CyclingEstimatingLine(
                phrases = listOf(
                    "Analyzing your activity…",
                    "Working on your numbers…",
                    "Estimating effort, calculating calories..."
                ),
                intervalMs = 1600, // 1.6 秒切換一次（可依體感調整）
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 底部提示（相對底部再往上）
        Text(
            text = "Please do not close the app or lock your device",
            color = Black.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .offset(y = -bottomLift) // ★ 再上移一點
        )
    }
}

@Composable
fun ResultContent(
    result: EstimateResponse,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 白底
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(Green)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "${result.minutes ?: 0} min ${result.activityDisplay.orEmpty()}",
                color = Black,
                // ✅ 修正拼字：typography
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${result.kcal ?: 0} kcal",
                color = Black,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Black
                )
            ) {
                Text(
                    "Save Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrayBtn,
                    contentColor = Color.White
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun FailedContent(
    onTryAgain: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // ★ 白底
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(Amber),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Uh-oh! Scan Failed",
                color = Black,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The activity description may be incorrect, or the internet connection is weak.",
                color = Black.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Black
                )
            ) {
                Text(
                    "Try Again",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrayBtn,
                    contentColor = Color.White
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }
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
