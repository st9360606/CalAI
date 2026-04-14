package com.calai.bitecal.ui.home.ui.progress

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.ui.progress.model.WaterChartUi
import com.calai.bitecal.ui.home.ui.progress.model.WaterProgressDayUi
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
private val WaterBarColor = Color(0xFF6C93D8)
private val WaterGoalLineColor = Color(0xFF49B35D)

private val WaterCardBg = Color.White
private val WaterBorderColor = Color(0xFFD9D9DB)
private val WaterGridColor = Color(0xFFBDBDBD)
private val WaterXAxisIdleColor = Color(0xFF8A8A8E)
private val WaterXAxisActiveColor = Color(0xFF666A73)

private val WaterTitleColor = Color(0xFF1B1B21)
private val WaterValueColor = Color(0xFF17171C)
private val WaterMetaColor = Color(0xFF74747A)

private val WaterMetricChipBg = Color(0xFFF7F9FC)
private val WaterMetricChipBorder = Color(0xFFE6EBF2)
private val WaterMetricChipLabelColor = Color(0xFF7F8794)
private val WaterMetricChipValueColor = Color(0xFF364152)

private val WaterFooterReachedBg = Color(0xFFEAF5E8)
private val WaterFooterReachedText = Color(0xFF3C9E45)
private val WaterFooterPendingBg = Color(0xFFEAF3FF)
private val WaterFooterPendingText = Color(0xFF3274D9)

private val WaterRetryBg = Color(0xFF111114)

@Composable
internal fun WaterChartCard(
    chart: WaterChartUi,
    modifier: Modifier = Modifier
) {
    val footerText = if (chart.reachedGoalToday) {
        "Goal reached for today"
    } else {
        "${formatMlPlain(chart.remainingMl)} ml left to reach today's goal"
    }

    WaterChartCardFrame(
        title = "Water intake",
        headlineValue = formatMlPlain(chart.todayMl),
        unitText = "ml",
        deltaText = chart.deltaText,
        goalText = "Goal",
        goalValue = "${formatMlPlain(chart.goalMl)} ml",
        avgText = "7-day avg",
        avgValue = "${formatMlPlain(chart.averageMl)} ml",
        footerText = footerText,
        footerBackground = if (chart.reachedGoalToday) WaterFooterReachedBg else WaterFooterPendingBg,
        footerTextColor = if (chart.reachedGoalToday) WaterFooterReachedText else WaterFooterPendingText,
        modifier = modifier
    ) {
        WaterBarChart(
            days = chart.days,
            goalMl = chart.goalMl,
            showBars = true
        )
    }
}

@Composable
internal fun WaterLoadingCard(
    modifier: Modifier = Modifier
) {
    WaterChartCardFrame(
        title = "Water intake",
        headlineValue = "--",
        unitText = "ml",
        deltaText = "--",
        goalText = "Goal",
        goalValue = "--",
        avgText = "7-day avg",
        avgValue = "--",
        footerText = "Loading...",
        footerBackground = Color(0xFFF2F4F7),
        footerTextColor = Color(0xFF667085),
        modifier = modifier
    ) {
        WaterBarChart(
            days = emptyList(),
            goalMl = 2000,
            showBars = false
        )
    }
}

@Composable
internal fun WaterErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WaterCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, WaterBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        Text(
            text = "Water intake",
            color = WaterTitleColor,
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
                    .background(WaterRetryBg, RoundedCornerShape(999.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Retry",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun WaterChartCardFrame(
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
    val resolvedDeltaText = if (deltaText == "--") {
        "--%"
    } else {
        deltaText
    }

    val resolvedDeltaColor = resolveWaterDeltaColor(resolvedDeltaText)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(WaterCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, WaterBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = WaterTitleColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = headlineValue,
                            color = WaterValueColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = unitText,
                            color = WaterMetaColor,
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
                    WaterMetricChip(
                        label = goalText,
                        value = goalValue,
                        accentColor = WaterGoalLineColor,
                        backgroundColor = WaterMetricChipBg,
                        borderColor = WaterMetricChipBorder,
                        labelColor = WaterMetricChipLabelColor,
                        valueColor = WaterMetricChipValueColor,
                        dashedIndicator = true,
                        modifier = Modifier.width(metricChipWidth)
                    )

                    WaterMetricChip(
                        label = avgText,
                        value = avgValue,
                        accentColor = WaterBarColor,
                        backgroundColor = WaterMetricChipBg,
                        borderColor = WaterMetricChipBorder,
                        labelColor = WaterMetricChipLabelColor,
                        valueColor = WaterMetricChipValueColor,
                        dashedIndicator = false,
                        modifier = Modifier.width(metricChipWidth)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            chartContent()

            Spacer(modifier = Modifier.height(18.dp))

            WaterLegendRow()

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = footerBackground,
                        shape = RoundedCornerShape(12.dp)
                    )
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
private fun WaterMetricChip(
    label: String,
    value: String,
    accentColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    labelColor: Color,
    valueColor: Color,
    dashedIndicator: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(12.dp)
                .height(8.dp)
        ) {
            if (dashedIndicator) {
                drawLine(
                    color = accentColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            } else {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(0f, size.height * 0.2f),
                    size = Size(size.width, size.height * 0.6f),
                    cornerRadius = CornerRadius(999f, 999f),
                    style = Fill
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = label,
                color = labelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WaterLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .background(WaterBarColor, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Intake",
                color = WaterValueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .width(18.dp)
                    .height(10.dp)
            ) {
                drawLine(
                    color = WaterGoalLineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Daily goal",
                color = WaterValueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WaterBarChart(
    days: List<WaterProgressDayUi>,
    goalMl: Int,
    showBars: Boolean = true
) {
    val orderedLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dayMap = days.associateBy { it.dayLabel.take(3) }

    val chartDays = orderedLabels.map { label ->
        dayMap[label] ?: WaterProgressDayUi(
            date = "",
            dayLabel = label,
            ml = 0
        )
    }

    val rawMax = maxOf(
        goalMl.toFloat(),
        chartDays.maxOfOrNull { it.ml.toFloat() } ?: 0f
    )

    val yAxisMax = computeWaterAxisMax(rawMax)
    val yTicks = buildYAxisTicks(yAxisMax, segments = 4)

    // ✅ 跟 ChartCard 完全對齊
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

    val animProgressList = remember {
        List(7) { Animatable(0f) }
    }

    LaunchedEffect(chartDays.map { it.ml }, showBars, goalMl) {
        chartDays.forEachIndexed { index, day ->
            launch {
                val target = if (!showBars || yAxisMax <= 0f || day.ml <= 0) {
                    0f
                } else {
                    (day.ml / yAxisMax).coerceIn(0f, 1f)
                }

                animProgressList[index].animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 320,
                        delayMillis = index * 24,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                        color = WaterXAxisIdleColor,
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
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val strokeWidth = 2f

                    val plotTop = plotTopInset.toPx()
                    val plotBottom = size.height - plotBottomInset.toPx()
                    val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)

                    yTicks.forEach { tick ->
                        val ratio = if (yAxisMax == 0f) 0f else tick / yAxisMax
                        val y = plotBottom - (ratio * plotHeight)

                        drawLine(
                            color = WaterGridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth,
                            pathEffect = dash
                        )
                    }

                    val goalRatio = if (yAxisMax == 0f) 0f else goalMl / yAxisMax
                    val goalY = plotBottom - (goalRatio * plotHeight)

                    drawLine(
                        color = WaterGoalLineColor,
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
                        val animatedProgress = animProgressList[index].value
                        val hasData = day.ml > 0

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

                                if (!showBars) return@Canvas
                                if (!hasData || animatedProgress <= 0f || yAxisMax <= 0f) return@Canvas

                                // ✅ 跟 ChartCard 柱體寬度一致
                                val barWidth = size.width * 0.84f
                                val left = (size.width - barWidth) / 2f
                                val barHeight = (plotHeight * animatedProgress).coerceAtLeast(0f)
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
                                    color = WaterBarColor,
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
                val active = showBars && day.ml > 0

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedWaterDayLabel(day.dayLabel),
                        color = if (active) WaterXAxisActiveColor else WaterXAxisIdleColor,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun computeWaterAxisMax(rawMax: Float): Float {
    if (rawMax <= 0f) return 2000f

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

private fun buildYAxisTicks(
    max: Float,
    segments: Int
): List<Float> {
    return (0..segments).map { index ->
        max * index / segments.toFloat()
    }
}

private fun formatMlPlain(value: Int): String {
    return value.toString()
}

@Composable
private fun localizedWaterDayLabel(label: String): String {
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

private fun resolveWaterDeltaColor(resolvedDeltaText: String): Color {
    val normalized = resolvedDeltaText.trim()

    return when {
        normalized.startsWith("↑") -> Color(0xFFE56C6C)
        normalized.startsWith("↓") -> Color(0xFF329A3F)
        else -> Color(0xFF74747A)
    }
}
