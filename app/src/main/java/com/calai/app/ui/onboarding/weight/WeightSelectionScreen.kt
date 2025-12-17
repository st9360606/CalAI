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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.calai.app.data.profile.repo.lbsToKg1
import com.calai.app.data.profile.repo.roundKg1
import com.calai.app.ui.common.OnboardingProgress
import kotlin.math.roundToInt
import java.util.Locale
import com.calai.app.i18n.LocalLocaleController

private fun isZhLanguageTag(tag: String): Boolean {
    // tag 可能是 "zh", "zh-TW", "zh-Hant-TW"
    return tag.lowercase(Locale.ROOT).startsWith("zh")
}

@Composable
private fun currentLanguageTag(): String {
    val composeLocale = LocalLocaleController.current
    val tag = composeLocale.tag
    return tag.ifBlank { Locale.getDefault().toLanguageTag() }
}

@Composable
private fun currentIsZhByAppLocale(): Boolean {
    val tag = currentLanguageTag()
    return remember(tag) { isZhLanguageTag(tag) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeightSelectionScreen(
    vm: WeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val weightKg by vm.weightKgState.collectAsState()
    val savedUnit by vm.weightUnitState.collectAsState()
    val weightLbs by vm.weightLbsState.collectAsState()

    // kg 範圍
    val KG_MIN = 20.0
    val KG_MAX = 800.0

    // lbs 範圍（由 kg 範圍換算）
    val LBS_TENTHS_MIN = kgToLbsTenths(KG_MIN)
    val LBS_TENTHS_MAX = kgToLbsTenths(KG_MAX)
    val LBS_INT_MIN = LBS_TENTHS_MIN / 10
    val LBS_INT_MAX = LBS_TENTHS_MAX / 10

    // 初始顯示單位：優先用已儲存的 weightUnit；預設 LBS
    var useMetric by rememberSaveable {
        mutableStateOf(
            when (savedUnit) {
                UserProfileStore.WeightUnit.KG -> true
                UserProfileStore.WeightUnit.LBS -> false
            }
        )
    }

    // === 初始化 kg / lbs（kg 用於計算，lbsTenths 記錄使用者原始 lbs） ===
    data class Initial(val kg: Double, val lbsTenths: Int)

    val initial = remember(weightKg, weightLbs) {
        val hasLbs = weightLbs > 0f
        if (hasLbs) {
            val lbsVal = weightLbs.toDouble()
            val lbsTenths = (lbsVal * 10.0).roundToInt()
                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)

            val kgVal = if (weightKg > 0f) {
                weightKg.toDouble()
            } else {
                lbsToKg1(lbsVal)
            }.coerceIn(KG_MIN, KG_MAX)

            Initial(kgVal, lbsTenths)
        } else {
            val kgValBase = if (weightKg > 0f) weightKg.toDouble() else 65.0
            val kgVal = kgValBase.coerceIn(KG_MIN, KG_MAX)
            val lbsTenths = kgToLbsTenths(kgVal)
                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)

            Initial(kgVal, lbsTenths)
        }
    }

    // valueKg：計算用 kg（0.1 精度）
    var valueKg by remember(weightKg, weightLbs) {
        mutableDoubleStateOf(initial.kg)
    }

    // valueLbsTenths：顯示用 lbs（0.1 精度，用 Int 表示）
    var valueLbsTenths by remember(weightKg, weightLbs) {
        mutableIntStateOf(initial.lbsTenths)
    }

    // --- kg wheel 選中值（整數＋小數） ---
    val kgTenths = (valueKg * 10.0).toInt()
        .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10

    // --- lbs wheel 選中值（整數＋小數），完全由 valueLbsTenths 控制 ---
    val lbsTenthsClamped = valueLbsTenths
        .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
    val lbsIntSel = lbsTenthsClamped / 10
    val lbsDecSel = lbsTenthsClamped % 10

    val isZh = currentIsZhByAppLocale()
    val subtitleToUnitSpacing = if (isZh) 60.dp else 25.dp

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
                            stepIndex = 5,
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
                        // 先存 kg（0.1，無條件捨去）
                        val kgToSave = roundKg1(valueKg)
                            .coerceIn(KG_MIN.toFloat(), KG_MAX.toFloat())
                        vm.saveWeightKg(kgToSave)

                        if (useMetric) {
                            // KG 模式：只存 kg，清掉 lbs
                            vm.saveWeightUnit(UserProfileStore.WeightUnit.KG)
                            vm.clearWeightLbs()
                        } else {
                            // LBS 模式：存 kg + 使用者原始 lbs（0.1 精度）
                            vm.saveWeightUnit(UserProfileStore.WeightUnit.LBS)
                            val lbsToSave =
                                (valueLbsTenths
                                    .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX) / 10.0).toFloat()
                            vm.saveWeightLbs(lbsToSave)
                        }
                        onNext()
                    },
                    enabled = valueKg > 0.0,
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
                text = stringResource(R.string.onboard_weight_title),
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
                text = stringResource(R.string.onboard_weight_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9AA3AF),
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(subtitleToUnitSpacing))

            WeightUnitSegmented(
                useMetric = useMetric,
                onChange = { useMetric = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (useMetric) {
                // ===== KG：整數位 + 小數位 =====
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        range = KG_MIN.toInt()..KG_MAX.toInt(),
                        value = kgIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + kgDecSel)
                                .coerceIn(
                                    (KG_MIN * 10).toInt(),
                                    (KG_MAX * 10).toInt()
                                )
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenths(newKg)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 23.dp)
                    )
                    Box(
                        modifier = Modifier.width(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".",
                            fontSize = 34.sp,
                            modifier = Modifier.offset(x = 2.dp)
                        )
                    }
                    NumberWheel(
                        range = 0..9,
                        value = kgDecSel,
                        onValueChange = { newDec ->
                            val newTenths = (kgIntSel * 10 + newDec)
                                .coerceIn(
                                    (KG_MIN * 10).toInt(),
                                    (KG_MAX * 10).toInt()
                                )
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenths(newKg)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 6.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("kg", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // ===== LBS：整數位 + 小數位，完全以 valueLbsTenths 為主 =====
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        range = LBS_INT_MIN..LBS_INT_MAX,
                        value = lbsIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + lbsDecSel)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            val newLbs = newTenths / 10.0
                            val newKg = lbsToKg1(newLbs)
                            valueKg = newKg.coerceIn(KG_MIN, KG_MAX)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 20.dp)
                    )
                    Box(
                        modifier = Modifier.width(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".",
                            fontSize = 34.sp,
                            modifier = Modifier.offset(x = 4.dp)
                        )
                    }
                    NumberWheel(
                        range = 0..9,
                        value = lbsDecSel,
                        onValueChange = { newDec ->
                            val intPart = lbsIntSel
                            val newTenths = (intPart * 10 + newDec)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            val newLbs = newTenths / 10.0
                            val newKg = lbsToKg1(newLbs)
                            valueKg = newKg.coerceIn(KG_MIN, KG_MAX)
                        },
                        rowHeight = 60.dp,
                        centerTextSize = 32.sp,
                        textSize = 28.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 9.dp)
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
        shape = RoundedCornerShape(40.dp),
        color = Color(0xFFE2E5EA),
        modifier = modifier
            .fillMaxWidth(0.55f)
            .heightIn(min = 40.dp)
    ) {
        Row(Modifier.padding(6.dp)) {
            SegItem(
                text = "lbs",
                selected = !useMetric,
                onClick = { onChange(false) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItem(
                text = "kg",
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

@Composable
private fun SegItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val corner = 22.dp
    val fSize = 18.sp
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

/** 通用數字滾輪（初始化置中 + 抑制首次回呼） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    textSize: TextUnit,
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
            state.scrollToItem(selectedIdx)
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

/* ---------------------------- 換算工具 ---------------------------- */

// 1 kg = 2.2 lbs；以「0.1 lbs」為刻度，無條件捨去
private fun kgToLbsTenths(kg: Double): Int =
    (kg * 2.2 * 10.0).toInt()
