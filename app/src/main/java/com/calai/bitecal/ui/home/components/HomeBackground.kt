package com.calai.bitecal.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.random.Random

// ----------------------------
// Light / Dark 自動切換背景
// ----------------------------
@Composable
fun HomeBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    enableNoise: Boolean = true
) {
    if (darkTheme) {
        DarkHomeBackground(modifier)
    } else {
        Box(modifier = modifier) {
            LightHomeBackground(modifier)

            if (enableNoise) {
                val noise by remember { mutableStateOf(makeNoiseImage(256, 0.022f)) }

                Canvas(Modifier.fillMaxSize()) {
                    val paint = Paint()
                    drawIntoCanvas { canvas ->
                        var y = 0f
                        while (y < size.height) {
                            var x = 0f
                            while (x < size.width) {
                                canvas.drawImage(noise, Offset(x, y), paint)
                                x += noise.width.toFloat()
                            }
                            y += noise.height.toFloat()
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------
// 雜訊產生器（可選，用來避免漸層色帶）
// ----------------------------
@Stable
private fun makeNoiseImage(side: Int, alpha: Float): ImageBitmap {
    val img = ImageBitmap(side, side, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(img)
    val paint = Paint()
    val rnd = Random(42)
    val step = 2
    for (y in 0 until side step step) {
        for (x in 0 until side step step) {
            val v = rnd.nextInt(180, 255)
            paint.color = Color(v, v, v, (alpha * 255).toInt())
            canvas.drawRect(
                Rect(x.toFloat(), y.toFloat(), (x + step).toFloat(), (y + step).toFloat()),
                paint
            )
        }
    }
    return img
}

// ----------------------------
// 亮色主題背景（奶白 + 暖橘 + 冷藍灰）
// ----------------------------
@Composable
fun LightHomeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // --- 主底：奶白 ---
        drawRect(color = Color(0xFFF5F5F5), size = size)
    }
}




// ----------------------------
// 暗色主題背景（藍灰 + 黑紫 + 紅橘光）
// ----------------------------
@Composable
fun DarkHomeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // 主漸層：左上 → 右下
        val mainGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2E3656), // 左上深藍紫
                Color(0xFF1D1A23), // 中段黑紫灰
                Color(0xFF1D1A23)  // 右下深黑
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        drawRect(brush = mainGradient, size = size)

        // 左上柔光（藍霧氣）
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2E3656).copy(alpha = 0.40f),
                    Color(0xFF3A3E52).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(x = -size.width * 0.05f, y = -size.height * 0.05f),
                radius = size.minDimension * 1.4f
            )
        )

        // 右上柔光（紅橘）
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF532D25).copy(alpha = 0.55f),
                    Color(0xFF4A2924).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(x = size.width * 0.85f, y = size.height * 0.05f),
                radius = size.minDimension * 0.9f
            )
        )

        // 下方柔光（暗部加深）
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1D1A23),
                    Color.Transparent
                ),
                center = Offset(x = size.width * 0.9f, y = size.height * 0.9f),
                radius = size.minDimension * 0.9f
            )
        )
    }
}