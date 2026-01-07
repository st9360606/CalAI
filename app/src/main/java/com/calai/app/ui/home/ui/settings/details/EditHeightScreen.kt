package com.calai.app.ui.home.ui.settings.details

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.calai.app.data.profile.repo.cmToFeetInches1
import com.calai.app.data.profile.repo.feetInchesToCm1
import com.calai.app.ui.home.ui.settings.details.model.EditHeightViewModel
import com.calai.app.ui.home.ui.weight.components.WeightTopBar
import kotlin.math.abs
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditHeightScreen(
    vm: EditHeightViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val init by vm.initialHeight.collectAsState()

    LaunchedEffect(Unit) { vm.initIfNeeded() }

    // SSOT: cmVal（Double 一位小數）
    val CM_MIN = 80.0
    val CM_MAX = 350.0

    // ✅ 不要用 rememberSaveable：避免回來時還原舊 seed
    var seeded by remember { mutableStateOf(false) }

    var useMetric by remember { mutableStateOf(true) }
    var cmVal by remember { mutableStateOf(170.0) }
    var feet by remember { mutableIntStateOf(5) }
    var inches by remember { mutableIntStateOf(7) }

    // ✅ 初始化完成後，seed 一次（DB 值優先）
    LaunchedEffect(ui.initializing, init) {
        if (!ui.initializing && !seeded) {
            useMetric = (init.unit == UserProfileStore.HeightUnit.CM)
            cmVal = init.cm.coerceIn(CM_MIN, CM_MAX)
            feet = init.feet
            inches = init.inches
            seeded = true
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Edit Your Height",
                onBack = onBack
            )
        },
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
                            vm.saveAndSyncHeight(
                                useMetric = useMetric,
                                cmVal = cmVal,
                                feet = feet,
                                inches = inches,
                                onSuccess = onSaved
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !ui.saving,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        if (ui.saving) {
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

            if (ui.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = ui.error!!,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
            HeightUnitSegmentedSameAsGoal(
                useMetric = useMetric,
                onChange = { isMetric ->
                    if (isMetric) {
                        // 切回 cm：用目前 ft/in 換算回 cm
                        cmVal = feetInchesToCm1(feet, inches).coerceIn(CM_MIN, CM_MAX)
                    } else {
                        // 切到 ft/in：用目前 cm 推導
                        val (ft, inch) = cmToFeetInches1(cmVal)
                        feet = ft
                        inches = inch
                    }
                    useMetric = isMetric
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            if (useMetric) {
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
                        rowHeight = 56.dp,
                        centerTextSize = 30.sp,
                        textSize = 26.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 30.dp)
                    )

                    // 小數點：用固定寬度 Box 來置中
                    Box(
                        modifier = Modifier.width(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".",
                            fontSize = 34.sp,
                            modifier = Modifier.offset(x = 5.dp)
                        )
                    }

                    NumberWheel(
                        range = 0..9,
                        value = cmDecSel,
                        onValueChange = { newDec ->
                            val newCm = (cmIntSel * 10 + newDec) / 10.0
                            cmVal = newCm.coerceIn(CM_MIN, CM_MAX)
                        },
                        rowHeight = 56.dp,
                        centerTextSize = 30.sp,
                        textSize = 26.sp,
                        sideAlpha = 0.35f,
                        unitLabel = null,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 8.dp)
                    )

                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "cm",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier
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
                        rowHeight = 56.dp,
                        centerTextSize = 30.sp,
                        textSize = 26.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "ft",
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 22.dp)
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
                        rowHeight = 56.dp,
                        centerTextSize = 30.sp,
                        textSize = 26.sp,
                        sideAlpha = 0.35f,
                        unitLabel = "in",
                        modifier = Modifier
                            .width(120.dp)
                            .padding(end = 22.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(
                        text = "Set your current Height",
                        fontSize = 12.sp,
                        color = Color(0xFF9AA3AE),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your height is used only to improve calorie estimate accuracy.", //你的身高僅用於提升熱量估算準確度。
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

@Composable
private fun HeightUnitSegmentedSameAsGoal(
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
                .fillMaxWidth(0.60f)
                .heightIn(min = 40.dp)
        ) {
            Row(Modifier.padding(6.dp)) {
                SegItemSameAsGoal(
                    text = "ft",
                    selected = !useMetric,
                    onClick = { onChange(false) },
                    selectedColor = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
                Spacer(Modifier.width(6.dp))
                SegItemSameAsGoal(
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
private fun SegItemSameAsGoal(
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

    // 外部 value 對應到 items 的 index
    val selectedIdx = (value - range.first).coerceIn(0, items.lastIndex)

    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // ✅ 防止「程式對齊」期間又回寫 value 造成跳回去
    var aligning by remember { mutableStateOf(true) }

    // ✅ 外部 value 改變時，Wheel 要跟著對齊（不然一定停錯）
    LaunchedEffect(selectedIdx) {
        if (!state.isScrollInProgress) {
            aligning = true
            state.scrollToItem(selectedIdx)
            aligning = false
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

    // ✅ 使用者滑動時才回寫；程式對齊中不回寫，避免把 DB 值打回舊值
    LaunchedEffect(centerIndex, aligning) {
        if (!aligning) {
            val newValue = items.getOrNull(centerIndex) ?: return@LaunchedEffect
            if (newValue != value) onValueChange(newValue)
        }
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

                    // ✅ 建議只在中心顯示 unit（比較像你 Age 的版本）
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

        // center lines（保留你原本的）
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

