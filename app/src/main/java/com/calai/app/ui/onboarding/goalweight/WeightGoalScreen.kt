package com.calai.app.ui.onboarding.goalweight

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
import com.calai.app.data.profile.repo.roundKg1
import com.calai.app.ui.common.OnboardingProgress
import kotlin.math.roundToInt
import java.util.Locale
import com.calai.app.i18n.LocalLocaleController
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

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
fun WeightGoalScreen(
    vm: WeightGoalViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val weightKg by vm.weightKgState.collectAsState()
    val savedUnit by vm.weightUnitState.collectAsState()
    val weightLbs by vm.weightLbsState.collectAsState()

    // ✅ 是否有 user_profiles（由 VM / Store 提供）
    // - true  => user_profiles 存在，使用 DB unit_preference
    // - false => user_profiles 不存在，一律顯示 LBS
    val hasProfile by vm.hasProfileState.collectAsState()

    // kg 範圍
    val kgMin = 20.0
    val kgMax = 800.0

    // lbs 範圍（由 kg 範圍換算）
    val lbsTenthsMin = kgToLbsTenths(kgMin)
    val lbsTenthsMax = kgToLbsTenths(kgMax)
    val lbsIntMin = lbsTenthsMin / 10
    val lbsIntMax = lbsTenthsMax / 10

    // ✅ 預設：LBS + 154.0（只在「user_profiles 不存在」時使用）
    val defaultLbsTenths = 1540     // 154.0 lbs
    val defaultKg = lbsTenthsToKgFloor1(defaultLbsTenths).coerceIn(kgMin, kgMax)

    // ✅ 防止使用者手動切換後，被 flow 更新覆蓋
    var didUserToggleUnit by rememberSaveable { mutableStateOf(false) }

    // ✅ UI 顯示單位（false=LBS, true=KG）
    // 先預設 LBS；若 hasProfile=true 且尚未手動切換，會被 LaunchedEffect 套用 DB unit_preference
    var useMetric by rememberSaveable { mutableStateOf(false) }

    // ✅ 初始顯示單位規則：
    // - user_profiles 不存在 => 一律 LBS
    // - user_profiles 存在   => 用 DB unit_preference (savedUnit)
    LaunchedEffect(hasProfile, savedUnit) {
        if (!didUserToggleUnit) {
            useMetric = if (!hasProfile) {
                false
            } else {
                savedUnit == UserProfileStore.WeightUnit.KG
            }
        }
    }

    // === 初始化 kg / lbs（kg 計算、lbsTenths 記錄原始 lbs） ===
    data class GoalInitial(val kg: Double, val lbsTenths: Int)

    val initial = remember(weightKg, weightLbs, hasProfile) {
        val hasLbs = weightLbs > 0f
        val hasKg = weightKg > 0f

        if (hasLbs) {
            // 有 lbs → 以 lbs 為主
            val lbsVal = weightLbs.toDouble()
            val lbsTenths = (lbsVal * 10.0).roundToInt()
                .coerceIn(lbsTenthsMin, lbsTenthsMax)

            val kgVal = if (hasKg) {
                weightKg.toDouble()
            } else {
                lbsToKgPrecise(lbsVal)
            }.coerceIn(kgMin, kgMax)

            GoalInitial(kgVal, lbsTenths)
        } else if (hasKg) {
            // 只有 kg → 由 kg 推 lbsTenths
            val kgVal = weightKg.toDouble().coerceIn(kgMin, kgMax)
            val lbsTenths = kgToLbsTenths(kgVal)
                .coerceIn(lbsTenthsMin, lbsTenthsMax)

            GoalInitial(kgVal, lbsTenths)
        } else {
            // ✅ 完全沒資料：
            // - 若 user_profiles 不存在 => 預設 154.0 lbs
            // - 若 user_profiles 存在但還沒拉到 weight（理論上不該發生，但保底也給 154.0 lbs）
            GoalInitial(defaultKg, defaultLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax))
        }
    }

    // ✅ 提交中 freeze：避免按 Continue 時 Flow 回來導致 wheel 重設 + scrollToItem 跳一下
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    // ✅ wheel 的 SSOT：用 saveable 本地狀態，不要用 remember(flowKey) 重新初始化
    var valueKg by rememberSaveable { mutableDoubleStateOf(0.0) }
    var valueLbsTenths by rememberSaveable { mutableIntStateOf(0) }

    // ✅ 只在「非提交中」才從 Flow seed（Flow 初次載入/切帳號/回到頁面時需要）
    LaunchedEffect(initial.kg, initial.lbsTenths, isSubmitting) {
        if (!isSubmitting) {
            valueKg = initial.kg
            valueLbsTenths = initial.lbsTenths
        }
    }

    // kg wheel 選中值
    val kgTenths = (floor1(valueKg) * 10.0).toInt()
        .coerceIn((kgMin * 10).toInt(), (kgMax * 10).toInt())
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10

    // lbs wheel 選中值
    val lbsTenthsClamped = valueLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax)
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
                            stepIndex = 8,
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
                        if (isSubmitting) return@Button
                        isSubmitting = true

                        val kgToSave = roundKg1(valueKg)
                            .coerceIn(kgMin.toFloat(), kgMax.toFloat())
                        vm.saveWeightKg(kgToSave)

                        if (useMetric) {
                            vm.saveWeightUnit(UserProfileStore.WeightUnit.KG)
                            vm.clearGoalWeightLbs()
                        } else {
                            vm.saveWeightUnit(UserProfileStore.WeightUnit.LBS)
                            val lbsToSave = (valueLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax) / 10.0).toFloat()
                            vm.saveGoalWeightLbs(lbsToSave)
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
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboard_goal_weight_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth(0.9f),   // ✅ 給一點左右留白（你也可改 0.85f / 0.8f）
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
                    .fillMaxWidth(0.82f),  // ✅ 副標通常更窄比較好看
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(subtitleToUnitSpacing))

            WeightUnitSegmented(
                useMetric = useMetric,
                onChange = { newUseMetric ->
                    didUserToggleUnit = true // ✅ 手動切換後，不再自動套用 DB

                    if (newUseMetric) {
                        // ✅ LBS → KG：無條件捨去到 0.1
                        val tenths = valueLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax)
                        valueKg = lbsTenthsToKgFloor1(tenths).coerceIn(kgMin, kgMax)
                        // 例：154.0 lbs -> 69.8 kg
                    } else {
                        // ✅ KG → LBS：無條件捨去到 0.1
                        valueLbsTenths = kgToLbsTenths(floor1(valueKg))
                            .coerceIn(lbsTenthsMin, lbsTenthsMax)
                        // 例：70.0 kg -> 154.3 lbs
                    }
                    useMetric = newUseMetric
                },
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
                        range = kgMin.toInt()..kgMax.toInt(),
                        value = kgIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + kgDecSel)
                                .coerceIn((kgMin * 10).toInt(), (kgMax * 10).toInt())
                            valueKg = newTenths / 10.0
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
                                .coerceIn((kgMin * 10).toInt(), (kgMax * 10).toInt())
                            valueKg = newTenths / 10.0
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
                        range = lbsIntMin..lbsIntMax,
                        value = lbsIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + lbsDecSel)
                                .coerceIn(lbsTenthsMin, lbsTenthsMax)
                            valueLbsTenths = newTenths

                            val newLbs = newTenths / 10.0
                            valueKg = lbsToKgPrecise(newLbs).coerceIn(kgMin, kgMax)
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
                            val newTenths = (lbsIntSel * 10 + newDec)
                                .coerceIn(lbsTenthsMin, lbsTenthsMax)
                            valueLbsTenths = newTenths

                            val newLbs = newTenths / 10.0
                            valueKg = lbsToKgPrecise(newLbs).coerceIn(kgMin, kgMax)
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
    modifier: Modifier = Modifier,
    unitLabel: String? = null
) {
    val visibleCount = 5
    val mid = visibleCount / 2

    val values: List<Int> = remember(range) { range.toList() }
    val count = values.size

    // 上下補 mid 個空白 item，讓中心值 = firstVisible + mid，穩定不偏移
    val padded: List<Int?> = remember(range) {
        List(mid) { null } + values.map { it as Int? } + List(mid) { null }
    }

    val selectedIdx = (value - range.first).coerceIn(0, count - 1)

    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(
        lazyListState = state,
        snapPosition = SnapPosition.Center
    )

    // 初次進入：把 selectedIdx 放到 firstVisible，中心就會落在 selected
    LaunchedEffect(range) {
        state.scrollToItem(selectedIdx)
    }

    // 外部 value 被程式更新時同步位置（避免跟使用者滑動打架）
    LaunchedEffect(range, value) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != selectedIdx) {
            state.scrollToItem(selectedIdx)
        }
    }

    val centerListIndex by remember {
        derivedStateOf {
            (state.firstVisibleItemIndex + mid).coerceIn(0, padded.lastIndex)
        }
    }

    val latestOnValueChange by rememberUpdatedState(onValueChange)

    // ✅ 核心：初次 layout 完成也會 emit，確保「不滑也同步到外層 state」
    LaunchedEffect(range) {
        snapshotFlow {
            padded.getOrNull((state.firstVisibleItemIndex + mid).coerceIn(0, padded.lastIndex))
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
            state = state,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
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

                    Text(
                        text = numOrNull.toString(),
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

/* ---------------------------- 換算工具（精準 + 無條件捨去到 0.1） ---------------------------- */
private const val KG_PER_LB = 0.45359237
private const val LBS_PER_KG = 1.0 / KG_PER_LB
private const val EPS = 1e-9

// 無條件捨去到 0.1（避免浮點誤差導致 69.799999 -> 69.7）
private fun floor1(v: Double): Double =
    kotlin.math.floor((v + EPS) * 10.0) / 10.0

private fun lbsToKgPrecise(lbs: Double): Double =
    lbs * KG_PER_LB

// 154.0 lbs -> 69.8 kg（floor 0.1）
private fun lbsTenthsToKgFloor1(lbsTenths: Int): Double =
    floor1((lbsTenths / 10.0) * KG_PER_LB)

// 70.0 kg -> 154.3 lbs（floor 0.1） => 回傳 1543
private fun kgToLbsTenths(kg: Double): Int =
    ((kg * LBS_PER_KG + EPS) * 10.0).toInt()
