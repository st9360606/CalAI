package com.calai.bitecal.ui.home.ui.progress

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.ui.progress.model.ProgressBarDayUi
import com.calai.bitecal.ui.home.ui.progress.model.ProgressViewModel
import com.calai.bitecal.ui.home.ui.weight.components.WeightTopBar
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

private val ProteinColor = Color(0xFFE56C6C)
private val CarbsColor = Color(0xFFD89A62)
private val FatsColor = Color(0xFF6C93D8)
private val ProgressBg = Color(0xFFF5F5F5)
private val CardBg = Color.White
private val BorderColor = Color(0xFFE7E7E7)
private val ChipSelected = Color(0xFF111114)





private val WeekTabsContainerBg = Color(0xFFF1F1F3)
private val WeekTabsActiveBg = Color(0xFFFFFFFF)
private val WeekTabsActiveBorder = Color(0xFFE2E2E6)
private val WeekTabsActiveText = Color(0xFF2D2F35)
private val WeekTabsIdleText = Color(0xFF3A3D43)



private val ChartGridColor = Color(0xFFBDBDBD)
private val ChartXAxisIdleColor = Color(0xFF8A8A8E)
private val ChartXAxisActiveColor = Color(0xFF666A73)
private val ChartZeroDotColor = Color(0xFFD8DADF)

@Composable
fun ProgressScreen(
    vm: ProgressViewModel,
    onBack: () -> Unit,
    onOpenTab: (HomeTab) -> Unit
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.loadIfNeeded() }
    BackHandler { onBack() }

    Scaffold(
        containerColor = ProgressBg,
        topBar = {
            WeightTopBar(
                title = "Progress",
                onBack = onBack
            )
        },
        bottomBar = {
            MainBottomBar(current = HomeTab.Progress, onOpenTab = onOpenTab)
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ProgressBg)
                .padding(inner),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WeekTabs(
                    selected = ui.selectedWeekOffset,
                    onSelect = vm::selectWeek,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when {
                ui.loading -> {
                    item {
                        LoadingCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                ui.error != null -> {
                    item {
                        ErrorCard(
                            message = ui.error ?: "Load failed",
                            onRetry = vm::retry,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                ui.isEmpty -> {
                    item {
                        EmptyCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                else -> {
                    item {
                        ChartCard(
                            totalCaloriesText = ui.totalCaloriesText,
                            deltaText = ui.deltaText,
                            deltaDirection = ui.deltaDirection,
                            days = ui.days,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(5.dp)) }
        }
    }
}

@Composable
private fun WeekTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("This Week", "Last Week", "2 wks. ago", "3 wks. ago")
    val containerShape = RoundedCornerShape(18.dp)
    val activeTabShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WeekTabsContainerBg, containerShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(
                        if (active) {
                            Modifier
                                .shadow(
                                    elevation = 1.5.dp,
                                    shape = activeTabShape,
                                    clip = false
                                )
                                .background(WeekTabsActiveBg, activeTabShape)
                                .border(1.dp, WeekTabsActiveBorder, activeTabShape)
                        } else {
                            Modifier.background(Color.Transparent, activeTabShape)
                        }
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (active) WeekTabsActiveText else WeekTabsIdleText,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    totalCaloriesText: String,
    deltaText: String,
    deltaDirection: String,
    days: List<ProgressBarDayUi>,
    modifier: Modifier = Modifier
) {
    ProgressChartCardFrame(
        totalCaloriesText = totalCaloriesText,
        deltaText = deltaText,
        deltaDirection = deltaDirection,
        modifier = modifier
    ) {
        StackedBarChart(
            days = days,
            showBars = true,
            showZeroDots = false
        )
    }
}

@Composable
private fun StackedBarChart(
    days: List<ProgressBarDayUi>,
    showBars: Boolean = true,
    showZeroDots: Boolean = false
) {
    val orderedLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dayMap = days.associateBy { it.dayLabel.take(3) }

    val chartDays = orderedLabels.map { label ->
        dayMap[label] ?: ProgressBarDayUi(
            date = "",
            dayLabel = label,
            proteinG = 0f,
            carbsG = 0f,
            fatsG = 0f,
            totalG = 0f,
            totalKcal = 0
        )
    }

    val rawMax = chartDays.maxOfOrNull { it.totalG.toDouble() }?.toFloat() ?: 0f
    val yAxisMax = computeNiceAxisMax(rawMax)
    val yTicks = buildYAxisTicks(yAxisMax, segments = 4)

    val chartAreaHeight = 184.dp
    val chartRowHeight = 184.dp
    val xAxisGap = 10.dp
    val yAxisWidth = 36.dp
    val yAxisToChartGap = 2.dp
    val plotVerticalInset = 10.dp
    val yLabelHalfHeight = 9.dp
    val plotHeightDp = chartAreaHeight - (plotVerticalInset * 2)
    val plotEndPadding = 8.dp

    val animProgressList = remember {
        List(7) { Animatable(0f) }
    }

    LaunchedEffect(chartDays.map { it.totalG }, showBars) {
        chartDays.forEachIndexed { index, day ->
            launch {
                val target = if (!showBars || yAxisMax <= 0f || day.totalG <= 0f) {
                    0f
                } else {
                    (day.totalG / yAxisMax).coerceIn(0f, 1f)
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
                    val tickY = chartAreaHeight - plotVerticalInset - (plotHeightDp * ratio)

                    Text(
                        text = tick.roundToInt().toString(),
                        color = ChartXAxisIdleColor,
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

                    val plotTop = plotVerticalInset.toPx()
                    val plotBottom = size.height - plotVerticalInset.toPx()
                    val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)

                    yTicks.forEach { tick ->
                        val ratio = if (yAxisMax == 0f) 0f else tick / yAxisMax
                        val y = plotBottom - (ratio * plotHeight)

                        drawLine(
                            color = ChartGridColor,
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
                        val hasData = day.totalG > 0f
                        val animatedProgress = animProgressList[index].value

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
                                val plotTop = plotVerticalInset.toPx()
                                val plotBottom = size.height - plotVerticalInset.toPx()
                                val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)

                                if (!showBars) return@Canvas
                                if (!hasData || yAxisMax <= 0f || animatedProgress <= 0f) return@Canvas

                                val barWidth = size.width * 0.84f
                                val left = (size.width - barWidth) / 2f
                                val totalHeight = (plotHeight * animatedProgress).coerceAtLeast(0f)
                                var cursorBottom = plotBottom

                                fun drawSegment(
                                    value: Float,
                                    color: Color,
                                    topCornerRadiusPx: Float = 0f,
                                    topOnlyRounded: Boolean = false
                                ) {
                                    if (value <= 0f || day.totalG <= 0f || totalHeight <= 0f) return

                                    val segmentHeight = (value / day.totalG) * totalHeight
                                    val top = cursorBottom - segmentHeight

                                    if (topOnlyRounded && topCornerRadiusPx > 0f) {
                                        val resolvedRadius = minOf(
                                            topCornerRadiusPx,
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

                                drawSegment(day.fatsG, FatsColor)
                                drawSegment(day.carbsG, CarbsColor)
                                drawSegment(
                                    value = day.proteinG,
                                    color = ProteinColor,
                                    topCornerRadiusPx = 6.dp.toPx(),
                                    topOnlyRounded = day.proteinG > 0f
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
                val active = showBars && day.totalG > 0f

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayLabel,
                        color = if (active) ChartXAxisActiveColor else ChartXAxisIdleColor,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun computeNiceAxisMax(rawMax: Float): Float {
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

private fun buildYAxisTicks(
    max: Float,
    segments: Int
): List<Float> {
    return (0..segments).map { index ->
        max * index / segments.toFloat()
    }
}

@Composable
private fun LoadingCard(
    modifier: Modifier = Modifier
) {
    ProgressChartCardFrame(
        totalCaloriesText = "--.- cals",
        deltaText = "--",
        deltaDirection = "UP",
        deltaDisplayText = "↑ --%",
        deltaColorOverride = Color(0xFF33A144),
        modifier = modifier
    ) {
        LoadingChartPlaceholder()
    }
}

@Composable
private fun LegendChip(
    label: String,
    emoji: String,
    emojiFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    emojiYOffset: Dp = 0.dp,
    textYOffset: Dp = 0.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
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

@Composable
private fun LoadingChartPlaceholder() {
    val yLabels = listOf("8", "6", "4", "2", "0")
    val xLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .height(200.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        color = Color(0xFF8A8A8E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val lineColor = Color(0xFFBDBDBD)
                    val strokeWidth = 2f

                    for (i in 0 until 5) {
                        val y = size.height * i / 4f
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth,
                            pathEffect = dash
                        )
                    }

                    val unitsMax = 8f
                    val monX = size.width * 0.22f
                    val barWidth = 42f

                    fun drawSegment(
                        fromUnit: Float,
                        toUnit: Float,
                        color: Color,
                        roundTop: Boolean
                    ) {
                        val bottomY = size.height - (fromUnit / unitsMax) * size.height
                        val topY = size.height - (toUnit / unitsMax) * size.height
                        val height = bottomY - topY

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(monX, topY),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(
                                x = if (roundTop) 10f else 0f,
                                y = if (roundTop) 10f else 0f
                            ),
                            style = Fill
                        )
                    }

                    drawSegment(
                        fromUnit = 0f,
                        toUnit = 2f,
                        color = Color(0xFF6C93D8),
                        roundTop = false
                    )
                    drawSegment(
                        fromUnit = 2f,
                        toUnit = 6f,
                        color = Color(0xFFD89A62),
                        roundTop = false
                    )
                    drawSegment(
                        fromUnit = 6f,
                        toUnit = 8f,
                        color = Color(0xFFE56C6C),
                        roundTop = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            xLabels.forEach { label ->
                Text(
                    text = label,
                    color = Color(0xFF8A8A8E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(
    modifier: Modifier = Modifier
) {
    ProgressChartCardFrame(
        totalCaloriesText = "0.0 cals",
        deltaText = "--",
        deltaDirection = "NONE",
        modifier = modifier
    ) {
        StackedBarChart(
            days = emptyList(),
            showBars = false,
            showZeroDots = false
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(CardBg, RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Color(0xFF111114), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .background(ChipSelected, RoundedCornerShape(999.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(text = "Retry", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProgressChartCardFrame(
    totalCaloriesText: String,
    deltaText: String,
    deltaDirection: String,
    modifier: Modifier = Modifier,
    deltaDisplayText: String? = null,
    deltaColorOverride: Color? = null,
    chartContent: @Composable () -> Unit
) {
    val valueText = totalCaloriesText.removeSuffix(" cals").trim()

    val resolvedDeltaText = deltaDisplayText ?: if (deltaText == "--") {
        "--%"
    } else {
        deltaText
    }

    val resolvedDeltaColor = deltaColorOverride ?: when (deltaDirection) {
        "UP" -> Color(0xFF329A3F)
        "DOWN" -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFFD9D9DB), RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        Text(
            text = "Total calories",
            color = Color(0xFF1B1B21),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = valueText,
                color = Color(0xFF17171C),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "cals",
                color = Color(0xFF74747A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = resolvedDeltaText,
                color = resolvedDeltaColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        chartContent()

        Spacer(modifier = Modifier.height(25.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendChip(
                label = "Protein",
                emoji = "🥩",
                emojiFontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(24.dp))

            LegendChip(
                label = "Carbs",
                emoji = "🌾",
                emojiFontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(24.dp))

            LegendChip(
                label = "Fats",
                emoji = "🥑",
                emojiFontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .offset(x = 8.dp) // 這裡用 offset 往右挪一點點，最不會破壞置中的感覺
                .background(
                    color = Color(0xFFEAF5E8),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Keep it up! Getting started is the hardest part!",
                color = Color(0xFF3C9E45 ), // 套用上次說的深一點點的綠
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
