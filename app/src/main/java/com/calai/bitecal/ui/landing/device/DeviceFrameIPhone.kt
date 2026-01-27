package com.calai.bitecal.ui.landing.device

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iPhone 風格外框（底層：外框/黑邊/側鍵/玻璃高光 → 中層：內容 → 上層：Dynamic Island + 前鏡頭）
 */
@Composable
fun DeviceFrameIPhone(
    modifier: Modifier = Modifier,
    // 外觀
    frameColor: Color = Color(0xFFB7B9C0),
    bezelColor: Color = Color(0xFF0A0A0A),
    backgroundColor: Color = Color.Transparent,
    cornerRadius: Dp = 28.dp,
    frameThickness: Dp = 4.dp,
    bezelThickness: Dp = 9.dp,
    // Dynamic Island（上層）
    islandHeight: Dp = 14.dp,
    islandTopOffset: Dp = 0.dp,
    islandWidthFraction: Float = 0.34f,
    // 動態島描邊（置中）
    islandStrokeWidth: Dp = 1.dp,
    islandStrokeColor: Color = Color.White,
    islandStrokeAlpha: Float = 0.20f,
    // 側鍵（底層，左右獨立上移）
    showButtons: Boolean = true,
    buttonColor: Color = Color(0xFF9DA1A8),
    buttonThickness: Dp = 3.4.dp,
    powerButtonLengthFraction: Float = 0.13f,
    volumeButtonLengthFraction: Float = 0.10f,
    buttonInsetFromEdge: Dp = 2.dp,
    powerButtonCenterBias: Float = 0.12f,
    volumeButtonsCenterBias: Float = 0.12f,
    // 前鏡頭（上層）
    showFrontCameraDot: Boolean = true,
    frontCameraDotSize: Dp = 7.dp,
    frontCameraRingScale: Float = 1.45f,
    frontCameraRingColor: Color = Color(0xFF3A3A3A),
    frontCameraRingAlpha: Float = 0.95f,
    frontCameraSpecSize: Dp = 1.8.dp,
    // 舊：以中心為基準的 X 偏移（相容用）
    frontCameraDotOffsetXFraction: Float = 0.18f,
    frontCameraDotColor: Color = Color(0xFF111315),
    // ★ 新：靠左 / 靠右對齊模式（啟用時覆蓋 fraction 演算法）
    frontCameraDotAlignLeft: Boolean = false,
    frontCameraDotLeftInset: Dp = 5.dp,
    frontCameraDotAlignRight: Boolean = false,
    frontCameraDotRightInset: Dp = 5.dp,
    // 內屏玻璃高光（底層）
    enableInnerGlassHighlight: Boolean = true,
    glassStroke: Dp = 1.dp,
    glassHighlightAlpha: Float = 0.06f,
    // 內容 padding（影片更高但不頂到邊）
    contentTopExtraPadding: Dp = 2.dp,
    contentBottomExtraPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val framePx = with(density) { frameThickness.toPx() }
    val bezelPx = with(density) { bezelThickness.toPx() }
    val cornerPx = with(density) { cornerRadius.toPx() }
    val islandHeightPx = with(density) { islandHeight.toPx() }
    val islandTopOffsetPx = with(density) { islandTopOffset.toPx() }

    // 內容圓角
    val innerCornerDp = (cornerRadius - frameThickness - bezelThickness).let {
        if (it.value < 0f) 0.dp else it
    }

    // 內容安全邊：四邊 + 避開島
    val topContentPadding = frameThickness + bezelThickness + islandHeight + islandTopOffset + contentTopExtraPadding
    val sideContentPadding = frameThickness + bezelThickness
    val bottomContentPadding = frameThickness + bezelThickness + contentBottomExtraPadding

    Box(modifier = modifier.background(backgroundColor)) {

        // ===== 底層：外框 / 黑邊 / 側鍵 / 內屏玻璃高光 =====
        Canvas(Modifier.fillMaxSize()) {
            // 外框
            drawRoundRect(color = frameColor, cornerRadius = CornerRadius(cornerPx, cornerPx))

            // 黑邊（bezel）
            val innerLeft = framePx
            val innerTop = framePx
            val innerRight = size.width - framePx
            val innerBottom = size.height - framePx
            drawRoundRect(
                color = bezelColor,
                topLeft = Offset(innerLeft, innerTop),
                size = Size(innerRight - innerLeft, innerBottom - innerTop),
                cornerRadius = CornerRadius(cornerPx - framePx, cornerPx - framePx)
            )

            // 螢幕區域（供按鍵/高光定位）
            val screenLeft = innerLeft + bezelPx
            val screenRight = innerRight - bezelPx
            val screenTop = innerTop + bezelPx
            val screenBottom = innerBottom - bezelPx
            val screenWidth = screenRight - screenLeft
            val screenHeight = screenBottom - screenTop

            // 側鍵
            if (showButtons) {
                val btnT = with(density) { buttonThickness.toPx() }
                val inset = with(density) { buttonInsetFromEdge.toPx() }

                // 右：電源鍵（單段）
                run {
                    val L = screenHeight * powerButtonLengthFraction
                    val cy = screenTop + screenHeight / 2f - screenHeight * powerButtonCenterBias
                    val topY = cy - L / 2f
                    val leftX = innerRight - inset - btnT
                    drawRoundRect(
                        color = buttonColor,
                        topLeft = Offset(leftX, topY),
                        size = Size(btnT, L),
                        cornerRadius = CornerRadius(btnT / 2f, btnT / 2f)
                    )
                }

                // 左：音量鍵兩段
                run {
                    val height = screenHeight * volumeButtonLengthFraction
                    val gap = screenHeight * 0.022f
                    val cy = screenTop + screenHeight / 2f - screenHeight * volumeButtonsCenterBias
                    val leftX = innerLeft + inset
                    // 上
                    drawRoundRect(
                        color = buttonColor,
                        topLeft = Offset(leftX, cy - height - gap / 2f),
                        size = Size(btnT, height),
                        cornerRadius = CornerRadius(btnT / 2f, btnT / 2f)
                    )
                    // 下
                    drawRoundRect(
                        color = buttonColor,
                        topLeft = Offset(leftX, cy + gap / 2f),
                        size = Size(btnT, height),
                        cornerRadius = CornerRadius(btnT / 2f, btnT / 2f)
                    )
                }
            }

            // 內屏玻璃高光（極淡一圈）
            if (enableInnerGlassHighlight) {
                val inset = bezelPx + with(density) { 1.dp.toPx() }
                val stroke = with(density) { glassStroke.toPx() }
                val w = (innerRight - innerLeft) - inset * 2
                val h = (innerBottom - innerTop) - inset * 2
                val r = (cornerPx - framePx - inset).coerceAtLeast(0f)
                drawRoundRect(
                    color = Color.White.copy(alpha = glassHighlightAlpha),
                    topLeft = Offset(innerLeft + inset, innerTop + inset),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = stroke)
                )
            }
        }

        // ===== 中層：內容（影片等） =====
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = sideContentPadding,
                    end = sideContentPadding,
                    top = topContentPadding,
                    bottom = bottomContentPadding
                )
                .clip(RoundedCornerShape(innerCornerDp)),
            content = content
        )

        // ===== 上層：Dynamic Island + 前鏡頭（永遠在最上） =====
        Canvas(Modifier.fillMaxSize()) {
            val innerLeft = framePx
            val innerTop = framePx
            val innerRight = size.width - framePx
            val innerBottom = size.height - framePx
            val screenLeft = innerLeft + bezelPx
            val screenRight = innerRight - bezelPx
            val screenTop = innerTop + bezelPx
            val screenBottom = innerBottom - bezelPx
            val screenWidth = screenRight - screenLeft

            val islandWidth = screenWidth * islandWidthFraction
            val islandLeft = screenLeft + (screenWidth - islandWidth) / 2f
            val islandTop = screenTop + islandTopOffsetPx
            val islandCorner = islandHeightPx / 2f

            // Dynamic Island（填色）
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(islandLeft, islandTop),
                size = Size(islandWidth, islandHeightPx),
                cornerRadius = CornerRadius(islandCorner, islandCorner)
            )

            // 動態島描邊（置中：stroke/2 內縮）
            val strokePx = with(density) { islandStrokeWidth.toPx() }
            val inset = strokePx / 2f
            drawRoundRect(
                color = islandStrokeColor.copy(alpha = islandStrokeAlpha),
                topLeft = Offset(islandLeft + inset, islandTop + inset),
                size = Size(islandWidth - strokePx, islandHeightPx - strokePx),
                cornerRadius = CornerRadius(islandCorner - inset, islandCorner - inset),
                style = Stroke(width = strokePx)
            )

            // 前鏡頭（外環 + 主體 + 白色高光）
            if (showFrontCameraDot) {
                val dotR = with(density) { (frontCameraDotSize / 2).toPx() }
                val ringR = dotR * frontCameraRingScale
                val islandCenterX = islandLeft + islandWidth / 2f
                val islandCenterY = islandTop + islandHeightPx / 2f

                // 位置：右優先 > 左 > fraction
                val dotCx = when {
                    frontCameraDotAlignRight -> {
                        val rightInsetPx = with(density) { frontCameraDotRightInset.toPx() }
                        val strokeForCam = with(density) { islandStrokeWidth.toPx() }
                        islandLeft + islandWidth - (rightInsetPx + ringR + strokeForCam)
                    }
                    frontCameraDotAlignLeft -> {
                        val leftInsetPx = with(density) { frontCameraDotLeftInset.toPx() }
                        val strokeForCam = with(density) { islandStrokeWidth.toPx() }
                        islandLeft + leftInsetPx + ringR + strokeForCam
                    }
                    else -> islandCenterX + islandWidth * frontCameraDotOffsetXFraction
                }
                val dotCy = islandCenterY

                // 外環
                drawCircle(
                    color = frontCameraRingColor.copy(alpha = frontCameraRingAlpha),
                    radius = ringR,
                    center = Offset(dotCx, dotCy)
                )
                // 主體
                drawCircle(color = frontCameraDotColor, radius = dotR, center = Offset(dotCx, dotCy))
                // 高光（右上）
                val specR = with(density) { (frontCameraSpecSize / 2).toPx() }
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = specR,
                    center = Offset(dotCx + dotR * 0.35f, dotCy - dotR * 0.35f)
                )
            }
        }
    }
}
