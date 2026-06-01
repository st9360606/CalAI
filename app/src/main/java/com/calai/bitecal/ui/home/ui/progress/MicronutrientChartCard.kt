package com.calai.bitecal.ui.home.ui.progress

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.ui.progress.model.ProgressBarDayUi
import com.calai.bitecal.ui.home.ui.progress.tooltip.ChartTooltipCard
import com.calai.bitecal.ui.home.ui.progress.tooltip.ChartTooltipMetricUi
import com.calai.bitecal.ui.home.ui.progress.tooltip.ChartTooltipPressState
import com.calai.bitecal.ui.home.ui.progress.tooltip.calculateChartTooltipOffsetPx
import com.calai.bitecal.ui.home.ui.progress.tooltip.chartTooltipPressTarget
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val MicronutrientFiberColor = Color(0xFFA78BFA)
private val MicronutrientSugarColor = Color(0xFFF08AAF)
private val MicronutrientSodiumColor = Color(0xFF73B6E6)

private val MicronutrientCardBg = Color.White
private val MicronutrientBorderColor = Color(0xFFD9D9DB)
private val MicronutrientTitleColor = Color(0xFF1B1B21)
private val MicronutrientValueColor = Color(0xFF17171C)
private val MicronutrientMetaColor = Color(0xFF74747A)
private val MicronutrientFooterBg = Color(0xFFF6F0FF)
private val MicronutrientFooterText = Color(0xFFA37FE0)
private val MicronutrientAverageChipBg = Color(0xFFF8FAFC)
private val MicronutrientAverageChipBorder = Color(0xFFE2E8F0)
private val MicronutrientAverageChipLabel = Color(0xFF64748B)
private val MicronutrientAverageChipValue = Color(0xFF0F172A)

@Composable
internal fun MicronutrientChartCard(
    days: List<ProgressBarDayUi>,
    weekOffset: Int = 0,
    average7FiberG: Int,
    average7SugarG: Int,
    average7SodiumMg: Int,
    modifier: Modifier = Modifier
) {
    val chartDays = normalizeMicronutrientWeekDays(days)
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val displayDay = if (weekOffset == 0) {
        chartDays.firstOrNull { it.date == today.toString() }
            ?: chartDays.lastOrNull { day ->
                parseMicronutrientDateOrNull(day.date)?.let { !it.isAfter(today) } == true
            }
            ?: emptyMicronutrientDayUi("Sat")
    } else {
        chartDays.getOrNull(6) ?: emptyMicronutrientDayUi("Sat")
    }

    val compareDay = if (weekOffset == 0) {
        chartDays.firstOrNull { it.date == yesterday.toString() }
    } else {
        chartDays.getOrNull(5)
    }
    val deltaText = calculateMicronutrientDeltaPercent(
        todayValue = displayDay.micronutrientTotalG(),
        yesterdayValue = compareDay?.micronutrientTotalG() ?: 0f
    ).toMicronutrientDeltaText()

    MicronutrientChartCardFrame(
        title = stringResource(R.string.progress_micronutrients_title),
        headlineValue = formatMicronutrientGramPlain(displayDay.micronutrientTotalG()),
        unitText = stringResource(R.string.progress_chart_grams),
        deltaText = deltaText,
        average7FiberG = average7FiberG,
        average7SugarG = average7SugarG,
        average7SodiumMg = average7SodiumMg,
        modifier = modifier
    ) {
        MicronutrientStackedBarChart(days = chartDays, showBars = true)
    }
}

@Composable
private fun MicronutrientChartCardFrame(
    title: String,
    headlineValue: String,
    unitText: String,
    deltaText: String,
    average7FiberG: Int,
    average7SugarG: Int,
    average7SodiumMg: Int,
    modifier: Modifier = Modifier,
    chartContent: @Composable () -> Unit
) {
    val resolvedDeltaText = if (deltaText == "--") {
        stringResource(R.string.progress_chart_delta_placeholder)
    } else {
        deltaText
    }
    val resolvedDeltaColor = resolveMicronutrientDeltaColor(resolvedDeltaText)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MicronutrientCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, MicronutrientBorderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 22.dp, vertical = 26.dp)
    ) {
        val averageChipWidth = 122.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = MicronutrientTitleColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = headlineValue,
                            color = MicronutrientValueColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = unitText,
                            color = MicronutrientMetaColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = resolvedDeltaText,
                            color = resolvedDeltaColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                MicronutrientAverageChip(
                    title = stringResource(R.string.progress_chart_7day_avg),
                    fiberLabel = stringResource(R.string.progress_legend_fiber),
                    sugarLabel = stringResource(R.string.progress_legend_sugar),
                    sodiumLabel = stringResource(R.string.progress_legend_sodium),
                    fiberValue = stringResource(R.string.progress_tooltip_grams_value, average7FiberG),
                    sugarValue = stringResource(R.string.progress_tooltip_grams_value, average7SugarG),
                    sodiumValue = stringResource(R.string.progress_tooltip_mg_value, average7SodiumMg),
                    modifier = Modifier.width(averageChipWidth)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            chartContent()

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ColorLegendChip(
                    label = stringResource(R.string.progress_legend_fiber),
                    emoji = "🌿",
                    emojiFontSize = 13.sp
                )

                Spacer(modifier = Modifier.width(24.dp))

                ColorLegendChip(
                    label = stringResource(R.string.progress_legend_sugar),
                    emoji = "🍯",
                    emojiFontSize = 15.sp
                )

                Spacer(modifier = Modifier.width(24.dp))

                ColorLegendChip(
                    label = stringResource(R.string.progress_legend_sodium),
                    emoji = "🍚",
                    emojiFontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset(x = 8.dp)
                    .background(
                        color = MicronutrientFooterBg,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.progress_micronutrients_keep_it_up),
                    color = MicronutrientFooterText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MicronutrientAverageChip(
    title: String,
    fiberLabel: String,
    sugarLabel: String,
    sodiumLabel: String,
    fiberValue: String,
    sugarValue: String,
    sodiumValue: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MicronutrientAverageChipBg, RoundedCornerShape(14.dp))
            .border(1.dp, MicronutrientAverageChipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = MicronutrientAverageChipLabel,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        MicronutrientAverageMetricRow(
            color = MicronutrientFiberColor,
            label = fiberLabel,
            value = fiberValue
        )

        Spacer(modifier = Modifier.height(4.dp))

        MicronutrientAverageMetricRow(
            color = MicronutrientSugarColor,
            label = sugarLabel,
            value = sugarValue
        )

        Spacer(modifier = Modifier.height(4.dp))

        MicronutrientAverageMetricRow(
            color = MicronutrientSodiumColor,
            label = sodiumLabel,
            value = sodiumValue
        )
    }
}

@Composable
private fun MicronutrientAverageMetricRow(
    color: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(color.copy(alpha = 0.86f), RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = MicronutrientAverageChipLabel,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = value,
            color = MicronutrientAverageChipValue,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun MicronutrientStackedBarChart(
    days: List<ProgressBarDayUi>,
    showBars: Boolean = true
) {
    val chartDays = normalizeMicronutrientWeekDays(days)
    val rawMax = chartDays.maxOfOrNull { it.micronutrientTotalG().toDouble() }?.toFloat() ?: 0f
    val yAxisMax = computeMicronutrientNiceAxisMax(rawMax)
    val yTicks = buildMicronutrientYAxisTicks(yAxisMax, segments = 4)

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

    var pressedTooltip by remember(chartDays, showBars) {
        mutableStateOf<ChartTooltipPressState<ProgressBarDayUi>?>(null)
    }

    LaunchedEffect(chartDays.map { it.micronutrientTotalG() }, showBars) {
        pressedTooltip = null

        chartDays.forEachIndexed { index, day ->
            launch {
                val target = if (!showBars || yAxisMax <= 0f || day.micronutrientTotalG() <= 0f) {
                    0f
                } else {
                    (day.micronutrientTotalG() / yAxisMax).coerceIn(0f, 1f)
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
                        text = formatMicronutrientAxisTick(tick),
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

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(chartAreaHeight)
                    .padding(end = plotEndPadding)
            ) {
                val density = LocalDensity.current
                var tooltipSizePx by remember { mutableStateOf(IntSize.Zero) }

                val tooltipMinWidth = 124.dp
                val chartWidthPx = with(density) { maxWidth.toPx() }
                val chartHeightPx = with(density) { maxHeight.toPx() }
                val slotWidthPx = chartWidthPx / chartDays.size.toFloat()
                val fallbackTooltipWidthPx = with(density) { tooltipMinWidth.toPx() }
                val fallbackTooltipHeightPx = with(density) { 108.dp.toPx() }
                val resolvedTooltipWidthPx = tooltipSizePx.width.takeIf { it > 0 }?.toFloat()
                    ?: fallbackTooltipWidthPx
                val resolvedTooltipHeightPx = tooltipSizePx.height.takeIf { it > 0 }?.toFloat()
                    ?: fallbackTooltipHeightPx
                val tooltipGapXPx = with(density) { 18.dp.toPx() }
                val tooltipFixedTopPx = with(density) { (-92).dp.toPx() }
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
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartDays.forEachIndexed { index, day ->
                        val totalG = day.micronutrientTotalG()
                        val hasData = totalG > 0f
                        val animatedProgress = animProgressList[index].value

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .chartTooltipPressTarget(
                                    enabled = showBars,
                                    index = index,
                                    payload = day,
                                    onTooltipChange = { pressedTooltip = it }
                                ),
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
                                if (!hasData || yAxisMax <= 0f || animatedProgress <= 0f) return@Canvas

                                val barWidth = size.width * 0.84f
                                val left = (size.width - barWidth) / 2f
                                val totalHeight = (plotHeight * animatedProgress).coerceAtLeast(0f)
                                var cursorBottom = plotBottom

                                val segments = listOf(
                                    day.sodiumG() to MicronutrientSodiumColor,
                                    day.sugarG to MicronutrientSugarColor,
                                    day.fiberG to MicronutrientFiberColor
                                )
                                val lastPositiveIndex = segments.indexOfLast { it.first > 0f }

                                fun drawSegment(
                                    value: Float,
                                    color: Color,
                                    roundTop: Boolean
                                ) {
                                    if (value <= 0f || totalG <= 0f || totalHeight <= 0f) return

                                    val segmentHeight = (value / totalG) * totalHeight
                                    val top = cursorBottom - segmentHeight

                                    if (roundTop) {
                                        val resolvedRadius = minOf(
                                            8.dp.toPx(),
                                            barWidth / 2f,
                                            segmentHeight / 2f
                                        )
                                        val path = Path().apply {
                                            addRoundRect(
                                                RoundRect(
                                                    left = left,
                                                    top = top,
                                                    right = left + barWidth,
                                                    bottom = top + segmentHeight,
                                                    topLeftCornerRadius = CornerRadius(resolvedRadius, resolvedRadius),
                                                    topRightCornerRadius = CornerRadius(resolvedRadius, resolvedRadius),
                                                    bottomRightCornerRadius = CornerRadius(0f, 0f),
                                                    bottomLeftCornerRadius = CornerRadius(0f, 0f)
                                                )
                                            )
                                        }
                                        drawPath(path = path, color = color, style = Fill)
                                    } else {
                                        drawRect(
                                            color = color,
                                            topLeft = Offset(left, top),
                                            size = Size(barWidth, segmentHeight)
                                        )
                                    }

                                    cursorBottom = top
                                }

                                segments.forEachIndexed { segmentIndex, segment ->
                                    drawSegment(
                                        value = segment.first,
                                        color = segment.second,
                                        roundTop = segmentIndex == lastPositiveIndex
                                    )
                                }
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

                    MicronutrientDayTooltip(
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
                        text = localizedMicronutrientDayLabel(day.dayLabel),
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
private fun MicronutrientDayTooltip(
    day: ProgressBarDayUi,
    modifier: Modifier = Modifier
) {
    val fiberLabel = stringResource(R.string.progress_tooltip_fiber)
    val sugarLabel = stringResource(R.string.progress_tooltip_sugar)
    val sodiumLabel = stringResource(R.string.progress_tooltip_sodium)

    ChartTooltipCard(
        metrics = listOf(
            ChartTooltipMetricUi(
                emoji = "🌿",
                label = stringResource(R.string.progress_tooltip_label_format, fiberLabel),
                value = stringResource(
                    R.string.progress_tooltip_grams_value,
                    day.fiberG.roundToInt()
                )
            ),
            ChartTooltipMetricUi(
                emoji = "🍯",
                label = stringResource(R.string.progress_tooltip_label_format, sugarLabel),
                value = stringResource(
                    R.string.progress_tooltip_grams_value,
                    day.sugarG.roundToInt()
                )
            ),
            ChartTooltipMetricUi(
                emoji = "🍚",
                label = stringResource(R.string.progress_tooltip_label_format, sodiumLabel),
                value = stringResource(
                    R.string.progress_tooltip_mg_value,
                    day.sodiumMg.roundToInt()
                )
            )
        ),
        dayLabel = localizedMicronutrientDayLabel(day.dayLabel),
        modifier = modifier,
        maxWidth = 228.dp
    )
}

@Composable
private fun ColorLegendChip(
    label: String,
    emoji: String,
    emojiFontSize: TextUnit = 16.sp,
    emojiYOffset: Dp = 0.dp,
    textYOffset: Dp = 0.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = emoji,
            fontSize = emojiFontSize,
            modifier = Modifier.offset(y = emojiYOffset)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = Color(0xFF17171C),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.offset(y = textYOffset)
        )
    }
}

private fun parseMicronutrientDateOrNull(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun normalizeMicronutrientWeekDays(days: List<ProgressBarDayUi>): List<ProgressBarDayUi> {
    val dayMap = days.associateBy { it.dayLabel.take(3) }
    return orderedMicronutrientWeekLabels.map { label ->
        dayMap[label] ?: emptyMicronutrientDayUi(label)
    }
}

private fun emptyMicronutrientDayUi(label: String): ProgressBarDayUi {
    return ProgressBarDayUi(
        date = "",
        dayLabel = label,
        proteinG = 0f,
        carbsG = 0f,
        fatsG = 0f,
        totalG = 0f,
        totalKcal = 0,
        fiberG = 0f,
        sugarG = 0f,
        sodiumMg = 0f
    )
}

private fun ProgressBarDayUi.sodiumG(): Float = sodiumMg / 1000f

private fun ProgressBarDayUi.micronutrientTotalG(): Float = fiberG + sugarG + sodiumG()

private fun calculateMicronutrientDeltaPercent(
    todayValue: Float,
    yesterdayValue: Float
): Double? {
    return when {
        todayValue <= 0f && yesterdayValue <= 0f -> 0.0
        yesterdayValue <= 0f -> 100.0
        else -> ((todayValue - yesterdayValue).toDouble() / yesterdayValue.toDouble()) * 100.0
    }
}

private fun Double?.toMicronutrientDeltaText(): String {
    if (this == null) return "--"

    val prefix = when {
        this > 0 -> "↑ "
        this < 0 -> "↓ "
        else -> ""
    }

    val rounded = String.format(Locale.getDefault(), "%.1f", abs(this))
    return "$prefix$rounded%"
}

private fun resolveMicronutrientDeltaColor(resolvedDeltaText: String): Color {
    val normalized = resolvedDeltaText.trim()
    return when {
        normalized.startsWith("↑") -> Color(0xFFE56C6C)
        normalized.startsWith("↓") -> Color(0xFF329A3F)
        else -> MicronutrientMetaColor
    }
}

private fun computeMicronutrientNiceAxisMax(rawMax: Float): Float {
    if (rawMax <= 0f) return 8f
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

private fun buildMicronutrientYAxisTicks(
    max: Float,
    segments: Int
): List<Float> {
    return (0..segments).map { index ->
        max * index / segments.toFloat()
    }
}

private fun formatMicronutrientAxisTick(value: Float): String {
    return if (value >= 10f || value % 1f == 0f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatMicronutrientGramPlain(value: Float): String {
    return String.format(Locale.getDefault(), "%.1f", value.coerceAtLeast(0f))
}

@Composable
private fun localizedMicronutrientDayLabel(label: String): String {
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

private val orderedMicronutrientWeekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

