package com.calai.app.ui.onboarding.height

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.ui.common.OnboardingProgress

// ← 調整這個數值即可讓「CM 單一滾輪」往右/往左偏移
private val CM_WHEEL_X_SHIFT = 24.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HeightSelectionScreen(
    vm: HeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    rowHeight: Dp = 56.dp,
) {
    val heightCm by vm.heightCmState.collectAsState()
    val savedUnit by vm.heightUnitState.collectAsState()

    // 單位：從存檔載入；預設英制（FT/IN）
    var useMetric by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(savedUnit) { useMetric = (savedUnit == UserProfileStore.HeightUnit.CM) }

    // cm 為 SSOT；切換單位只改顯示，不改 cm
    var cmVal by rememberSaveable(heightCm) { mutableIntStateOf(heightCm) }
    var feet by rememberSaveable(heightCm) { mutableIntStateOf(cmToFeetInches(cmVal).first) }
    var inches by rememberSaveable(heightCm) { mutableIntStateOf(cmToFeetInches(cmVal).second) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(39.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F3F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111114)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        vm.saveHeightCm(cmVal) // 一律存 cm
                        if (useMetric) {
                            vm.saveHeightUnit(UserProfileStore.HeightUnit.CM)
                            vm.clearHeightImperial() // 不存英制
                        } else {
                            vm.saveHeightUnit(UserProfileStore.HeightUnit.FT_IN)
                            vm.saveHeightImperial(feet, inches) // 存 ft/in
                        }
                        onNext()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 75.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            OnboardingProgress(
                stepIndex = 4,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Text(
                text = stringResource(R.string.onboard_height_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.onboard_height_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = Color(0xFFB6BDC6),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // 切換單位：只更新顯示值；不改 cmVal
            UnitSegmented(
                useMetric = useMetric,
                onChange = { isMetric ->
                    if (!isMetric) {
                        val (ft, inch) = cmToFeetInches(cmVal) // 165cm -> 5ft4in（floor）
                        feet = ft; inches = inch
                    }
                    useMetric = isMetric
                },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )

            if (useMetric) {
                // ✅ CM 單一滾輪：整組往右偏一點
                NumberWheel(
                    range = 80..350,
                    value = cmVal,
                    onValueChange = { cmVal = it },
                    rowHeight = rowHeight,
                    centerTextSize = 42.sp,
                    sideAlpha = 0.35f,
                    unitLabel = "cm",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = CM_WHEEL_X_SHIFT) // ← 這行就是右移關鍵
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    NumberWheel(
                        range = 4..7,
                        value = feet,
                        onValueChange = { newFeet ->
                            feet = newFeet
                            cmVal = feetInchesToCm(feet = newFeet, inches = inches)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 42.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "ft",
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(11.dp))
                    NumberWheel(
                        range = 0..11,
                        value = inches,
                        onValueChange = { newIn ->
                            inches = newIn
                            cmVal = feetInchesToCm(feet = feet, inches = newIn)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 42.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "in",
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
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
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF1F3F7),
        modifier = modifier
            .fillMaxWidth(0.58f)
            .heightIn(min = 45.dp)
    ) {
        Row(Modifier.padding(6.dp)) {
            SegItem(
                text = "ft",
                selected = !useMetric,
                onClick = { onChange(false) },
                selectedColor = Color.Black,
                modifier = Modifier.weight(1f).height(45.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItem(
                text = "cm",
                selected = useMetric,
                onClick = { onChange(true) },
                selectedColor = Color.Black,
                modifier = Modifier.weight(1f).height(45.dp)
            )
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
    val minH = 48.dp
    val hPad = 24.dp
    val vPad = 10.dp
    val fSize = 23.sp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = if (selected) selectedColor else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = minH)
                .fillMaxWidth()
                .padding(horizontal = hPad, vertical = vPad),
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

/** 通用數字滾輪（首次精準置中 + 抑制首次回呼） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    sideAlpha: Float,
    unitLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val VISIBLE_COUNT = 5
    val MID = VISIBLE_COUNT / 2
    val items = remember(range) { range.toList() }
    val selectedIdx = (value - range.first).coerceIn(0, items.lastIndex)

    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    var initialized by remember(range) { mutableStateOf(false) }
    LaunchedEffect(range, value) {
        if (!initialized) {
            state.scrollToItem(selectedIdx) // contentPadding 讓它位於中心
            initialized = true
        }
    }

    val centerIndex by remember {
        derivedStateOf {
            val li = state.layoutInfo
            if (li.visibleItemsInfo.isEmpty()) return@derivedStateOf selectedIdx
            val viewportCenter = (li.viewportStartOffset + li.viewportEndOffset) / 2
            li.visibleItemsInfo.minByOrNull { info ->
                kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
            }?.index ?: selectedIdx
        }
    }
    LaunchedEffect(centerIndex, initialized) {
        if (initialized) onValueChange(items[centerIndex])
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * VISIBLE_COUNT)
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = rowHeight * MID),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, num ->
                val isCenter = index == centerIndex
                val alpha = if (isCenter) 1f else sideAlpha
                val size = if (isCenter) centerTextSize else 28.sp
                val weight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal
                val unitSize = if (isCenter) 20.sp else 18.sp

                Row(
                    modifier = Modifier
                        .height(rowHeight)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = num.toString(),
                        fontSize = size,
                        fontWeight = weight,
                        color = Color.Black.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
                    if (unitLabel != null) {
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

        // 中心框線：中心 ± 半格
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
