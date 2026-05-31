package com.calai.bitecal.ui.home.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.home.ui.progress.model.WorkoutChartUi
import com.calai.bitecal.ui.home.ui.progress.model.WorkoutProgressDayUi
import com.calai.bitecal.ui.home.ui.progress.tooltip.ChartTooltipCard
import com.calai.bitecal.ui.home.ui.progress.tooltip.ChartTooltipMetricUi
import com.calai.bitecal.ui.home.ui.progress.tooltip.ChartTooltipPressState
import com.calai.bitecal.ui.home.ui.progress.tooltip.calculateChartTooltipOffsetPx
import com.calai.bitecal.ui.home.ui.progress.tooltip.chartTooltipPressTarget
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val WorkoutBarColor = Color(0xFFE9A35F)
private val WorkoutGoalLineColor = Color(0xFF3C9E45)
private val WorkoutCardBg = Color.White
private val WorkoutBorderColor = Color(0xFFD9D9DB)
private val WorkoutTitleColor = Color(0xFF1B1B21)
private val WorkoutValueColor = Color(0xFF17171C)
private val WorkoutMetaColor = Color(0xFF74747A)
private val WorkoutMetricChipBg = Color(0xFFF8FAFC)
private val WorkoutMetricChipBorder = Color(0xFFE2E8F0)
private val WorkoutMetricChipLabelColor = Color(0xFF64748B)
private val WorkoutMetricChipValueColor = Color(0xFF0F172A)

@Composable
internal fun WorkoutChartCard(
    chart: WorkoutChartUi,
    modifier: Modifier = Modifier
) {
    val footerText = if (chart.reachedGoalToday) {
        stringResource(R.string.workout_chart_goal_reached)
    } else {
        stringResource(R.string.workout_chart_goal_left, chart.remainingKcal)
    }

    WorkoutChartCardFrame(
        title = stringResource(R.string.workout_chart_title),
        headlineValue = chart.todayBurnedKcal.toString(),
        unitText = stringResource(R.string.workout_chart_unit_kcal),
        deltaText = chart.deltaText,
        goalText = stringResource(R.string.workout_chart_goal),
        goalValue = stringResource(R.string.workout_chart_value_kcal, chart.goalKcal),
        avgText = stringResource(R.string.workout_chart_7day_avg),
        avgValue = stringResource(R.string.workout_chart_value_kcal, chart.averageKcal),
        footerText = footerText,
        footerBackground = if (chart.reachedGoalToday) Color(0xFFEAF5E8) else Color(0xFFFFF3E6),
        footerTextColor = if (chart.reachedGoalToday) Color(0xFF3C9E45) else WorkoutBarColor,
        modifier = modifier
    ) {
        WorkoutBarChart(
            days = chart.days,
            goalKcal = chart.goalKcal,
            showBars = true
        )
    }
}

@Composable
internal fun WorkoutLoadingCard(
    modifier: Modifier = Modifier
) {
    WorkoutChartCardFrame(
        title = stringResource(R.string.workout_chart_title),
        headlineValue = "--",
        unitText = stringResource(R.string.workout_chart_unit_kcal),
        deltaText = "--",
        goalText = stringResource(R.string.workout_chart_goal),
        goalValue = "--",
        avgText = stringResource(R.string.workout_chart_7day_avg),
        avgValue = "--",
        footerText = stringResource(R.string.workout_chart_loading),
        footerBackground = Color(0xFFF2F4F7),
        footerTextColor = Color(0xFF667085),
        modifier = modifier
    ) {
        WorkoutBarChart(
            days = emptyList(),
            goalKcal = 450,
            showBars = false
        )
    }
}

@Composable
internal fun WorkoutErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WorkoutCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, WorkoutBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        Text(
            text = stringResource(R.string.workout_chart_title),
            color = WorkoutTitleColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = Color(0xFF111114),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFF111114), RoundedCornerShape(999.dp))
                    .biteCalClickable { onRetry() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.progress_retry),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WorkoutChartCardFrame(
    title: String,
    headlineValue: String,
    unitText: String,
    deltaText: String,
    goalText: String,
    goalValue: String,
    avgText: String,
    avgValue: String,
    footerText: String,
    footerBackground: Color,
    footerTextColor: Color,
    modifier: Modifier = Modifier,
    chartContent: @Composable () -> Unit
) {
    val resolvedDeltaText = if (deltaText == "--") "--%" else deltaText
    val resolvedDeltaColor = when {
        resolvedDeltaText.startsWith("↑") -> Color(0xFFE56C6C)
        resolvedDeltaText.startsWith("↓") -> Color(0xFF329A3F)
        else -> Color(0xFF74747A)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(WorkoutCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, WorkoutBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        val metricChipWidth = 102.dp

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = WorkoutTitleColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = headlineValue,
                            color = WorkoutValueColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = unitText,
                            color = WorkoutMetaColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = resolvedDeltaText,
                            color = resolvedDeltaColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutMetricChip(
                        label = goalText,
                        value = goalValue,
                        accentColor = WorkoutGoalLineColor,
                        modifier = Modifier.width(metricChipWidth)
                    )

                    WorkoutMetricChip(
                        label = avgText,
                        value = avgValue,
                        accentColor = WorkoutBarColor,
                        modifier = Modifier.width(metricChipWidth)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            chartContent()

            Spacer(modifier = Modifier.height(18.dp))

            WorkoutLegendRow()

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(footerBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = footerText,
                    color = footerTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WorkoutMetricChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(WorkoutMetricChipBg, RoundedCornerShape(14.dp))
            .border(1.dp, WorkoutMetricChipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(26.dp)
                .background(accentColor.copy(alpha = 0.86f), RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = label,
                color = WorkoutMetricChipLabelColor,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                color = WorkoutMetricChipValueColor,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
@Composable
private fun WorkoutLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .background(WorkoutBarColor, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.workout_legend_burned),
                color = WorkoutValueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .width(18.dp)
                    .height(10.dp)
            ) {
                drawLine(
                    color = WorkoutGoalLineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.workout_legend_daily_goal),
                color = WorkoutValueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WorkoutBarChart(
    days: List<WorkoutProgressDayUi>,
    goalKcal: Int,
    showBars: Boolean
) {
    val chartDays = remember(days) {
        if (days.size == 7) days else emptyList()
    }

    val maxValue = maxOf(
        goalKcal.toFloat(),
        chartDays.maxOfOrNull { it.kcal }?.toFloat() ?: 0f
    )

    val yAxisMax = computeWorkoutAxisMax(maxValue)
    val yTicks = buildWorkoutYAxisTicks(yAxisMax, 4)

    val chartAreaHeight = 184.dp
    val chartRowHeight = 184.dp
    val xAxisGap = 8.dp
    val yAxisWidth = 36.dp
    val yAxisToChartGap = 2.dp
    val plotTopInset = 10.dp
    val plotBottomInset = 0.dp
    val yLabelHalfHeight = 9.dp
    val plotHeightDp = chartAreaHeight - plotTopInset - plotBottomInset
    val plotEndPadding = 8.dp

    var pressedTooltip by remember(chartDays, showBars) {
        mutableStateOf<ChartTooltipPressState<WorkoutProgressDayUi>?>(null)
    }

    var chartSizePx by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartRowHeight),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .width(yAxisWidth)
                    .height(chartAreaHeight)
            ) {
                yTicks.asReversed().forEach { tick ->
                    val ratio = if (yAxisMax == 0f) 0f else tick / yAxisMax
                    val tickY = chartAreaHeight - plotBottomInset - (plotHeightDp * ratio)

                    Text(
                        text = tick.roundToInt().toString(),
                        color = ProgressChartAxisDefaults.IdleLabelColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(
                                x = (-8).dp,
                                y = tickY - yLabelHalfHeight
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(yAxisToChartGap))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(chartAreaHeight)
                    .padding(end = plotEndPadding)
                    .onSizeChanged { chartSizePx = it }
            ) {
                val density = LocalDensity.current
                var tooltipSizePx by remember { mutableStateOf(IntSize.Zero) }

                val tooltipMinWidth = 124.dp
                val slotCount = if (chartDays.isEmpty()) 7 else chartDays.size

                val chartWidthPx = chartSizePx.width.takeIf { it > 0 }?.toFloat() ?: 1f
                val chartHeightPx = chartSizePx.height.takeIf { it > 0 }?.toFloat() ?: 1f
                val slotWidthPx = chartWidthPx / slotCount.toFloat()

                val fallbackTooltipWidthPx = with(density) { tooltipMinWidth.toPx() }
                val fallbackTooltipHeightPx = with(density) { 86.dp.toPx() }

                val resolvedTooltipWidthPx = tooltipSizePx.width.takeIf { it > 0 }?.toFloat()
                    ?: fallbackTooltipWidthPx

                val resolvedTooltipHeightPx = tooltipSizePx.height.takeIf { it > 0 }?.toFloat()
                    ?: fallbackTooltipHeightPx

                val tooltipGapXPx = with(density) { 18.dp.toPx() }
                val tooltipFixedTopPx = with(density) { (-18).dp.toPx() }
                val tooltipEdgePaddingPx = with(density) { 4.dp.toPx() }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val strokeWidth = 2f

                    val plotTop = plotTopInset.toPx()
                    val plotBottom = size.height - plotBottomInset.toPx()
                    val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)

                    yTicks.forEach { tick ->
                        val ratio = if (yAxisMax == 0f) 0f else tick / yAxisMax
                        val y = plotBottom - (ratio * plotHeight)

                        drawLine(
                            color = ProgressChartAxisDefaults.GridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth,
                            pathEffect = dash
                        )
                    }

                    val goalRatio = if (yAxisMax == 0f) 0f else goalKcal / yAxisMax
                    val goalY = plotBottom - (goalRatio * plotHeight)

                    drawLine(
                        color = WorkoutGoalLineColor,
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartDays.forEachIndexed { index, day ->
                        val barPressModifier =
                            Modifier.chartTooltipPressTarget(
                                enabled = showBars,
                                index = index,
                                payload = day,
                                onTooltipChange = { pressedTooltip = it }
                            )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(barPressModifier),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(34.dp)
                                    .fillMaxHeight()
                            ) {
                                val plotTop = plotTopInset.toPx()
                                val plotBottom = size.height - plotBottomInset.toPx()
                                val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)

                                if (!showBars || day.kcal <= 0 || yAxisMax <= 0f) return@Canvas

                                val barWidth = size.width * 0.84f
                                val left = (size.width - barWidth) / 2f
                                val barHeight = (plotHeight * (day.kcal / yAxisMax)).coerceAtLeast(0f)
                                val top = plotBottom - barHeight

                                val resolvedRadius = minOf(
                                    8.dp.toPx(),
                                    barWidth / 2f,
                                    barHeight / 2f
                                )

                                val path = Path().apply {
                                    addRoundRect(
                                        RoundRect(
                                            left = left,
                                            top = top,
                                            right = left + barWidth,
                                            bottom = top + barHeight,
                                            topLeftCornerRadius = CornerRadius(resolvedRadius, resolvedRadius),
                                            topRightCornerRadius = CornerRadius(resolvedRadius, resolvedRadius),
                                            bottomRightCornerRadius = CornerRadius(0f, 0f),
                                            bottomLeftCornerRadius = CornerRadius(0f, 0f)
                                        )
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = WorkoutBarColor,
                                    style = Fill
                                )
                            }
                        }
                    }
                }

                pressedTooltip?.let { tooltip ->
                    val tooltipOffset = calculateChartTooltipOffsetPx(
                        chartWidthPx = chartWidthPx,
                        chartHeightPx = chartHeightPx,
                        slotWidthPx = slotWidthPx,
                        tooltipWidthPx = resolvedTooltipWidthPx,
                        tooltipHeightPx = resolvedTooltipHeightPx,
                        tooltipIndex = tooltip.index,
                        pressOffsetInSlotPx = tooltip.pressOffsetInSlotPx,
                        horizontalGapPx = tooltipGapXPx,
                        fixedTopPx = tooltipFixedTopPx,
                        edgePaddingPx = tooltipEdgePaddingPx
                    )

                    WorkoutDayTooltip(
                        day = tooltip.payload,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { tooltipOffset }
                            .onGloballyPositioned { coordinates ->
                                val newSize = coordinates.size
                                if (tooltipSizePx != newSize) {
                                    tooltipSizePx = newSize
                                }
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(xAxisGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = yAxisWidth + yAxisToChartGap,
                    end = plotEndPadding
                )
        ) {
            chartDays.forEach { day ->
                val isToday = ProgressChartAxisDefaults.isToday(
                    dateIso = day.date,
                    dayLabel = day.dayLabel
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedWorkoutDayLabel(day.dayLabel),
                        color = if (isToday) {
                            ProgressChartAxisDefaults.TodayLabelColor
                        } else {
                            ProgressChartAxisDefaults.IdleLabelColor
                        },
                        fontSize = 13.sp,
                        fontWeight = if (isToday) {
                            ProgressChartAxisDefaults.TodayLabelWeight
                        } else {
                            ProgressChartAxisDefaults.IdleLabelWeight
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
@Composable
private fun WorkoutDayTooltip(
    day: WorkoutProgressDayUi,
    modifier: Modifier = Modifier
) {
    val burnedLabel = stringResource(R.string.workout_tooltip_burned)

    ChartTooltipCard(
        metrics = listOf(
            ChartTooltipMetricUi(
                emoji = "🔥",
                label = stringResource(R.string.progress_tooltip_label_format, burnedLabel),
                value = stringResource(R.string.workout_tooltip_kcal_value, day.kcal)
            )
        ),
        dayLabel = localizedWorkoutDayLabel(day.dayLabel),
        modifier = modifier,
        maxWidth = 196.dp
    )
}

private fun computeWorkoutAxisMax(rawMax: Float): Float {
    if (rawMax <= 0f) return 500f

    val roughStep = rawMax / 4f
    val magnitude = 10.0.pow(floor(log10(roughStep.toDouble()))).toFloat()
    val residual = roughStep / magnitude

    val niceStep = when {
        residual <= 1f -> 1f
        residual <= 2f -> 2f
        residual <= 5f -> 5f
        else -> 10f
    } * magnitude

    return ceil(rawMax / niceStep).toFloat() * niceStep
}

private fun buildWorkoutYAxisTicks(max: Float, segments: Int): List<Float> {
    return (0..segments).map { index ->
        max * index / segments.toFloat()
    }
}

@Composable
private fun localizedWorkoutDayLabel(label: String): String {
    return when (label.take(3)) {
        "Sun" -> stringResource(R.string.progress_day_sun)
        "Mon" -> stringResource(R.string.progress_day_mon)
        "Tue" -> stringResource(R.string.progress_day_tue)
        "Wed" -> stringResource(R.string.progress_day_wed)
        "Thu" -> stringResource(R.string.progress_day_thu)
        "Fri" -> stringResource(R.string.progress_day_fri)
        "Sat" -> stringResource(R.string.progress_day_sat)
        else -> label
    }
}
