package com.calai.bitecal.ui.onboarding.goalweight

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
import com.calai.bitecal.data.profile.repo.roundKg1
import com.calai.bitecal.i18n.LocalLocaleController
import com.calai.bitecal.ui.common.design.BiteCalOnboardingBottomContainer
import com.calai.bitecal.ui.common.design.BiteCalOnboardingPrimaryButton
import com.calai.bitecal.ui.common.design.BiteCalOnboardingTopBar
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.haptic.HapticWheelTickEffect
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private fun isZhLanguageTag(tag: String): Boolean {
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
    val hasProfile by vm.hasProfileState.collectAsState()

    val kgMin = 20.0
    val kgMax = 800.0

    val lbsTenthsMin = kgToLbsTenths(kgMin)
    val lbsTenthsMax = kgToLbsTenths(kgMax)
    val lbsIntMin = lbsTenthsMin / 10
    val lbsIntMax = lbsTenthsMax / 10

    val defaultLbsTenths = 1540
    val defaultKg = lbsTenthsToKgFloor1(defaultLbsTenths).coerceIn(kgMin, kgMax)

    var didUserToggleUnit by rememberSaveable { mutableStateOf(false) }
    var useMetric by rememberSaveable { mutableStateOf(false) }

    val hasAnyWeight = (weightKg > 0f) || (weightLbs > 0f)

    LaunchedEffect(hasProfile, savedUnit, hasAnyWeight) {
        if (!didUserToggleUnit) {
            useMetric = when {
                !hasAnyWeight -> false
                !hasProfile -> false
                else -> savedUnit == UserProfileStore.WeightUnit.KG
            }
        }
    }

    data class Initial(val kg: Double, val lbsTenths: Int)

    val initial = remember(weightKg, weightLbs) {
        val hasLbs = weightLbs > 0f
        val hasKg = weightKg > 0f

        if (hasLbs) {
            val lbsVal = weightLbs.toDouble()
            val lbsTenths = (lbsVal * 10.0).roundToInt()
                .coerceIn(lbsTenthsMin, lbsTenthsMax)

            val kgVal = if (hasKg) {
                normalizeKg1(weightKg.toDouble())
            } else {
                lbsToKgPrecise(lbsVal)
            }.coerceIn(kgMin, kgMax)

            Initial(kgVal, lbsTenths)
        } else if (hasKg) {
            val kgVal = normalizeKg1(weightKg.toDouble())
                .coerceIn(kgMin, kgMax)
            val lbsTenths = kgToLbsTenths(kgVal)
                .coerceIn(lbsTenthsMin, lbsTenthsMax)

            Initial(kgVal, lbsTenths)
        } else {
            Initial(defaultKg, defaultLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax))
        }
    }

    var isSaving by rememberSaveable { mutableStateOf(false) }
    var valueKg by rememberSaveable { mutableDoubleStateOf(initial.kg) }
    var valueLbsTenths by rememberSaveable { mutableIntStateOf(initial.lbsTenths) }

    LaunchedEffect(initial.kg, initial.lbsTenths, isSaving) {
        if (!isSaving) {
            valueKg = initial.kg
            valueLbsTenths = initial.lbsTenths
        }
    }

    val kgTenths = (normalizeKg1(valueKg) * 10.0).roundToInt()
        .coerceIn((kgMin * 10).toInt(), (kgMax * 10).toInt())
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10

    val lbsTenthsClamped = valueLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax)
    val lbsIntSel = lbsTenthsClamped / 10
    val lbsDecSel = lbsTenthsClamped % 10

    val isZh = currentIsZhByAppLocale()
    val subtitleToUnitSpacing = if (isZh) 60.dp else 25.dp

    val scope = rememberCoroutineScope()

    val kgIntWheelState = rememberLazyListState()
    val kgDecWheelState = rememberLazyListState()
    val lbsIntWheelState = rememberLazyListState()
    val lbsDecWheelState = rememberLazyListState()

    val isWheelScrolling = if (useMetric) {
        kgIntWheelState.isScrollInProgress || kgDecWheelState.isScrollInProgress
    } else {
        lbsIntWheelState.isScrollInProgress || lbsDecWheelState.isScrollInProgress
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            BiteCalOnboardingTopBar(
                stepIndex = 8,
                totalSteps = 12,
                onBack = onBack
            )
        },
        bottomBar = {
            BiteCalOnboardingBottomContainer {
                BiteCalOnboardingPrimaryButton(
                    text = stringResource(R.string.continue_text),
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val kgToSave: Float
                                val lbsToSaveOrNull: Float?
                                val lbsTenthsForUi: Int

                                if (useMetric) {
                                    val intNow = centeredWheelValue(
                                        state = kgIntWheelState,
                                        range = kgMin.toInt()..kgMax.toInt()
                                    )
                                    val decNow = centeredWheelValue(
                                        state = kgDecWheelState,
                                        range = 0..9
                                    )

                                    val kgTenthsNow = (intNow * 10 + decNow)
                                        .coerceIn((kgMin * 10).toInt(), (kgMax * 10).toInt())

                                    kgToSave = (kgTenthsNow / 10f)
                                        .coerceIn(kgMin.toFloat(), kgMax.toFloat())

                                    lbsToSaveOrNull = null
                                    lbsTenthsForUi = kgToLbsTenths(kgToSave.toDouble())
                                        .coerceIn(lbsTenthsMin, lbsTenthsMax)
                                } else {
                                    val intNow = centeredWheelValue(
                                        state = lbsIntWheelState,
                                        range = lbsIntMin..lbsIntMax
                                    )
                                    val decNow = centeredWheelValue(
                                        state = lbsDecWheelState,
                                        range = 0..9
                                    )

                                    val lbsTenthsNow = (intNow * 10 + decNow)
                                        .coerceIn(lbsTenthsMin, lbsTenthsMax)

                                    val lbsNow = lbsTenthsNow / 10f
                                    lbsToSaveOrNull = lbsNow

                                    kgToSave = roundKg1(lbsToKgPrecise(lbsNow.toDouble()))
                                        .coerceIn(kgMin.toFloat(), kgMax.toFloat())

                                    lbsTenthsForUi = lbsTenthsNow
                                }

                                valueKg = kgToSave.toDouble()
                                valueLbsTenths = lbsTenthsForUi

                                vm.saveAll(
                                    kgToSave = kgToSave,
                                    useMetric = useMetric,
                                    lbsToSaveOrNull = lbsToSaveOrNull
                                )

                                onNext()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = valueKg > 0.0 && !isSaving && !isWheelScrolling,
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
                text = stringResource(R.string.onboard_goal_weight_title),
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
                text = stringResource(R.string.onboard_weight_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9AA3AF),
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BiteCalScreenFrame.onboardingSubtitleHorizontal),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(subtitleToUnitSpacing))

            WeightUnitSegmented(
                useMetric = useMetric,
                onChange = { newUseMetric ->
                    didUserToggleUnit = true

                    if (newUseMetric) {
                        val tenths = valueLbsTenths.coerceIn(lbsTenthsMin, lbsTenthsMax)
                        valueKg = lbsTenthsToKgFloor1(tenths).coerceIn(kgMin, kgMax)
                    } else {
                        valueLbsTenths = kgToLbsTenths(normalizeKg1(valueKg))
                            .coerceIn(lbsTenthsMin, lbsTenthsMax)
                    }
                    useMetric = newUseMetric
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (useMetric) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        listState = kgIntWheelState,
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
                        Modifier.width(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".",
                            fontSize = 34.sp,
                            modifier = Modifier.offset(x = 2.dp)
                        )
                    }

                    NumberWheel(
                        listState = kgDecWheelState,
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheel(
                        listState = lbsIntWheelState,
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
                        listState = lbsDecWheelState,
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

private fun centeredWheelValue(
    state: LazyListState,
    range: IntRange
): Int {
    return (range.first + state.firstVisibleItemIndex)
        .coerceIn(range.first, range.last)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    listState: LazyListState,
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

    val padded: List<Int?> = remember(range) {
        List(mid) { null } + values.map { it as Int? } + List(mid) { null }
    }

    val selectedIdx = (value - range.first).coerceIn(0, count - 1)

    val fling = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

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

    var ignoreNextCenterCallback by remember { mutableStateOf(true) }

    LaunchedEffect(range) {
        ignoreNextCenterCallback = true
        listState.scrollToItem(selectedIdx)
    }

    LaunchedEffect(range, value) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != selectedIdx) {
            ignoreNextCenterCallback = true
            listState.scrollToItem(selectedIdx)
        }
    }

    LaunchedEffect(range) {
        snapshotFlow {
            padded.getOrNull(
                (listState.firstVisibleItemIndex + mid).coerceIn(0, padded.lastIndex)
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { centerValue ->
                if (ignoreNextCenterCallback) {
                    ignoreNextCenterCallback = false
                    return@collect
                }
                latestOnValueChange(centerValue)
            }
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

private fun normalizeKg1(v: Double): Double =
    (v * 10.0).roundToInt() / 10.0

private const val KG_PER_LB = 0.45359237
private const val LBS_PER_KG = 1.0 / KG_PER_LB
private const val EPS = 1e-9

private fun floor1(v: Double): Double =
    kotlin.math.floor((v + EPS) * 10.0) / 10.0

private fun lbsToKgPrecise(lbs: Double): Double =
    lbs * KG_PER_LB

private fun lbsTenthsToKgFloor1(lbsTenths: Int): Double =
    floor1((lbsTenths / 10.0) * KG_PER_LB)

private fun kgToLbsTenths(kg: Double): Int =
    ((kg * LBS_PER_KG + EPS) * 10.0).toInt()
