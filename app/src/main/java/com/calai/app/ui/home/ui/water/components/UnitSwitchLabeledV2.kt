package com.calai.app.ui.home.ui.water.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * UnitSwitchLabeledV2 (iOS-like: white track + black thumb)
 * - Track：白底 + 淡描邊
 * - Thumb：黑底 + 白字（你要的「滑動那顆」）
 * - 可左右拖曳切換 + 點左右半邊切換
 * - RTL 支援
 */
@Composable
fun UnitSwitchLabeledV2(
    checked: Boolean,                         // true = 右側 (ml)；false = 左側 (oz)
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,

    width: Dp = 92.dp,
    height: Dp = 30.dp,
    padding: Dp = 3.dp,

    leftLabel: String = "oz",
    rightLabel: String = "ml",

    // ✅ Track：白底 + 淡描邊（高級感）
    trackBg: Color = Color.White,
    trackStroke: Color = Color(0xFF111114).copy(alpha = 0.10f),

    // ✅ Thumb：黑底 + 白字（重點）
    thumbBg: Color = Color(0xFF111114),
    thumbShadow: Dp = 6.dp,

    // 文字：選中白字（在 thumb 上），未選淡黑字（在 track 上）
    textOn: Color = Color.White,
    textOff: Color = Color(0xFF111114).copy(alpha = 0.55f),

    textStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),

    // 拖曳門檻：避免誤觸
    snapThreshold: Float = 0.35f,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }

    val shape = RoundedCornerShape(height / 2)

    fun checkedToPos(c: Boolean): Float = if (c) 1f else 0f

    // 拖曳位置（0..1）
    var dragPos by remember { mutableFloatStateOf(checkedToPos(checked)) }

    LaunchedEffect(checked, isRtl) {
        dragPos = checkedToPos(checked)
    }

    val animPos by animateFloatAsState(
        targetValue = dragPos,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.86f),
        label = "unitSwitchThumbPos"
    )

    // 幾何（thumb 略寬於半寬）
    val innerWidthDp = width - padding * 2
    val halfDp = innerWidthDp / 2
    val thumbWidthDp = (halfDp + 6.dp).coerceIn(halfDp, innerWidthDp)
    val thumbHeightDp = height - padding * 2
    val travelDp = innerWidthDp - thumbWidthDp

    val thumbOffsetDp = padding + travelDp * (if (!isRtl) animPos else (1f - animPos))

    val stateText = if (checked) rightLabel else leftLabel

    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .background(trackBg)
            .border(width = 1.dp, color = trackStroke, shape = shape)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                contentDescription = "Unit switch"
                stateDescription = stateText
            }
    ) {
        // ① 底層兩側文字（未選側看得到；選側被 thumb 蓋住）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(halfDp)
                    .height(thumbHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) rightLabel else leftLabel,
                    style = textStyle,
                    color = textOff,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .width(halfDp)
                    .height(thumbHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) leftLabel else rightLabel,
                    style = textStyle,
                    color = textOff,
                    maxLines = 1
                )
            }
        }

        // ② Thumb：黑底 + 白字 + 柔陰影
        val thumbShape = RoundedCornerShape(thumbHeightDp / 2)
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetDp)
                .align(Alignment.CenterStart)
                .width(thumbWidthDp)
                .height(thumbHeightDp)
                .shadow(thumbShadow, thumbShape, clip = false)
                .clip(thumbShape)
                .background(thumbBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stateText,
                style = textStyle,
                color = textOn,
                maxLines = 1
            )
        }

        // ③ 拖曳層（thumb 跟手、放開吸附）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerDragToToggle(
                    enabled = true,
                    isRtl = isRtl,
                    travelDp = travelDp,
                    currentPosProvider = { dragPos },
                    onPosChange = { dragPos = it },
                    onRelease = { finalPos ->
                        val wantChecked = finalPos >= 0.5f
                        if (wantChecked != checked) onCheckedChange(wantChecked)
                        dragPos = checkedToPos(wantChecked)
                    },
                    snapThreshold = snapThreshold
                )
        )

        // ④ 點擊左右半邊（segmented 正常操作）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(interactionSource = interaction, indication = null) {
                        val want = if (isRtl) true else false
                        if (want != checked) scope.launch { onCheckedChange(want) }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(interactionSource = interaction, indication = null) {
                        val want = if (isRtl) false else true
                        if (want != checked) scope.launch { onCheckedChange(want) }
                    }
            )
        }
    }
}

/**
 * pointerInput scope 自帶 Density：用 Dp.toPx()，不要 LocalDensity.current
 */
private fun Modifier.pointerDragToToggle(
    enabled: Boolean,
    isRtl: Boolean,
    travelDp: Dp,
    currentPosProvider: () -> Float,     // 0..1
    onPosChange: (Float) -> Unit,
    onRelease: (Float) -> Unit,          // final 0..1
    snapThreshold: Float
): Modifier = if (!enabled) this else {
    this.then(
        Modifier.pointerInput(enabled, isRtl, travelDp, snapThreshold) {
            val travelPx = travelDp.toPx().coerceAtLeast(1f)

            detectDragGestures(
                onDragEnd = {
                    val pos = currentPosProvider()
                    val snapped = when {
                        pos <= snapThreshold -> 0f
                        pos >= (1f - snapThreshold) -> 1f
                        else -> if (pos >= 0.5f) 1f else 0f
                    }
                    onRelease(snapped)
                },
                onDragCancel = {
                    val pos = currentPosProvider()
                    onRelease(if (pos >= 0.5f) 1f else 0f)
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    val dx = dragAmount.x
                    val ltrDx = if (!isRtl) dx else -dx

                    val delta = ltrDx / travelPx
                    val next = (currentPosProvider() + delta).coerceIn(0f, 1f)
                    onPosChange(next)
                }
            )
        }
    )
}
