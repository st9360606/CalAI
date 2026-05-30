package com.calai.bitecal.ui.home.ui.workout

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.data.workout.api.WorkoutHistorySessionDto
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.components.toast.DeleteFailedTopToast
import com.calai.bitecal.ui.home.components.toast.DeleteSuccessTopToast
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutDeleteToastType
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

// --- 精煉後的色彩計畫 (維持原意涵，但微調適用於無邊框設計) ---
private val WorkoutCardWhite = Color(0xFFFFFFFF)
private val WorkoutInk = Color(0xFF18181B) // 稍微加深提升對比
private val WorkoutMuted = Color(0xFF8E8E93) // 更柔和的次要文字
private val WorkoutSubtle = Color(0xFFF4F4F5)
private val WorkoutAccent = Color(0xFFF59E0B)
private val WorkoutTimelineBar = Color(0xFF18181B)
private val WorkoutDurationBlue = Color(0xFF3B82F6)
private val WorkoutBurnRed = Color(0xFFF43F5E)

private val WorkoutMetricPrimarySoft = Color(0xFFF5F5F6)
private val WorkoutMetricPrimaryInk = Color(0xFF2F2F33)
private val WorkoutMetricSecondarySoft = Color(0xFFF5F5F6)
private val WorkoutMetricSecondaryInk = Color(0xFF2F2F33)
private const val WorkoutHistoryRangeDays = 7

@Composable
fun WorkoutHistoryScreen(
    vm: WorkoutViewModel,
    onBack: () -> Unit,
    currentTab: HomeTab = HomeTab.Workout,
    onOpenTab: (HomeTab) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        vm.init()
        vm.refreshRecentHistory()
    }

    val ui = vm.ui.collectAsStateWithLifecycle().value
    val deleteToastType = ui.deleteToastType
    val deleteToastTick = ui.deleteToastTick
    val history = ui.recentHistory
    val sessions = history?.sessions.orEmpty()
    val totalKcal = history?.totalKcal ?: 0

    val todayTotalKcal = ui.today?.totalKcalToday ?: 0
    val averageDailyKcal = (totalKcal.toDouble() / WorkoutHistoryRangeDays).roundToInt()
    val averageKcal = if (sessions.isNotEmpty()) {
        (totalKcal.toDouble() / sessions.size).roundToInt()
    } else {
        0
    }

    LaunchedEffect(ui.saving) {
        if (ui.saving) onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景保持不變
        LightHomeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BiteCalTopBar(
                    title = stringResource(R.string.workout_history_title),
                    onBack = onBack
                )
            },
            bottomBar = {
                MainBottomBar(
                    current = currentTab,
                    onOpenTab = onOpenTab
                )
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp, // 稍微增加兩側留白，更具呼吸感
                    top = 2.dp,
                    end = 24.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp) // 拉大區塊間距
            ) {
                // 1. 總覽卡片
                item {
                    WorkoutHistorySummaryCard(
                        todayTotalKcal = todayTotalKcal,
                        averageDailyKcal = averageDailyKcal,
                        averageKcal = averageKcal
                    )
                }

                // 2. 狀態或歷史列表
                when {
                    ui.historyLoading -> {
                        item {
                            WorkoutHistoryStateCard(
                                title = stringResource(R.string.workout_history_loading_title),
                                body = stringResource(R.string.workout_history_loading),
                                iconTint = WorkoutAccent
                            )
                        }
                    }

                    ui.historyError -> {
                        item {
                            WorkoutHistoryStateCard(
                                title = stringResource(R.string.workout_history_error_title),
                                body = stringResource(R.string.workout_history_error),
                                iconTint = MaterialTheme.colorScheme.error,
                                action = {
                                    Button(
                                        onClick = rememberClickWithHaptic(onClick = vm::refreshRecentHistory),
                                        enabled = !ui.historyLoading,
                                        shape = RoundedCornerShape(16.dp), // 更現代的按鈕圓角
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WorkoutInk,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                                    ) {
                                        Text(text = stringResource(R.string.cta_retry), fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }

                    sessions.isEmpty() -> {
                        item {
                            WorkoutHistoryStateCard(
                                title = stringResource(R.string.workout_history_empty_title),
                                body = stringResource(R.string.workout_history_empty),
                                iconTint = WorkoutMuted
                            )
                        }
                    }

                    else -> {
                        item {
                            WorkoutHistorySectionHeader(
                                title = stringResource(R.string.workout_history_recent_title)
                            )
                        }

                        items(
                            items = sessions,
                            key = { session -> session.id }
                        ) { session ->
                            SwipeToDeleteWorkoutSessionTile(
                                session = session,
                                deleting = ui.deletingSessionIds.contains(session.id),
                                onDeleteClick = {
                                    vm.deleteHistorySession(session.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        when (deleteToastType) {
            WorkoutDeleteToastType.SUCCESS -> {
                DeleteSuccessTopToast(
                    message = stringResource(R.string.workout_history_delete_success),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            WorkoutDeleteToastType.FAILED -> {
                DeleteFailedTopToast(
                    message = stringResource(R.string.workout_history_delete_failed),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            null -> Unit
        }
    }

    LaunchedEffect(deleteToastTick, deleteToastType) {
        if (deleteToastType != null) {
            delay(2_000)
            vm.clearDeleteToast()
        }
    }

    // ✅ 防止刪除 toast 還沒自動消失就離開 WorkoutHistoryScreen，回來後又顯示舊 toast。
    DisposableEffect(Unit) {
        onDispose {
            vm.clearDeleteToast()
        }
    }
}

@Composable
private fun WorkoutHistorySummaryCard(
    todayTotalKcal: Int,
    averageDailyKcal: Int,
    averageKcal: Int
) {
    val cardShape = RoundedCornerShape(28.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = WorkoutCardWhite,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp // 增加些微層次感
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_history_summary_label),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = WorkoutInk,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = stringResource(R.string.workout_history_summary_body),
                style = MaterialTheme.typography.bodyMedium.copy(color = WorkoutMuted)
            )

            Spacer(Modifier.height(18.dp))

            // 將最重要的今日卡路里與平均數據左右分流
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.workout_history_today_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WorkoutBurnRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = todayTotalKcal.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                color = WorkoutInk,
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.workout_history_unit_kcal),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = WorkoutMuted,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(15.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WorkoutHistorySummaryMetric(
                    label = stringResource(R.string.workout_history_avg_daily_label),
                    value = stringResource(R.string.workout_history_avg_daily_value, averageDailyKcal),
                    containerColor = WorkoutMetricPrimarySoft,
                    valueColor = WorkoutMetricPrimaryInk,
                    modifier = Modifier.weight(1f)
                )

                WorkoutHistorySummaryMetric(
                    label = stringResource(R.string.workout_history_avg_label),
                    value = stringResource(R.string.workout_history_avg_value, averageKcal),
                    containerColor = WorkoutMetricSecondarySoft,
                    valueColor = WorkoutMetricSecondaryInk,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistorySummaryMetric(
    label: String,
    value: String,
    containerColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WorkoutMuted,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = valueColor,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WorkoutHistorySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            color = WorkoutInk,
            fontWeight = FontWeight.ExtraBold
        ),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun WorkoutHistoryStateCard(
    title: String,
    body: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = WorkoutCardWhite,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(WorkoutSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = WorkoutInk,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WorkoutMuted
                ),
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(Modifier.height(24.dp))
                action()
            }
        }
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SwipeToDeleteWorkoutSessionTile(
    session: WorkoutHistorySessionDto,
    deleting: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val scope = rememberCoroutineScope()
        val actionWidth = maxWidth * 0.25f
        val density = LocalDensity.current
        val actionWidthPx = with(density) { actionWidth.toPx() }
        val openThresholdPx = actionWidthPx * 0.42f
        val flingThresholdPx = with(density) { 380.dp.toPx() }

        val offsetX = remember(session.id) { Animatable(0f) }
        val isOpened = offsetX.value < -1f

        val dragState = rememberDraggableState { delta ->
            scope.launch {
                val next = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                offsetX.snapTo(next)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFE46A6A))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
                    .fillMaxHeight()
                    .padding(end = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = rememberClickWithHaptic {
                        scope.launch { offsetX.snapTo(0f) }
                        onDeleteClick()
                    },
                    enabled = isOpened && !deleting,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("workout_history_delete_button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash),
                        contentDescription = stringResource(
                            R.string.workout_history_delete_content_description
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            WorkoutHistorySessionTile(
                session = session,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = offsetX.value
                    }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        enabled = !deleting,
                        onDragStarted = {
                            scope.launch { offsetX.stop() }
                        },
                        onDragStopped = { velocity ->
                            val target = when {
                                velocity <= -flingThresholdPx -> -actionWidthPx
                                velocity >= flingThresholdPx -> 0f
                                offsetX.value <= -openThresholdPx -> -actionWidthPx
                                else -> 0f
                            }

                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = 180,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    )
                    .biteCalClickable(enabled = isOpened) {
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 160,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
            )
        }
    }
}

@Composable
private fun WorkoutHistorySessionTile(
    session: WorkoutHistorySessionDto,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = WorkoutCardWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(WorkoutTimelineBar)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedWorkoutName(rawName = session.name),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = WorkoutInk,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = stringResource(R.string.workout_history_date_time, session.dateLabel, session.timeLabel),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WorkoutMuted,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.fire),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = stringResource(
                            R.string.workout_history_kcal_with_unit,
                            session.kcal
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = WorkoutInk,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = WorkoutDurationBlue,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = stringResource(
                            R.string.workout_history_minutes,
                            session.minutes
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = WorkoutMuted,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
