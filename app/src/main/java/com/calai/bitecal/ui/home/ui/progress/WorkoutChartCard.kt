package com.calai.bitecal.ui.home.ui.progress

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.ui.progress.model.WorkoutChartUi
import com.calai.bitecal.ui.home.ui.progress.model.WorkoutProgressDayUi
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val WorkoutBarColor = Color(0xFFFF8A50)
private val WorkoutGoalLineColor = Color(0xFF49B35D)
private val WorkoutCardBg = Color.White
private val WorkoutBorderColor = Color(0xFFD9D9DB)
private val WorkoutGridColor = Color(0xFFBDBDBD)
private val WorkoutAxisIdleColor = Color(0xFF8A8A8E)
private val WorkoutAxisActiveColor = Color(0xFF666A73)
private val WorkoutTitleColor = Color(0xFF1B1B21)
private val WorkoutValueColor = Color(0xFF17171C)
private val WorkoutMetaColor = Color(0xFF74747A)
private val WorkoutMetricChipBg = Color(0xFFF7F9FC)
private val WorkoutMetricChipBorder = Color(0xFFE6EBF2)
private val WorkoutMetricChipLabelColor = Color(0xFF7F8794)
private val WorkoutMetricChipValueColor = Color(0xFF364152)

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
        goalValue = "${chart.goalKcal} kcal",
        avgText = stringResource(R.string.workout_chart_7day_avg),
        avgValue = "${chart.averageKcal} kcal",
        footerText = footerText,
        footerBackground = if (chart.reachedGoalToday) Color(0xFFEAF5E8) else Color(0xFFFFF3E8),
        footerTextColor = if (chart.reachedGoalToday) Color(0xFF3C9E45) else Color(0xFFD97706),
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
                    .clickable { onRetry() }
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

@SuppressLint("UnusedBoxWithConstraintsScope")
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(WorkoutCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, WorkoutBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        // 跟 WaterChartCardFrame 完全一致
        val metricChipWidth = (maxWidth * 0.30f).coerceIn(88.dp, 102.dp)

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
                        dashed = true,
                        modifier = Modifier.width(metricChipWidth)
                    )

                    WorkoutMetricChip(
                        label = avgText,
                        value = avgValue,
                        dashed = false,
                        modifier = Modifier.width(metricChipWidth)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            chartContent()

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(footerBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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
    dashed: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(WorkoutMetricChipBg, RoundedCornerShape(14.dp))
            .border(1.dp, WorkoutMetricChipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(width = 12.dp, height = 8.dp)) {
            if (dashed) {
                drawLine(
                    color = WorkoutGoalLineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            } else {
                drawLine(
                    color = WorkoutBarColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 4f
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = label,
                color = WorkoutMetricChipLabelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = WorkoutMetricChipValueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
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

    // 跟 WaterBarChart 完全對齊
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
                        color = WorkoutAxisIdleColor,
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
            ) {
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
                            color = WorkoutGridColor,
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
                    chartDays.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
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
                val active = showBars && day.kcal > 0
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedWorkoutDayLabel(day.dayLabel),
                        color = if (active) WorkoutAxisActiveColor else WorkoutAxisIdleColor,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
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
