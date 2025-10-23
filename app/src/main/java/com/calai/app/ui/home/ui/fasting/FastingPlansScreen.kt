package com.calai.app.ui.home.ui.fasting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.data.fasting.model.FastingPlan
import com.calai.app.ui.home.ui.fasting.model.FastingPlanViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import com.calai.app.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FastingPlansScreen(
    vm: FastingPlanViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (state.loading) vm.load() }

    var showCupertinoPicker by rememberSaveable { mutableStateOf(false) }
    BackHandler(true) { onBack() }

    if (showCupertinoPicker) {
        CupertinoWheelTimePickerDialog(
            initial = state.start,
            onDismiss = { showCupertinoPicker = false },
            onConfirm = { picked ->
                vm.onChangeStart(picked)
                showCupertinoPicker = false
            }
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Fasting Plans",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            )
        },
        bottomBar = {
            Surface(color = Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 37.dp)
                ) {
                    Button(
                        onClick = {
                            onBack()
                            vm.persistAndReschedule()
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.9f)
                            .height(60.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF000000),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.fasting_plan_save),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 19.sp
                            )
                        )
                    }
                }
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 5.dp)
        ) {
            // === 禁食計畫卡片 ===
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp), // ✅ 間距更緊湊
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 2000.dp)
            ) {
                items(FastingPlan.entries) { plan ->
                    val selected = plan == state.selected
                    FastingPlanCard(plan, selected) { vm.onPlanSelected(plan) }
                }
            }

            Spacer(Modifier.height(28.dp)) // 🔹 間隔略縮小

            // === 開始時間 ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(start = 10.dp), // 🔹 整體往右一點
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Start time",
                        style = MaterialTheme.typography.titleLarge.copy( // 改用 titleLarge
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,                             // 額外指定大小
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(start = 24.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF4F4F4F),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showCupertinoPicker = true },
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, Color(0xFF5C5C5C)), // ✅ 外框略深
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text(
                        text = format12hEn(state.start),
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.Black)
                    )
                }
            }

            Spacer(Modifier.height(13.dp)) // 🔹 間距略縮小

            // === 結束時間 ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(start = 10.dp), // 🔹 整體往右一點
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "End time",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.padding(start = 30.dp)
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)), // ✅ 淺灰外框
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text(
                        text = format12hEn(state.end),
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.Black.copy(alpha = 0.6f))
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

/* -----------------------------
   禁食計畫卡片（改版）
------------------------------ */

@Composable
private fun FastingPlanCard(
    plan: FastingPlan,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val emoji = when (plan.code) {
        "14:10" -> "🍏"
        "16:8" -> "🍊"
        "20:4" -> "🥝"
        "22:2" -> "🍋"
        "6:1" -> "🥥"
        "5:2" -> "🥑"
        else -> "🍽️"
    }
    val desc = when (plan.code) {
        "14:10" -> "Easy your way into fasting for 14 hours, eating within a 10-hour window"
        "16:8" -> "Our most popular tracker! 16 hours fasting, 8 hours eating"
        "20:4" -> "20 hours of fasting. 1–2 meals per day within a 4-hour window"
        "22:2" -> "22 hours of fasting. One meal a day within a 2-hour eating period"
        "6:1" -> "One fasting day per week. No more than 500 calories on a fasting day"
        "5:2" -> "Two fasting days per week. Two days break between fasts. Stay hydrated"
        else -> ""
    }

    val neutralCard = Color(0xFFFAFAFA)
    val cardBorder = Color(0xFFDDDDDD)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = neutralCard,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
                drawRoundRect(
                    color = cardBorder,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .padding(2.dp)
    ) {
        // 🔹 上方主內容（標題 + 描述）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = plan.code,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = Color(0xFF0A0A0A)
                    )
                )
                Text(
                    text = emoji,
                    fontSize = 30.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6F6F6F),
                    lineHeight = 18.sp
                ),
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        // 🔹 固定右下角 Switch
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Switch(
                checked = selected,
                onCheckedChange = { onSelect() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFEAEAEA),
                    uncheckedBorderColor = Color(0xFFE3E3E3)
                ),
                interactionSource = remember { MutableInteractionSource() }
            )
        }
    }
}


@Composable
private fun SelectionBandBehind() {
    val bandHeight = 44.dp
    val bandRadius = 10.dp

    // 更淡的灰：systemGray6 / systemGray4
    val bandColor = Color(0xFFFAFAFA) // 更淡的背景灰
    val lineColor = Color(0xFFD1D1D6) // 更淡的分隔線灰

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .height(bandHeight)
                .clip(RoundedCornerShape(bandRadius))
                .background(bandColor)      // 淡灰底
        )
        val lineW = 1.dp                   // 線也細一點
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = -bandHeight / 2)
                .fillMaxWidth(0.92f)
                .height(lineW)
                .background(lineColor)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = bandHeight / 2)
                .fillMaxWidth(0.92f)
                .height(lineW)
                .background(lineColor)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    values: List<String>,
    startIndex: Int,
    columnWidth: Dp,
    onSnapped: (index: Int) -> Unit,
    infinite: Boolean
) {
    val visibleCount = 5
    val itemHeight = 44.dp

    // 是否無限滾動
    val total: Int
    val initIndex: Int
    val normalize: (Int) -> Int
    if (infinite) {
        val loop = 1000
        total = values.size * loop
        val base = (loop / 2) * values.size
        initIndex = (base + startIndex).coerceIn(0, total - 1)
        normalize = { idx -> ((idx % values.size) + values.size) % values.size }
    } else {
        total = values.size
        initIndex = startIndex.coerceIn(0, total - 1)
        normalize = { idx -> idx.coerceIn(0, total - 1) }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIndex)
    val fling = rememberSnapFlingBehavior(listState)

    // 找到最接近中央的 item
    val centerListIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val vpCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - vpCenter)
            }?.index ?: initIndex
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val target = centerListIndex
            listState.animateScrollToItem(target, 0)
            onSnapped(normalize(target))
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
        modifier = Modifier
            .width(columnWidth)
            .height(itemHeight * visibleCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(total) { i ->
            val show = values[normalize(i)]
            val isCenter = i == centerListIndex
            val fontSize = if (isCenter) 28.sp else 18.sp
            val weight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal
            // ✅ 固定純灰，不用主題色也不用 alpha 疊色
            val color = if (isCenter) IOS_TEXT else IOS_TEXT_FADED

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = show,
                    fontSize = fontSize,
                    fontWeight = weight,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* -------- helpers -------- */

private fun to12hTuple(t: LocalTime): Triple<Int, Int, Boolean> {
    val isAm = t.hour < 12
    val h12 = when (val h = t.hour % 12) { 0 -> 12; else -> h }
    return Triple(h12, t.minute, isAm)
}
private fun from12h(hour12: Int, minute: Int, isAm: Boolean): LocalTime {
    val h = when {
        isAm && hour12 == 12 -> 0
        !isAm && hour12 != 12 -> hour12 + 12
        else -> hour12
    }
    return LocalTime.of(h, minute)
}
private fun format12hEn(t: LocalTime): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    return t.format(formatter)
}

/* ==============================
   iOS 風格轉盤（灰底在數字下面，固定純灰）
   ============================== */

private val IOS_BLUE = Color(0xFF007AFF)
private val IOS_TEXT = Color(0xFF1C1C1E)     // 主字：純深灰(幾近黑)
private val IOS_TEXT_FADED = Color(0xFF8E8E93) // 次字：systemGray

@Composable
private fun CupertinoWheelTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val (initHour12, initMinute, initIsAm) = remember(initial) { to12hTuple(initial) }
    var hour by remember { mutableStateOf(initHour12) }    // 1..12
    var minute by remember { mutableStateOf(initMinute) }  // 0..59
    var isAm by remember { mutableStateOf(initIsAm) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // 讓內建動作區留白，我們自己在內容裡置中放按鈕
        confirmButton = {},
        dismissButton = {},
        containerColor = Color.White, // ✅ 整個對話框白底
        tonalElevation = 0.dp,        // ✅ 移除動態陰影色
        shape = RoundedCornerShape(24.dp), // ✅ 更圓滑
        text = {
            Column(
                modifier = Modifier.background(Color.White), // ✅ 再保險：內部內容白底
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 轉盤本體
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SelectionBandBehind() // 灰底在數字下方

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        WheelColumn(
                            values = (1..12).map { it.toString() },
                            startIndex = hour - 1,
                            columnWidth = 84.dp,
                            onSnapped = { hour = it + 1 },
                            infinite = true
                        )
                        Spacer(Modifier.width(8.dp))
                        WheelColumn(
                            values = (0..59).map { "%02d".format(it) },
                            startIndex = minute,
                            columnWidth = 84.dp,
                            onSnapped = { minute = it },
                            infinite = true
                        )
                        Spacer(Modifier.width(8.dp))
                        WheelColumn(
                            values = listOf("AM", "PM"),
                            startIndex = if (isAm) 0 else 1,
                            columnWidth = 84.dp,
                            onSnapped = { isAm = it == 0 },
                            infinite = false
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ★ 置中按鈕列
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", fontSize = 22.sp, color = IOS_BLUE, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(28.dp))
                    TextButton(onClick = { onConfirm(from12h(hour, minute, isAm)) }) {
                        Text("OK", fontSize = 22.sp, color = IOS_BLUE, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    )
}