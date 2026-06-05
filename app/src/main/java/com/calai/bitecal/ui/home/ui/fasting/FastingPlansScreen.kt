package com.calai.bitecal.ui.home.ui.fasting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.data.fasting.model.FastingPlan
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.common.design.BiteCalCommonScreenTokens
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.home.ui.fasting.model.FastingPlanViewModel
import com.calai.bitecal.ui.common.haptic.HapticWheelTickEffect
import com.calai.bitecal.ui.common.haptic.clickWithoutHaptic
import com.calai.bitecal.ui.common.haptic.consumeClickWithoutHaptic
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame


@Composable
fun FastingPlansScreen(
    vm: FastingPlanViewModel,
    onBack: () -> Unit,
    currentTab: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.loading) vm.load()
    }

    var showCupertinoPicker by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    val uiScope = rememberCoroutineScope()

    fun persistFastingPlan(
        showToast: Boolean,
        navigateBack: Boolean,
        closePicker: Boolean = false
    ) {
        if (saving) return

        saving = true

        uiScope.launch {
            try {
                val job = vm.persistAndReschedule(showToast = showToast)
                job.join()

                if (closePicker) {
                    showCupertinoPicker = false
                }

                if (navigateBack) {
                    onBack()
                }
            } finally {
                saving = false
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            saving -> Unit
            showCupertinoPicker -> showCupertinoPicker = false
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BiteCalCommonScreenTokens.softGrayBackground)
    ) {
        Scaffold(
            containerColor = BiteCalCommonScreenTokens.softGrayBackground,
            topBar = {
                BiteCalTopBar(
                    title = stringResource(R.string.fasting_plans),
                    onBack = onBack
                )
            },
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
                    .background(BiteCalCommonScreenTokens.softGrayBackground)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 5.dp)
            ) {
                Spacer(Modifier.height(2.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier.heightIn(max = 2000.dp)
                ) {
                    items(FastingPlan.entries) { plan ->
                        val selected = plan == state.selected

                        FastingPlanCard(plan, selected) {
                            if (saving) return@FastingPlanCard

                            vm.onPlanSelected(plan)

                            persistFastingPlan(
                                showToast = false,
                                navigateBack = false
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                FastingNotificationInfoCard(
                    planCode = state.selected.code,
                    startText = format24h(state.start),
                    endSoonText = format24h(state.end.minusHours(1)),
                    endText = format24h(state.end),
                    remindersEnabled = state.enabled,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .biteCalClickable { showCupertinoPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.start_time),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
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

                    FastingPlanTimeValueCard(
                        text = format24h(state.start),
                        enabled = true,
                        onClick = { showCupertinoPicker = true },
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(48.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.end_time),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth(0.4f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    FastingPlanTimeValueCard(
                        text = format24h(state.end),
                        enabled = false,
                        onClick = null,
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(48.dp)
                    )
                }
                Spacer(Modifier.height(44.dp))
            }
        }

        if (showCupertinoPicker) {
            CupertinoWheelTimePickerSheet(
                initial = state.start,
                saving = saving,
                onDismiss = {
                    if (!saving) {
                        showCupertinoPicker = false
                    }
                },
                onConfirm = { picked ->
                    if (!saving) {
                        vm.onChangeStart(picked)

                        persistFastingPlan(
                            showToast = true,
                            navigateBack = true,
                            closePicker = true
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun FastingNotificationInfoCard(
    planCode: String,
    startText: String,
    endSoonText: String,
    endText: String,
    remindersEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = stringResource(
        if (remindersEnabled) {
            R.string.fasting_notification_info_status_on
        } else {
            R.string.fasting_notification_info_status_off
        }
    )
    val statusLabel = stringResource(
        if (remindersEnabled) {
            R.string.fasting_notification_info_status_enabled_label
        } else {
            R.string.fasting_notification_info_status_disabled_label
        }
    )

    val statusDotColor = if (remindersEnabled) Color(0xFF22C55E) else Color(0xFF9CA3AF)
    val statusTextColor = if (remindersEnabled) Color(0xFF166534) else Color(0xFF4B5563)
    val statusChipBg = if (remindersEnabled) Color(0xFFEAF7EF) else Color(0xFFF3F4F6)
    val statusChipBorder = if (remindersEnabled) Color(0xFFCDEBD8) else Color(0xFFDADDE3)

    val reminderIconBg = Color(0xFFFAFAFA)
    val reminderIconMain = Color(0xFFF97316)
    val reminderBadgeBg = Color.White
    val reminderBadgeText = Color(0xFF111114)
    val reminderBadgeBorder = Color(0xFFDADDE3)

    val statusBoxBg = Color(0xFFFAFAFA)
    val statusBoxBorder = Color(0xFFE5E7EB)
    val statusBoxText = Color(0xFF374151)

    Card(
        modifier = modifier
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        border = CardStyles.Border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(reminderIconBg)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .clip(CircleShape)
                            .background(reminderIconMain)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.88f))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-5).dp, y = 5.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(reminderBadgeBg)
                            .border(
                                width = 1.dp,
                                color = reminderBadgeBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = reminderBadgeText,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.fasting_notification_info_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF111114),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    lineHeight = 21.sp
                                )
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = stringResource(
                                    R.string.fasting_notification_info_subtitle,
                                    planCode,
                                    endText
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(statusChipBg)
                                .border(
                                    width = 1.dp,
                                    color = statusChipBorder,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = statusTextColor,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFAFAFA))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                NotificationTimelineRow(
                    stepText = "1",
                    accentColor = Color(0xFF111114),
                    timeText = stringResource(R.string.fasting_notification_info_at_time, startText),
                    title = stringResource(R.string.fasting_notification_info_start_title),
                    body = stringResource(R.string.fasting_notification_info_start_body),
                    isLast = false
                )

                NotificationTimelineRow(
                    stepText = "2",
                    accentColor = Color(0xFF111114),
                    timeText = stringResource(R.string.fasting_notification_info_at_time, endSoonText),
                    title = stringResource(R.string.fasting_notification_info_endsoon_title),
                    body = stringResource(R.string.fasting_notification_info_endsoon_body, endText),
                    isLast = true
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = statusBoxText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusBoxBg)
                    .border(
                        width = 1.dp,
                        color = statusBoxBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun NotificationTimelineRow(
    stepText: String,
    accentColor: Color,
    timeText: String,
    title: String,
    body: String,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFDADDE3),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = accentColor,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(46.dp)
                        .background(Color(0xFFE5E7EB))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF111114),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF111114),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false)
            .biteCalClickable(role = Role.Button, onClick = onSelect),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        border = CardStyles.Border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
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
}

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
            .biteCalClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCheckedChange
            ),
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
    selectedFontSize: TextUnit = 20.sp,
    unselectedFontSize: TextUnit = 19.sp,
    selectedFontWeight: FontWeight = FontWeight.Bold,
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

    HapticWheelTickEffect(
        tickKey = normalize(centerListIndex),
        enabled = listState.isScrollInProgress
    )

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

private fun format24h(t: LocalTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return t.format(formatter)
}

@Composable
private fun FastingPlanTimeValueCard(
    text: String,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (enabled && onClick != null) {
        Modifier.biteCalClickable(
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .shadow(CardStyles.Elevation, CardStyles.Corner, clip = false)
            .then(clickableModifier),
        shape = CardStyles.Corner,
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        border = CardStyles.Border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = if (enabled) {
                        Color(0xFF0F172A)
                    } else {
                        Color(0xFF0F172A).copy(alpha = 0.62f)
                    },
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

private val IOS_TEXT = Color(0xFF111114)
private val IOS_TEXT_FADED = Color(0xFF8E8E93)

@Composable
private fun CupertinoWheelTimePickerSheet(
    initial: LocalTime,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by rememberSaveable(initial) { mutableIntStateOf(initial.hour) }
    var minute by rememberSaveable(initial) { mutableIntStateOf(initial.minute) }

    val confirmClick = rememberClickWithHaptic(enabled = !saving) {
        if (!saving) {
            onConfirm(LocalTime.of(hour, minute))
        }
    }
    val dismissClick = rememberClickWithHaptic(enabled = !saving, onClick = onDismiss)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickWithoutHaptic(
                    enabled = !saving,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFFF5F5F5))
                .padding(start = BiteCalScreenFrame.contentHorizontal, end = BiteCalScreenFrame.contentHorizontal, top = BiteCalScreenFrame.contentTop, bottom = BiteCalScreenFrame.contentBottomLarge)
                .consumeClickWithoutHaptic(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFD1D1D6))
            )

            Text(
                text = stringResource(R.string.fasting_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.fasting_picker_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

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
                            columnWidth = 92.dp,
                            onSnapped = { idx -> hour = idx },
                            infinite = true,
                            selectedFontSize = 22.sp,
                            unselectedFontSize = 21.sp
                        )

                        Box(
                            modifier = Modifier.width(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                        }

                        WheelColumn(
                            values = (0..59).map { "%02d".format(it) },
                            startIndex = minute,
                            columnWidth = 92.dp,
                            onSnapped = { idx -> minute = idx },
                            infinite = true,
                            selectedFontSize = 22.sp,
                            unselectedFontSize = 21.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = confirmClick,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.common_close),
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = dismissClick,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFE1E4EA),
                        contentColor = Color(0xFF111114)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
