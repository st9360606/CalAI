package com.calai.bitecal.ui.home.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.bitecal.R
import com.calai.bitecal.data.workout.api.WorkoutHistorySessionDto
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel
import kotlin.math.roundToInt

private val WorkoutCardWhite = Color(0xFFFFFFFF)
private val WorkoutInk = Color(0xFF111114)
private val WorkoutMuted = Color(0xFF71717A)
private val WorkoutLine = Color(0xFFE5E7EB)
private val WorkoutSubtle = Color(0xFFF1F2F4)

private val WorkoutAccent = Color(0xFFF59E0B)
private val WorkoutSuccess = Color(0xFF15803D)
private val WorkoutSuccessSoft = Color(0xFFEAF7EE)

private val WorkoutDurationBlue = Color(0xFF2563EB)
private val WorkoutDurationBlueSoft = Color(0xFFEFF6FF)

private val WorkoutBurnRed = Color(0xFFE11D48)
private val WorkoutBurnRedSoft = Color(0xFFFFF1F2)

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
        LightHomeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                WorkoutHistoryTopBar(
                    onBack = onBack,
                    onClose = onBack
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
                    start = 20.dp,
                    top = 10.dp,
                    end = 20.dp,
                    bottom = 26.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    WorkoutHistorySummaryCard(
                        todayTotalKcal = todayTotalKcal,
                        averageDailyKcal = averageDailyKcal,
                        averageKcal = averageKcal
                    )
                }

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

                    ui.historyError != null -> {
                        item {
                            WorkoutHistoryStateCard(
                                title = stringResource(R.string.workout_history_error_title),
                                body = stringResource(R.string.workout_history_error),
                                iconTint = MaterialTheme.colorScheme.error,
                                action = {
                                    Button(
                                        onClick = vm::refreshRecentHistory,
                                        enabled = !ui.historyLoading,
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WorkoutInk,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                                    ) {
                                        Text(text = stringResource(R.string.cta_retry))
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
                            WorkoutHistorySessionCard(session = session)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryTopBar(
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .height(48.dp)
    ) {
        WorkoutHistoryCircleButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.workout_history_back),
                tint = WorkoutInk,
                modifier = Modifier.size(21.dp)
            )
        }

        Text(
            text = stringResource(R.string.workout_history_title),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = WorkoutInk
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        WorkoutHistoryCircleButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.workout_history_close),
                tint = WorkoutInk,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WorkoutHistoryCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .background(WorkoutCardWhite, CircleShape)
            .border(1.dp, WorkoutLine, CircleShape)
    ) {
        content()
    }
}

@Composable
private fun WorkoutHistorySummaryCard(
    todayTotalKcal: Int,
    averageDailyKcal: Int,
    averageKcal: Int
) {
    val cardShape = RoundedCornerShape(32.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkoutLine, cardShape),
        shape = cardShape,
        color = WorkoutCardWhite,
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.workout_history_summary_label),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = WorkoutInk,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.workout_history_summary_body),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WorkoutMuted,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(15.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = todayTotalKcal.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = WorkoutBurnRed,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.workout_history_unit_kcal),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = WorkoutMuted,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkoutHistorySummaryMetric(
                    label = stringResource(R.string.workout_history_avg_daily_label),
                    value = stringResource(R.string.workout_history_avg_daily_value, averageDailyKcal),
                    containerColor = WorkoutDurationBlueSoft,
                    valueColor = WorkoutDurationBlue,
                    modifier = Modifier.weight(1f)
                )

                WorkoutHistorySummaryMetric(
                    label = stringResource(R.string.workout_history_avg_label),
                    value = stringResource(R.string.workout_history_avg_value, averageKcal),
                    containerColor = WorkoutBurnRedSoft,
                    valueColor = WorkoutBurnRed,
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
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = WorkoutInk
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WorkoutMuted,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = valueColor,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WorkoutHistorySectionHeader(
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(WorkoutAccent, CircleShape)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = WorkoutInk,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.workout_history_recent_body),
            style = MaterialTheme.typography.bodySmall.copy(
                color = WorkoutMuted,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun WorkoutHistoryStateCard(
    title: String,
    body: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, WorkoutLine, cardShape),
        shape = cardShape,
        color = WorkoutCardWhite,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(WorkoutSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
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

            Spacer(Modifier.height(7.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WorkoutMuted,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            if (action != null) {
                Spacer(Modifier.height(20.dp))
                action()
            }
        }
    }
}

@Composable
private fun WorkoutHistorySessionCard(
    session: WorkoutHistorySessionDto
) {
    val cardShape = RoundedCornerShape(26.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WorkoutLine, cardShape),
        shape = cardShape,
        color = WorkoutCardWhite,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = WorkoutInk,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = stringResource(
                            R.string.workout_history_date_time,
                            session.dateLabel,
                            session.timeLabel
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WorkoutMuted,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = WorkoutSuccessSoft,
                    contentColor = WorkoutSuccess
                ) {
                    Text(
                        text = stringResource(R.string.workout_history_session_completed),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WorkoutSuccess,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkoutHistoryMetricChip(
                    label = stringResource(R.string.workout_history_duration_label),
                    value = stringResource(R.string.workout_history_minutes, session.minutes),
                    modifier = Modifier.weight(1f),
                    containerColor = WorkoutDurationBlueSoft,
                    valueColor = WorkoutDurationBlue,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = WorkoutDurationBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )

                WorkoutHistoryMetricChip(
                    label = stringResource(R.string.workout_history_burn_label),
                    value = stringResource(R.string.workout_history_kcal_with_unit, session.kcal),
                    modifier = Modifier.weight(1f),
                    containerColor = WorkoutBurnRedSoft,
                    valueColor = WorkoutBurnRed,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = WorkoutBurnRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = WorkoutSubtle,
    valueColor: Color = WorkoutInk,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = WorkoutInk
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = WorkoutMuted,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = valueColor,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
