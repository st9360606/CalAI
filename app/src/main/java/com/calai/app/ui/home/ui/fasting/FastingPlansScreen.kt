package com.calai.app.ui.home.ui.fasting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.data.fasting.model.FastingPlan
import com.calai.app.ui.home.HomeTab
import com.calai.app.ui.home.ui.components.MainBottomBar
import com.calai.app.ui.home.ui.fasting.model.FastingPlanViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val Black = Color(0xFF111114)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingPlansScreen(
    vm: FastingPlanViewModel,
    onBack: () -> Unit,
    currentTab: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (state.loading) vm.load() }

    var showCupertinoPicker by rememberSaveable { mutableStateOf(false) }

    // ✅ 新增：避免 Save 連點 & 讓按鈕顯示 loading
    var saving by rememberSaveable { mutableStateOf(false) }

    // ✅ 新增：UI scope，讓我們能 await ViewModel 的 job
    val uiScope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (showCupertinoPicker) showCupertinoPicker = false else onBack()
    }

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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Fasting Plans",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.height(28.dp)
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
        // ➜ 底部只保留固定的 BottomNav
        bottomBar = {
            MainBottomBar(
                current = currentTab,
                onOpenTab = onOpenTab
            )
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
            // 標題與卡片區域的額外間距
            Spacer(Modifier.height(2.dp))

            // === 禁食計畫卡片 ===
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 2000.dp)
            ) {
                items(FastingPlan.entries) { plan ->
                    val selected = plan == state.selected
                    FastingPlanCard(plan, selected) { vm.onPlanSelected(plan) }
                }
            }

            Spacer(Modifier.height(2.dp))

            // === 開始時間 ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .clickable {
                            showCupertinoPicker = true
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "start time",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(start = 18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Color(0xFF4F4F4F),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showCupertinoPicker = true },
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, Color(0xFFB8B8B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format24h(state.start),
                            fontSize = 21.sp,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(13.dp))

            // === 結束時間 ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "end time",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.4f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, Color(0xFFB8B8B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format24h(state.end),
                            fontSize = 21.sp,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // === SAVE 按鈕（跟內容一起捲動） ===
            Button(
                enabled = !saving,
                onClick = {
                    if (saving) return@Button
                    saving = true

                    uiScope.launch {
                        // ✅ 關鍵：等 VM 的 persist job 完成，才返回
                        val job = vm.persistAndReschedule(showToast = true)
                        job.join()

                        saving = false
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black,
                    contentColor = Color.White,
                    disabledContainerColor = Black.copy(alpha = 0.5f),
                    disabledContentColor = Color.White
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(text = stringResource(R.string.fasting_plan_save))
            }

            Spacer(Modifier.height(16.dp))
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
        "14:10" -> "🥑"
        "16:8" -> "🍊"
        "12:12" -> "🍎"
        "18:6" -> "🍇"
        "20:4" -> "🥝"
        "22:2" -> "🍋"
        else -> "🍽️"
    }
    val desc = when (plan.code) {
        "14:10" -> "Ease in with a steady rhythm—14 hours to reset appetite cues and energy."
        "16:8" -> "Crowd favorite for a reason—16 hours to stay focused and in a lean groove."
        "12:12" -> "Balanced split: fast 12 hours, eat within a 12-hour window — perfect for beginners"
        "18:6" -> "18 hours of fasting, with a 6-hour eating window — a leaner schedule than 16:8"
        "20:4" -> "Stay laser-focused—1–2 mindful meals within a tight 4-hour window."
        "22:2" -> "One intentional meal in a 2-hour slot—keep water intake up."
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
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
                drawRoundRect(
                    color = cardBorder,
                    style = Stroke(width = 1.dp.toPx()),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .padding(2.dp)
    ) {
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            CupertinoSwitch(
                checked = selected,
                onCheckedChange = onSelect
            )
        }
    }
}

/* -----------------------------
   自訂 Cupertino 風格 Switch
   （沒有灰圓底閃爍）
------------------------------ */

@Composable
private fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbSize = 28.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF34C759) else Color(0xFFEAEAEA),
        animationSpec = tween(durationMillis = 160),
        label = "switchTrackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 2.dp else 2.dp,
        animationSpec = tween(durationMillis = 160),
        label = "switchThumbOffset"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // ✅ 不要 ripple / 灰圓底
            ) { onCheckedChange() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 2.dp, shape = CircleShape, clip = true)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun SelectionBandBehind() {
    val bandHeight = 44.dp
    val bandRadius = 10.dp
    val bandColor = Color(0xFFFAFAFA)
    val lineColor = Color(0xFFD1D1D6)

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .height(bandHeight)
                .clip(RoundedCornerShape(bandRadius))
                .background(bandColor)
        )
        val lineW = 1.dp
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
    infinite: Boolean,
    selectedFontSize: TextUnit = 28.sp,
    unselectedFontSize: TextUnit = 18.sp,
    selectedFontWeight: FontWeight = FontWeight.SemiBold,
    unselectedFontWeight: FontWeight = FontWeight.Normal,
) {
    val visibleCount = 5
    val itemHeight = 44.dp

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
            val goal = centerListIndex
            listState.animateScrollToItem(goal, 0)
            onSnapped(normalize(goal))
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

            val fontSize = if (isCenter) selectedFontSize else unselectedFontSize
            val weight = if (isCenter) selectedFontWeight else unselectedFontWeight
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

private fun format24h(t: LocalTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return t.format(formatter)
}

private val IOS_TEXT = Color(0xFF1C1C1E)
private val IOS_TEXT_FADED = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CupertinoWheelTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by rememberSaveable(initial) { mutableIntStateOf(initial.hour) }     // 0..23
    var minute by rememberSaveable(initial) { mutableIntStateOf(initial.minute) } // 0..59

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { goal -> goal != SheetValue.Hidden }
    )

    ModalBottomSheet(
        onDismissRequest = { /* 故意留空：只能按 Cancel */ },
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp)
                .padding(start = 20.dp, end = 20.dp, top = 5.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set your fasting start time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "We'll remind you at this time each day when your eating window starts.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SelectionBandBehind()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        WheelColumn(
                            values = (0..23).map { "%02d".format(it) },
                            startIndex = hour,
                            columnWidth = 120.dp,
                            onSnapped = { idx -> hour = idx },
                            infinite = true,
                            selectedFontSize = 26.sp,
                            unselectedFontSize = 19.sp
                        )

                        Box(
                            modifier = Modifier.width(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                        }

                        WheelColumn(
                            values = (0..59).map { "%02d".format(it) },
                            startIndex = minute,
                            columnWidth = 120.dp,
                            onSnapped = { idx -> minute = idx },
                            infinite = true,
                            selectedFontSize = 26.sp,
                            unselectedFontSize = 19.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onConfirm(LocalTime.of(hour, minute))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) { Text("Save", fontSize = 16.sp) }

                OutlinedButton(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFE1E4EA),
                        contentColor = Color(0xFF111114)
                    )
                ) { Text("Cancel", fontSize = 16.sp) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
