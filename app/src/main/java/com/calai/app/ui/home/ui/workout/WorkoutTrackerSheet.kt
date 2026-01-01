package com.calai.app.ui.home.ui.workout

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.calai.app.R
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter

// 色票
private val Black = Color(0xFF111114)
private val Gray300 = Color(0xFFE5E7EB)
private val Gray600 = Color(0xFF4B5563)
private val DividerGray = Color(0xFFD1D5DB)
private val TextPrimary = Color(0xFF111114)
private val TextSecondary = Color(0xFF4B5563)
private val HandleGray = Color(0xFF9CA3AF)
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
fun WorkoutTrackerSheet(
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
                        ResultContent(
                            result = m.result,
                            onSave = onFlowSave,
                            onCancel = onFlowCancel,
                            activityIconRes = R.drawable.workout_activity,
                            activityIconSize = 130.dp,
                            activityIconTopPadding = 0.dp
                        )
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
    diameter: Dp = 80.dp,   // ★ 預設改大
    ringWidth: Dp = 8.dp,   // ★ 預設改粗
    sweepDegrees: Float = 90f,
    durationMillis: Int = 900,
    color: Color = Color(0xFFFF8F33),
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
/**
 * 中央徽章：黑色圓＋白色勾
 * - 勾的線條粗細以元件直徑的百分比計算（預設 14%）
 * - 勾的路徑採三點路徑，端點/轉角皆為圓角，視覺更「厚實」
 */
/** 中央徽章：黑色圓＋白色勾（勾大小與粗細可調） */
@Composable
private fun CheckBadge(
    badgeSize: Dp,
    bgColor: Color = Black,
    checkColor: Color = Color.White,
    checkStrokePercent: Float = 0.16f,  // 勾線粗細（占直徑比例，建議 0.12f–0.18f）
    checkScale: Float = 0.80f           // 勾整體縮放（1.0=原始；<1 變小）
) {
    val clampedScale = checkScale.coerceIn(0.7f, 1.1f)

    Box(
        modifier = Modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            // 原始三點（較大幅度的打勾）
            val p1 = Pair(w * 0.24f, h * 0.54f)
            val p2 = Pair(w * 0.45f, h * 0.74f)
            val p3 = Pair(w * 0.78f, h * 0.34f)

            fun scaleAroundCenter(px: Float, py: Float): Pair<Float, Float> {
                val sx = cx + (px - cx) * clampedScale
                val sy = cy + (py - cy) * clampedScale
                return sx to sy
            }
            val (x1, y1) = scaleAroundCenter(p1.first, p1.second)
            val (x2, y2) = scaleAroundCenter(p2.first, p2.second)
            val (x3, y3) = scaleAroundCenter(p3.first, p3.second)

            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
                lineTo(x3, y3)
            }

            // 筆畫隨 scale 等比，維持視覺比例；若想「小但很粗」，移除 * clampedScale
            val strokeWidth = minOf(w, h) * checkStrokePercent * clampedScale
            drawPath(
                path = path,
                color = checkColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
/**
 * 變更點：
 * - horizontalMargin：左右邊距（越小越「寬」；0.dp = 滿版）
 * - buttonHeight：按鈕高度（建議 68–72.dp）
 * - bottomLift：整組 CTA 自底部往上「抬」的距離（越大越上移）
 */
/**
 * - 將 horizontalMargin 預設為 0.dp：兩顆按鈕滿版最寬
 * - applySafeEdgePadding：若為 true，會在左右加上「安全邊界」(safeDrawing Horizontal)
 */
@Composable
fun ResultContent(
    result: EstimateResponse,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    centerLift: Dp = 110.dp,
    indicatorSize: Dp = 32.dp,         // 勾徽章大小
    horizontalMargin: Dp = 0.dp,       // CTA 滿版
    buttonHeight: Dp = 60.dp,
    bottomLift: Dp = 40.dp,
    applySafeEdgePadding: Boolean = false,
    kcalTextSize: TextUnit = 40.sp,    // kcal 字級
    @DrawableRes activityIconRes: Int? = null, // 例：R.drawable.workout_activity
    activityIconSize: Dp = 90.dp,      // 圖示大小
    activityIconTopPadding: Dp = 0.dp, // 圖示頂部內距
    activityIconTint: Color? = Color(0xFFFF8F33), // ★ 預設橘色；不要上色→傳 null
    activityIconLift: Dp = 8.dp        // ★ 僅圖示向上位移量
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 中央內容（整塊上移 centerLift）
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -centerLift),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // (1) 活動圖示（可選）
            if (activityIconRes != null) {
                Spacer(Modifier.height(activityIconTopPadding))
                Image(
                    painter = painterResource(id = activityIconRes),
                    contentDescription = result.activityDisplay ?: "Activity",
                    modifier = Modifier
                        .size(activityIconSize)
                        .offset(y = -activityIconLift), // ← 圖示往上移
                    contentScale = ContentScale.Fit,
                    colorFilter = activityIconTint?.let { tint ->
                        ColorFilter.tint(tint, blendMode = BlendMode.SrcIn)
                    }
                )
                Spacer(Modifier.height(20.dp))
            }

            // (2) kcal
            Text(
                text = "${result.kcal ?: 0} kcal",
                color = Black,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = kcalTextSize
                )
            )

            Spacer(Modifier.height(8.dp))

            // (3) minutes + activity（單行省略）
            Text(
                text = "${result.minutes ?: 0} min ${result.activityDisplay.orEmpty()}",
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(24.dp))

            // (4) 黑圓白勾徽章
            CheckBadge(
                badgeSize = indicatorSize,
                bgColor = Black,
                checkColor = Color.White,
                checkStrokePercent = 0.16f,
                checkScale = 0.60f
            )
        }

        // 底部兩顆按鈕（滿版）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = -bottomLift)
                .then(
                    if (applySafeEdgePadding)
                        Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                        )
                    else Modifier
                )
                .padding(horizontal = horizontalMargin, vertical = 16.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black,
                    contentColor = Color.White
                )
            ) {
                Text("Add Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gray300,
                    contentColor = Black
                )
            ) {
                Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
            .background(Color.White)
    ) {
        // 中央警示圖示（放大）
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-110).dp), // 與 ResultContent 一致
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 頂部留白可選擇調整
            Spacer(Modifier.height(0.dp))

            Box(
                modifier = Modifier
                    .size(112.dp) // ✅ 比原本 90.dp 更大
                    .offset(y = (-12).dp) // ✅ 稍微上移一點，保持視覺平衡
                    .clip(CircleShape)
                    .background(Amber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp) // ✅ 原本 56 → 放大為 64
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Uh-oh! Scan Failed",
                color = Black,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "The workout description may be incorrect ( For example : 30 min Running ),  or the internet connection is weak.",
                color = Black.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // ✅ 底部按鈕與 ResultContent 對齊
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = 0.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Try Again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gray300,
                    contentColor = Black
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
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
                .background(Color(0xFFB5B5B5)),
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
