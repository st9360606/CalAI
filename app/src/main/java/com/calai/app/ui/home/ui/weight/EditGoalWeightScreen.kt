package com.calai.app.ui.home.ui.weight

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.kgToLbs1
import com.calai.app.data.profile.repo.lbsToKg1
import com.calai.app.ui.home.ui.weight.components.WeightTopBar
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditGoalWeightScreen(
    vm: WeightViewModel,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val KG_MIN = 20.0
    val KG_MAX = 800.0
    val LBS_TENTHS_MIN = kgToLbsTenthsForGoal(KG_MIN)
    val LBS_TENTHS_MAX = kgToLbsTenthsForGoal(KG_MAX)
    val LBS_INT_MIN = LBS_TENTHS_MIN / 10
    val LBS_INT_MAX = LBS_TENTHS_MAX / 10

    val profileUnit = ui.unit

    // ✅ 初始值只初始化一次（避免 ui refresh 時輪盤跳回）
    var initialized by rememberSaveable { mutableStateOf(false) }
    var useMetric by rememberSaveable { mutableStateOf(profileUnit == UserProfileStore.WeightUnit.KG) }
    var valueKg by rememberSaveable { mutableStateOf(70.0) }
    var valueLbsTenths by rememberSaveable { mutableStateOf(kgToLbsTenthsForGoal(70.0)) }

    LaunchedEffect(
        profileUnit,
        ui.goal, ui.profileGoalWeightKg, ui.current, ui.profileWeightKg,
        ui.goalLbs, ui.profileGoalWeightLbs, ui.currentLbs, ui.profileWeightLbs
    ) {
        if (initialized) return@LaunchedEffect
        initialized = true

        useMetric = (profileUnit == UserProfileStore.WeightUnit.KG)

        val kgCandidate =
            ui.goal
                ?: ui.profileGoalWeightKg
                ?: ui.current
                ?: ui.profileWeightKg

        val lbsCandidate =
            ui.goalLbs
                ?: ui.profileGoalWeightLbs
                ?: ui.currentLbs
                ?: ui.profileWeightLbs

        val fromKg = kgCandidate
        val fromLbs = lbsCandidate?.let { lbsToKg1(it) }

        val baseKg = (fromKg ?: fromLbs ?: 70.0).coerceIn(KG_MIN, KG_MAX)
        valueKg = baseKg
        valueLbsTenths = kgToLbsTenthsForGoal(baseKg).coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
    }

    val kgTenths = (valueKg * 10.0).toInt()
        .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10

    val lbsTenthsClamped = valueLbsTenths.coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
    val lbsIntSel = lbsTenthsClamped / 10
    val lbsDecSel = lbsTenthsClamped % 10

    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Edit Goal Weight",
                onBack = onCancel
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true

                            val (valueToSave, unitToSave) =
                                if (useMetric) {
                                    val kgClamped = valueKg.coerceIn(KG_MIN, KG_MAX)
                                    val kgRounded = roundToOneDecimalForGoal(kgClamped)
                                    kgRounded to UserProfileStore.WeightUnit.KG
                                } else {
                                    val rawLbs = (valueLbsTenths.coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)) / 10.0
                                    val lbsRounded = roundToOneDecimalForGoal(rawLbs)
                                    lbsRounded to UserProfileStore.WeightUnit.LBS
                                }

                            vm.updateGoalWeight(value = valueToSave, unit = unitToSave) { result ->
                                result.onSuccess {
                                    onSaved()
                                }.onFailure { e ->
                                    isSaving = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = e.message ?: "Failed to update goal weight"
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            WeightUnitSegmentedForGoal(
                useMetric = useMetric,
                onChange = { useMetric = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            if (useMetric) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheelForGoal(
                        range = KG_MIN.toInt()..KG_MAX.toInt(),
                        value = kgIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + kgDecSel)
                                .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenthsForGoal(newKg)
                        },
                        rowHeight = 56.dp,
                        centerTextSize = 28.sp,
                        textSize = 24.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 35.dp)
                    )
                    Text(".", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    NumberWheelForGoal(
                        range = 0..9,
                        value = kgDecSel,
                        onValueChange = { newDec ->
                            val newTenths = (kgIntSel * 10 + newDec)
                                .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenthsForGoal(newKg)
                        },
                        rowHeight = 56.dp,
                        centerTextSize = 28.sp,
                        textSize = 24.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(end = 7.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("kg", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheelForGoal(
                        range = LBS_INT_MIN..LBS_INT_MAX,
                        value = lbsIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + lbsDecSel)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            valueKg = lbsToKg1(newTenths / 10.0).coerceIn(KG_MIN, KG_MAX)
                        },
                        rowHeight = 56.dp,
                        centerTextSize = 28.sp,
                        textSize = 24.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 35.dp)
                    )
                    Text(".", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    NumberWheelForGoal(
                        range = 0..9,
                        value = lbsDecSel,
                        onValueChange = { newDec ->
                            val newTenths = (lbsIntSel * 10 + newDec)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            valueKg = lbsToKg1(newTenths / 10.0).coerceIn(KG_MIN, KG_MAX)
                        },
                        rowHeight = 56.dp,
                        centerTextSize = 28.sp,
                        textSize = 24.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(end = 7.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("lbs", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(
                        text = "Set your goal weight",
                        fontSize = 12.sp,
                        color = Color(0xFF9AA3AE),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "We will use this goal to calculate your progress.",
                        fontSize = 12.sp,
                        color = Color(0xFF9AA3AE),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/* ---------------------------- Segmented ---------------------------- */

@Composable
private fun WeightUnitSegmentedForGoal(
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
            SegItemForGoal(
                text = "lbs",
                selected = !useMetric,
                onClick = { onChange(false) },
                selectedColor = Color.Black,
                modifier = Modifier.weight(1f).height(40.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItemForGoal(
                text = "kg",
                selected = useMetric,
                onClick = { onChange(true) },
                selectedColor = Color.Black,
                modifier = Modifier.weight(1f).height(40.dp)
            )
        }
    }
}

@Composable
private fun SegItemForGoal(
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

/* ---------------------------- NumberWheel ---------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheelForGoal(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    textSize: TextUnit,
    sideAlpha: Float,
    modifier: Modifier = Modifier,
    label: (Int) -> String = { it.toString() }
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
                abs((info.offset + info.size / 2) - viewportCenter)
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

                Row(
                    modifier = Modifier.height(rowHeight).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label(num),
                        fontSize = size,
                        fontWeight = weight,
                        color = Color.Black.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
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

/* ---------------------------- utils ---------------------------- */

private fun kgToLbsTenthsForGoal(kg: Double): Int =
    (kgToLbs1(kg) * 10.0).toInt()

private fun roundToOneDecimalForGoal(value: Double): Double =
    BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toDouble()
