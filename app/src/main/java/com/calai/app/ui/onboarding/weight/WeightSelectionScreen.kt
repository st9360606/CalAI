package com.calai.app.ui.onboarding.weight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlin.math.round
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeightSelectionScreen(
    vm: WeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    rowHeight: Dp = 56.dp,
) {
    val weightKg by vm.weightKgState.collectAsState()
    val savedUnit by vm.weightUnitState.collectAsState()

    // ✅ 改成預設 LBS（false），一進畫面就顯示 143 lbs（= 65.0 kg）
    var useMetric by rememberSaveable { mutableStateOf(false) }
    // ⛔ 移除：不要再用 savedUnit 覆寫，否則會被切回 KG
    // LaunchedEffect(savedUnit) { useMetric = (savedUnit == UserProfileStore.WeightUnit.KG) }

    // 以 kg 為 SSOT；若資料層給 0，預設 65.0kg（= 143lbs）
    var valueKg by remember(weightKg) {
        mutableStateOf(if (weightKg > 0f) weightKg.toDouble() else 65.0)
    }

    // 可選範圍
    val KG_INT_MIN = 20
    val KG_INT_MAX = 800
    val LBS_INT_MIN = kgToLbsInt(KG_INT_MIN.toDouble())   // 44
    val LBS_INT_MAX = kgToLbsInt(KG_INT_MAX.toDouble())   // 660

    // —— 目前選中項 —— //
    val kgTenths = (valueKg * 10.0).roundToInt().coerceIn(KG_INT_MIN * 10, KG_INT_MAX * 10)
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10
    val lbsIntSel = kgToLbsInt(valueKg).coerceIn(LBS_INT_MIN, LBS_INT_MAX)

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
                        vm.saveWeightKg(roundKg2(valueKg))
                        vm.saveWeightUnit(
                            if (useMetric) UserProfileStore.WeightUnit.KG
                            else UserProfileStore.WeightUnit.LBS
                        )
                        onNext()
                    },
                    enabled = valueKg > 0.0,
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
                stepIndex = 5,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Text(
                text = stringResource(R.string.onboard_weight_title),
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
                text = stringResource(R.string.onboard_weight_subtitle),
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

            WeightUnitSegmented(
                useMetric = useMetric,
                onChange = { useMetric = it },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(12.dp))

            if (useMetric) {
                // KG：整數位 + 小數位（0~9）
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        range = KG_INT_MIN..KG_INT_MAX,
                        value = kgIntSel,
                        onValueChange = { newInt ->
                            val newKg = (newInt * 10 + kgDecSel) / 10.0
                            valueKg = newKg.coerceIn(KG_INT_MIN.toDouble(), KG_INT_MAX.toDouble())
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 42.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(".", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    NumberWheel(
                        range = 0..9,
                        value = kgDecSel,
                        onValueChange = { newDec ->
                            val newKg = (kgIntSel * 10 + newDec) / 10.0
                            valueKg = newKg.coerceIn(KG_INT_MIN.toDouble(), KG_INT_MAX.toDouble())
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 42.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("kg", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // LBS：整數（無小數輪）— 進到畫面會顯示 143（來自 65kg）
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        range = LBS_INT_MIN..LBS_INT_MAX,
                        value = lbsIntSel,
                        onValueChange = { newLbsInt ->
                            val kgPref = lbsIntToKgPreferred(newLbsInt)
                            valueKg = kgPref.coerceIn(KG_INT_MIN.toDouble(), KG_INT_MAX.toDouble())
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 42.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        modifier = Modifier.width(160.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("lbs", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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

/** 分段切換（lbs / kg） */
@Composable
private fun WeightUnitSegmented(
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
                text = "lbs",
                selected = !useMetric,
                onClick = { onChange(false) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItem(
                text = "kg",
                selected = useMetric,
                onClick = { onChange(true) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp)
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
    val fSize = 22.sp
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = if (selected) selectedColor else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
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

/** 通用數字滾輪（初始化置中 + 抑制首次回呼） */
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
            state.scrollToItem(selectedIdx) // contentPadding 會把它放到正中
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

        // 中心框線
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

/* ---------------------------- 換算 & 取整邏輯 ---------------------------- */

// 以整數 lbs 為主：1 kg = 2.2 lbs，lbs 一律四捨五入為整數
fun kgToLbsInt(v: Double): Int = round(v * 2.2).toInt()

// 從整數 lbs 取得顯示用 kg：選擇「能 round 回同一個 lbs」的區間內最小 0.1kg
fun lbsIntToKgPreferred(lbsInt: Int): Double {
    val lowerBoundKg = (lbsInt - 0.5) / 2.2
    return kotlin.math.ceil(lowerBoundKg * 10.0) / 10.0
}

// 存檔：公斤保留 2 位小數
private fun roundKg2(v: Double): Float =
    (kotlin.math.round(v * 100.0) / 100.0).toFloat()
