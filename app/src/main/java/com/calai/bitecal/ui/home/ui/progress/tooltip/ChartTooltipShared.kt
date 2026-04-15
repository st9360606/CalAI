package com.calai.bitecal.ui.home.ui.progress.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

internal data class ChartTooltipMetricUi(
    val emoji: String,
    val label: String,
    val value: String
)

internal data class ChartTooltipPressState<T>(
    val index: Int,
    val payload: T,
    val pressOffsetInSlotPx: Offset
)

internal object ChartTooltipDefaults {
    val BorderColor = Color(0xFFE9E9ED)
    val LabelTextColor = Color(0xFF525866)
    val ValueTextColor = Color(0xFF333947)
    val DayLabelColor = Color(0xFF74747A)

    val EmojiSlotWidth = 18.dp
    val EmojiToLabelGap = 4.dp
    val LabelToValueGap = 1.dp
    val DayLabelStartPadding = EmojiSlotWidth + EmojiToLabelGap

    val MinWidth = 124.dp
    val MaxWidth = 228.dp
    val CornerRadius = 20.dp
    val ShadowElevation = 14.dp
    val HorizontalPadding = 11.dp
    val TopPadding = 11.dp
    val BottomPadding = 10.dp
    val RowSpacing = 4.dp
    val RowHeight = 20.dp
}

internal fun <T> Modifier.chartTooltipPressTarget(
    enabled: Boolean,
    index: Int,
    payload: T,
    onTooltipChange: (ChartTooltipPressState<T>?) -> Unit
): Modifier {
    if (!enabled) return this

    return this.pointerInput(index, payload, enabled) {
        detectTapGestures(
            onPress = { pressOffset ->
                onTooltipChange(
                    ChartTooltipPressState(
                        index = index,
                        payload = payload,
                        pressOffsetInSlotPx = pressOffset
                    )
                )

                try {
                    tryAwaitRelease()
                } finally {
                    onTooltipChange(null)
                }
            }
        )
    }
}

internal fun calculateChartTooltipOffsetPx(
    chartWidthPx: Float,
    chartHeightPx: Float,
    slotWidthPx: Float,
    tooltipWidthPx: Float,
    tooltipHeightPx: Float,
    tooltipIndex: Int,
    pressOffsetInSlotPx: Offset,
    horizontalGapPx: Float,
    fixedTopPx: Float,
    edgePaddingPx: Float
): IntOffset {
    val anchorX = (slotWidthPx * tooltipIndex) + pressOffsetInSlotPx.x

    var targetX = anchorX + horizontalGapPx

    val maxX = (chartWidthPx - tooltipWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
    val minY = fixedTopPx
    val maxY = (chartHeightPx - tooltipHeightPx - edgePaddingPx).coerceAtLeast(minY)

    val fitsRight = targetX + tooltipWidthPx <= chartWidthPx - edgePaddingPx
    if (!fitsRight) {
        targetX = anchorX - tooltipWidthPx - horizontalGapPx
    }

    targetX = targetX.coerceIn(edgePaddingPx, maxX)

    val targetY = fixedTopPx.coerceIn(minY, maxY)

    return IntOffset(
        x = targetX.roundToInt(),
        y = targetY.roundToInt()
    )
}

@Composable
internal fun ChartTooltipCard(
    metrics: List<ChartTooltipMetricUi>,
    dayLabel: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = ChartTooltipDefaults.MinWidth,
    maxWidth: Dp = ChartTooltipDefaults.MaxWidth,
    borderColor: Color = ChartTooltipDefaults.BorderColor,
    labelTextColor: Color = ChartTooltipDefaults.LabelTextColor,
    valueTextColor: Color = ChartTooltipDefaults.ValueTextColor,
    dayLabelColor: Color = ChartTooltipDefaults.DayLabelColor,
    dayLabelStartPadding: Dp = ChartTooltipDefaults.DayLabelStartPadding
) {
    Column(
        modifier = modifier
            .zIndex(2f)
            .width(IntrinsicSize.Max)
            .widthIn(min = minWidth, max = maxWidth)
            .shadow(
                elevation = ChartTooltipDefaults.ShadowElevation,
                shape = RoundedCornerShape(ChartTooltipDefaults.CornerRadius),
                clip = false
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(ChartTooltipDefaults.CornerRadius)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(ChartTooltipDefaults.CornerRadius)
            )
            .padding(
                start = ChartTooltipDefaults.HorizontalPadding,
                end = ChartTooltipDefaults.HorizontalPadding,
                top = ChartTooltipDefaults.TopPadding,
                bottom = ChartTooltipDefaults.BottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(ChartTooltipDefaults.RowSpacing)
    ) {
        metrics.forEach { metric ->
            ChartTooltipMetricRow(
                metric = metric,
                labelTextColor = labelTextColor,
                valueTextColor = valueTextColor
            )
        }

        Text(
            text = dayLabel,
            color = dayLabelColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dayLabelStartPadding,
                    top = 1.dp
                )
        )
    }
}

@Composable
private fun ChartTooltipMetricRow(
    metric: ChartTooltipMetricUi,
    labelTextColor: Color,
    valueTextColor: Color
) {
    Row(
        modifier = Modifier.height(ChartTooltipDefaults.RowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metric.emoji,
            fontSize = 13.sp,
            modifier = Modifier.width(ChartTooltipDefaults.EmojiSlotWidth),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(ChartTooltipDefaults.EmojiToLabelGap))

        Text(
            text = metric.label,
            color = labelTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(ChartTooltipDefaults.LabelToValueGap))

        Text(
            text = metric.value,
            color = valueTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Start
        )
    }
}
