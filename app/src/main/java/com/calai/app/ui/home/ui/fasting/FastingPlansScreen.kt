package com.calai.app.ui.home.ui.fasting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Fasting Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 0.dp,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()  // 保留安全區
                        .padding(bottom = 37.dp)  // 浮起距離底部
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
                            containerColor = Color(0xFF000000),  // 🔥 黑底
                            contentColor = Color.White           // 白字
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 10.dp
                        )
                    ) {
                        Text(
                            text = "儲存",
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
        // ★ 改成可滾動
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // 加入滾動
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("選擇禁食計畫", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            FastingPlanList(
                plans = FastingPlan.entries,
                selected = state.selected,
                onSelect = vm::onPlanSelected
            )

            Text("開始時間", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { showCupertinoPicker = true },
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, Color(0xFF8C8C8C)), // 🔹 淺灰邊框 (#BDBDBD)
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text(
                    format12hEn(state.start), // 🔹 改成 AM/PM 英文格式
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.Black)
                )
            }

            Text("結束時間", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { /* no-op */ },
                enabled = false,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)), // 🔹 更淡灰邊框 (#E0E0E0)
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text(
                    format12hEn(state.end),
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.Black.copy(alpha = 0.6f))
                )
            }

            Spacer(Modifier.height(80.dp)) // 確保滾動時底部留白
        }
    }
}

@Composable
private fun FastingPlanList(
    plans: List<FastingPlan>,
    selected: FastingPlan?,
    onSelect: (FastingPlan) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plans.forEach { plan ->
            PlanRow(
                plan = plan,
                selected = plan == selected,
                onSelect = { onSelect(plan) }
            )
        }
    }
}

@Composable
private fun PlanRow(plan: FastingPlan, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        onClick = onSelect
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(plan.code, style = MaterialTheme.typography.titleLarge)
            Text(
                "${plan.fastingHours}h fasting • ${plan.eatingHours}h eating",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
        shape = MaterialTheme.shapes.medium,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

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
                        Text("CANCEL", fontSize = 20.sp, color = IOS_BLUE, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(28.dp))
                    TextButton(onClick = { onConfirm(from12h(hour, minute, isAm)) }) {
                        Text("OK", fontSize = 20.sp, color = IOS_BLUE, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    )
}


@Composable
private fun SelectionBandBehind() {
    val bandHeight = 44.dp
    val bandRadius = 10.dp

    // 更淡的灰：systemGray6 / systemGray4
    val bandColor = Color(0xFFF2F2F7) // 更淡的背景灰
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

