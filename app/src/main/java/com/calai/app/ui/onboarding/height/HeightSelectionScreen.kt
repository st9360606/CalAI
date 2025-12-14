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
import androidx.compose.runtime.mutableIntStateOf
import com.calai.app.data.profile.repo.roundCm1
import com.calai.app.data.profile.repo.cmToFeetInches1
import com.calai.app.data.profile.repo.feetInchesToCm1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HeightSelectionScreen(
    vm: HeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val heightCm by vm.heightCmState.collectAsState()
    val savedUnit by vm.heightUnitState.collectAsState()

    var useMetric by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(savedUnit) { useMetric = (savedUnit == UserProfileStore.HeightUnit.CM) }

    // ★ cm 為 SSOT，Double 一位小數
    val CM_MIN = 80.0
    val CM_MAX = 350.0

    var cmVal by rememberSaveable(heightCm) {
        mutableDoubleStateOf(roundCm1(heightCm.toDouble()).toDouble())
    }

    // ★ ft/in 初始值從 cm 推導
    var feet by rememberSaveable(heightCm) { mutableIntStateOf(cmToFeetInches1(cmVal).first) }
    var inches by rememberSaveable(heightCm) { mutableIntStateOf(cmToFeetInches1(cmVal).second) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
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
                },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingProgress(
                            stepIndex = 4,
                            totalSteps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        // ★ 一律存 cm（1 位小數）
                        vm.saveHeightCm(roundCm1(cmVal))
                        if (useMetric) {
                            vm.saveHeightUnit(UserProfileStore.HeightUnit.CM)
                            vm.clearHeightImperial()
                        } else {
                            vm.saveHeightUnit(UserProfileStore.HeightUnit.FT_IN)
                            vm.saveHeightImperial(feet, inches)
                        }
                        onNext()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.continue_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Text(
                text = stringResource(R.string.onboard_height_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
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
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(81.dp))
            // 切換單位：只更新顯示值；不改 cmVal
            UnitSegmented(
                useMetric = useMetric,
                onChange = { isMetric ->
                    if (!isMetric) {
                        // cm → ft/in
                        val (ft, inch) = cmToFeetInches1(cmVal)
                        feet = ft; inches = inch
                    }
                    useMetric = isMetric
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (useMetric) {
                // ===== CM：整數位 + 小數位 =====
                val cmTenths = (cmVal * 10.0).toInt()
                    .coerceIn((CM_MIN * 10).toInt(), (CM_MAX * 10).toInt())
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
                        range = CM_MIN.toInt()..CM_MAX.toInt(),
                        value = cmIntSel,
                        onValueChange = { newInt ->
                            val newCm = (newInt * 10 + cmDecSel) / 10.0
                            cmVal = newCm.coerceIn(CM_MIN, CM_MAX)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
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
                        range = 0..9,
                        value = cmDecSel,
                        onValueChange = { newDec ->
                            val newCm = (cmIntSel * 10 + newDec) / 10.0
                            cmVal = newCm.coerceIn(CM_MIN, CM_MAX)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        range = 4..9,
                        value = feet,
                        onValueChange = { newFeet ->
                            feet = newFeet
                            cmVal = feetInchesToCm1(newFeet, inches)
                                .coerceIn(CM_MIN, CM_MAX)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "ft",
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 20.dp)
                    )

                    Spacer(Modifier.width(11.dp))

                    NumberWheel(
                        range = 0..11,
                        value = inches,
                        onValueChange = { newIn ->
                            inches = newIn
                            cmVal = feetInchesToCm1(feet, newIn)
                                .coerceIn(CM_MIN, CM_MAX)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "in",
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
                    text = stringResource(R.string.onboard_weight_hint),
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
        onClick = onClick,
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

/** 通用數字滾輪（首次精準置中 + 抑制首次回呼） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    textSize: TextUnit = 26.sp,
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
                    if (unitLabel != null && isCenter) {
                        Spacer(Modifier.width(16.dp))  // 想再靠右一點可以改成 10.dp、12.dp
                    }
                    Text(
                        text = num.toString(),
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
