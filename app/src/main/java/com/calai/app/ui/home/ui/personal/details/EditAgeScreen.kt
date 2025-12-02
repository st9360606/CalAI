package com.calai.app.ui.home.ui.personal.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.ui.home.ui.personal.details.model.EditAgeViewModel
import com.calai.app.ui.home.ui.weight.components.WeightTopBar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditAgeScreen(
    vm: EditAgeViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val initialAge by vm.initialAge.collectAsState()

    LaunchedEffect(Unit) { vm.initIfNeeded() }

    val AGE_MIN = 10
    val AGE_MAX = 150

    // ✅ 關鍵：不要用 rememberSaveable，避免 Nav 回來時還原舊的 age/seeded
    var seeded by remember { mutableStateOf(false) }
    var age by remember { mutableIntStateOf(25) }

    LaunchedEffect(ui.initializing, initialAge) {
        if (!ui.initializing && !seeded) {
            age = initialAge.coerceIn(AGE_MIN, AGE_MAX)
            seeded = true
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = { WeightTopBar(title = "Edit Your Age", onBack = onBack) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { vm.saveAndSyncAge(ageYears = age, onSuccess = onSaved) },
                        enabled = !ui.saving && !ui.initializing,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        if (ui.saving) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
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
            Spacer(Modifier.height(140.dp))

            ui.error?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = msg,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }

            val wheelHeight = 56.dp * 5
            Box(
                modifier = Modifier.fillMaxWidth().height(wheelHeight),
                contentAlignment = Alignment.Center
            ) {
                if (ui.initializing) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberWheel(
                            range = AGE_MIN..AGE_MAX,
                            value = age,
                            onValueChange = { age = it },
                            rowHeight = 56.dp,
                            centerTextSize = 32.sp,
                            textSize = 26.sp,
                            sideAlpha = 0.35f,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "We only use your age to improve your experience and calorie estimation accuracy.",
                fontSize = 12.sp,
                color = Color(0xFF9AA3AE),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
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

    // 目標 index（外部 value 對應的 index）
    val selectedIdx = (value - range.first).coerceIn(0, items.lastIndex)

    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    /**
     * ✅ 核心修正：
     * - 外部 value 改變 → selectedIdx 改變
     * - 如果目前不是使用者在滑動，就把列表對齊到 selectedIdx
     * - 這樣 seed(34) 會真的捲到 34，不會卡在 25
     */
    LaunchedEffect(selectedIdx) {
        if (!state.isScrollInProgress) {
            state.scrollToItem(selectedIdx)
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

    /**
     * ✅ 防止無限回寫：
     * 只有當中心值真的跟 value 不同，才通知外層更新
     */
    LaunchedEffect(centerIndex) {
        val newValue = items.getOrNull(centerIndex) ?: return@LaunchedEffect
        if (newValue != value) onValueChange(newValue)
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
                val unitSize = if (isCenter) 18.sp else 16.sp

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
                    if (unitLabel != null && isCenter) {
                        Spacer(Modifier.width(6.dp))
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

        // center lines
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

