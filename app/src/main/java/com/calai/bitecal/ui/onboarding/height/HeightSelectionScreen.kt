package com.calai.bitecal.ui.onboarding.height

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.data.profile.repo.cmToFeetInches1
import com.calai.bitecal.data.profile.repo.feetInchesToCm1
import com.calai.bitecal.ui.common.design.BiteCalOnboardingBottomContainer
import com.calai.bitecal.ui.common.design.BiteCalOnboardingPrimaryButton
import com.calai.bitecal.ui.common.design.BiteCalOnboardingTopBar
import com.calai.bitecal.ui.common.haptic.HapticWheelTickEffect
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HeightSelectionScreen(
    vm: HeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val heightCm by vm.heightCmState.collectAsState()
    val savedUnit by vm.heightUnitState.collectAsState()

    // ====== 範圍（cm SSOT）======
    val cmMin = 80.0
    val cmMax = 350.0

    // ft 範圍建議跟 cmMin/cmMax 對齊，避免切換單位被 clamp 造成跳值
    val feetRange = remember(cmMin, cmMax) {
        val ftMin = cmToFeetInches1(cmMin).first
        val ftMax = cmToFeetInches1(cmMax).first
        ftMin..ftMax
    }

    // ====== 儲存中旗標（freeze seed / freeze unit sync）======
    val scope = rememberCoroutineScope()
    var isSaving by rememberSaveable { mutableStateOf(false) }

// ====== 使用者是否真的有操作（用 wheel 的 isScrollInProgress 判斷）======
    var didUserEdit by rememberSaveable { mutableStateOf(false) }
    var didUserToggleUnit by rememberSaveable { mutableStateOf(false) }

// ====== 單位顯示（不綁 flow 當 key；且儲存中不更新；使用者手動切換後不覆蓋）======
    var useMetric by rememberSaveable { mutableStateOf(false) }

    val cmIntWheelState = rememberLazyListState()
    val cmDecWheelState = rememberLazyListState()
    val ftWheelState = rememberLazyListState()
    val inWheelState = rememberLazyListState()

    val isWheelScrolling = if (useMetric) {
        cmIntWheelState.isScrollInProgress || cmDecWheelState.isScrollInProgress
    } else {
        ftWheelState.isScrollInProgress || inWheelState.isScrollInProgress
    }
    LaunchedEffect(savedUnit, isSaving) {
        if (!isSaving && !didUserToggleUnit) {
            useMetric = (savedUnit == UserProfileStore.HeightUnit.CM)
        }
    }

    // ====== 從 flow 計算「應 seed 的初始值」======
    val initialCm = remember(heightCm, cmMin, cmMax) {
        normalizeCm1(heightCm.toDouble()).toDouble().coerceIn(cmMin, cmMax)
    }
    val initialFtIn = remember(initialCm) { cmToFeetInches1(initialCm) }

    // ✅ 本地 wheel 狀態：不要用 rememberSaveable(heightCm) 當 key（會導致 Continue 抖動）
    var cmVal by rememberSaveable { mutableDoubleStateOf(initialCm) }
    var feet by rememberSaveable { mutableIntStateOf(initialFtIn.first) }
    var inches by rememberSaveable { mutableIntStateOf(initialFtIn.second) }

    // ✅ 只有「非儲存中」且「使用者尚未滑動」才讓 flow 回填（避免 Continue 時跳一下）
    LaunchedEffect(initialCm, isSaving) {
        if (!isSaving && !didUserEdit) {
            cmVal = initialCm
            feet = initialFtIn.first
            inches = initialFtIn.second
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            BiteCalOnboardingTopBar(
                stepIndex = 4,
                totalSteps = 12,
                onBack = onBack
            )
        },
        bottomBar = {
            BiteCalOnboardingBottomContainer {
                BiteCalOnboardingPrimaryButton(
                    text = stringResource(R.string.common_continue_btn),
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val cmToSave = (cmToTenths(cmVal) / 10f)
                                    .coerceIn(cmMin.toFloat(), cmMax.toFloat())

                                vm.saveAll(
                                    cm = cmToSave,
                                    useMetric = useMetric,
                                    feet = feet,
                                    inches = inches
                                )

                                onNext()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving && !isWheelScrolling,
                    loading = isSaving
                )
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboard_height_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BiteCalScreenFrame.contentHorizontalMedium),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboard_height_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9AA3AF),
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BiteCalScreenFrame.onboardingSubtitleHorizontal),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(65.dp))

            UnitSegmented(
                useMetric = useMetric,
                onChange = { isMetric ->
                    didUserToggleUnit = true

                    if (!isMetric) {
                        // cm → ft/in（切到英制時，顯示值從 cmVal 推導）
                        val (ft, inch) = cmToFeetInches1(cmVal)
                        feet = ft
                        inches = inch
                    }
                    useMetric = isMetric
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (useMetric) {
                // ===== CM：整數位 + 小數位 =====
                val cmTenths = cmToTenths(cmVal)
                    .coerceIn(cmToTenths(cmMin), cmToTenths(cmMax))
                val cmIntSel = cmTenths / 10
                val cmDecSel = cmTenths % 10

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        listState = cmIntWheelState,
                        range = cmMin.toInt()..cmMax.toInt(),
                        value = cmIntSel,
                        onValueChange = { newInt ->
                            val newCm = (newInt * 10 + cmDecSel) / 10.0
                            cmVal = newCm.coerceIn(cmMin, cmMax)
                        },
                        onUserScroll = { didUserEdit = true },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        userScrollEnabled = !isSaving,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 27.dp)
                    )

                    Box(
                        modifier = Modifier.width(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".",
                            fontSize = 34.sp,
                            modifier = Modifier.offset(x = 8.dp)
                        )
                    }

                    NumberWheel(
                        listState = cmDecWheelState,
                        range = 0..9,
                        value = cmDecSel,
                        onValueChange = { newDec ->
                            val newCm = (cmIntSel * 10 + newDec) / 10.0
                            cmVal = newCm.coerceIn(cmMin, cmMax)
                        },
                        onUserScroll = { didUserEdit = true },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        userScrollEnabled = !isSaving,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 13.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "cm",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // ===== FT/IN =====
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        listState = ftWheelState,
                        range = feetRange,
                        value = feet.coerceIn(feetRange.first, feetRange.last),
                        onValueChange = { newFeet ->
                            feet = newFeet
                            cmVal = feetInchesToCm1(newFeet, inches)
                                .coerceIn(cmMin, cmMax)
                        },
                        onUserScroll = { didUserEdit = true },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "ft",
                        userScrollEnabled = !isSaving,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 20.dp)
                    )

                    Spacer(Modifier.width(11.dp))

                    NumberWheel(
                        listState = inWheelState,
                        range = 0..11,
                        value = inches.coerceIn(0, 11),
                        onValueChange = { newIn ->
                            inches = newIn
                            cmVal = feetInchesToCm1(feet, newIn)
                                .coerceIn(cmMin, cmMax)
                        },
                        onUserScroll = { didUserEdit = true },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "in",
                        userScrollEnabled = !isSaving,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(end = 19.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.onboard_height_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9AA3AE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 分段切換（ft / cm） */
@Composable
private fun UnitSegmented(
    useMetric: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = Color(0xFFE2E5EA),
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .heightIn(min = 40.dp)
        ) {
            Row(Modifier.padding(6.dp)) {
                SegItem(
                    text = "ft",
                    selected = !useMetric,
                    onClick = { onChange(false) },
                    selectedColor = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
                Spacer(Modifier.width(6.dp))
                SegItem(
                    text = "cm",
                    selected = useMetric,
                    onClick = { onChange(true) },
                    selectedColor = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
            }
        }
    }
}

@Composable
private fun SegItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val corner = 22.dp
    val fSize = 20.sp

    Surface(
        onClick = rememberClickWithHaptic(onClick = onClick),
        shape = RoundedCornerShape(corner),
        color = if (selected) selectedColor else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = fSize,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 通用數字滾輪（中心對齊 + 初次也會 emit）
 * - onUserScroll：只有真的開始滑動時才回呼（用來設 didUserEdit=true）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    listState: LazyListState,
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    textSize: TextUnit = 26.sp,
    sideAlpha: Float,
    unitLabel: String? = null,
    userScrollEnabled: Boolean = true,
    onUserScroll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visibleCount = 5
    val mid = visibleCount / 2

    val values: List<Int> = remember(range) { range.toList() }
    val count = values.size

    val padded: List<Int?> = remember(range) {
        List(mid) { null } + values.map { it as Int? } + List(mid) { null }
    }

    val selectedIdx = ((value - range.first).coerceIn(0, count - 1))

    val fling = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    LaunchedEffect(range) {
        listState.scrollToItem(selectedIdx)
    }

    LaunchedEffect(range, value) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != selectedIdx) {
            listState.scrollToItem(selectedIdx)
        }
    }

    val centerListIndex by remember(listState, padded) {
        derivedStateOf {
            (listState.firstVisibleItemIndex + mid).coerceIn(0, padded.lastIndex)
        }
    }

    HapticWheelTickEffect(
        tickKey = centerListIndex,
        enabled = listState.isScrollInProgress
    )

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnUserScroll by rememberUpdatedState(onUserScroll)

    LaunchedEffect(range) {
        if (onUserScroll != null) {
            snapshotFlow { listState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { inProgress ->
                    if (inProgress) latestOnUserScroll?.invoke()
                }
        }
    }

    LaunchedEffect(range) {
        snapshotFlow {
            padded.getOrNull((listState.firstVisibleItemIndex + mid).coerceIn(0, padded.lastIndex))
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { latestOnValueChange(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * visibleCount)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = userScrollEnabled,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(padded) { index, numOrNull ->
                val isCenter = index == centerListIndex
                val alpha = if (isCenter) 1f else sideAlpha
                val size = if (isCenter) centerTextSize else textSize
                val weight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal
                val unitSize = if (isCenter) 20.sp else 18.sp

                Row(
                    modifier = Modifier
                        .height(rowHeight)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (numOrNull == null) {
                        Spacer(Modifier.height(rowHeight))
                        return@Row
                    }

                    if (unitLabel != null && isCenter) Spacer(Modifier.width(16.dp))

                    Text(
                        text = numOrNull.toString(),
                        fontSize = size,
                        fontWeight = weight,
                        color = Color.Black.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )

                    if (unitLabel != null && isCenter) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = unitLabel,
                            fontSize = unitSize,
                            color = Color(0xFF333333).copy(alpha = alpha),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        val lineColor = Color(0x11000000)
        val half = rowHeight / 2
        val lineThickness = 1.dp

        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = -half)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = half - lineThickness)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor)
        )
    }
}

private fun normalizeCm1(value: Double): Float {
    return ((value * 10.0).roundToInt()) / 10f
}

private fun cmToTenths(value: Double): Int {
    return (value * 10.0).roundToInt()
}
