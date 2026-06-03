package com.calai.bitecal.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.roundToInt

object BiteCalWidgetRingRenderer {
    fun render(
        context: Context,
        progressPercent: Int,
        sizeDp: Int,
        strokeDp: Float = 7f,
        progressColor: Int = Color.rgb(17, 17, 20),
        trackColor: Int = Color.rgb(234, 234, 237),
        tickRadiusScale: Float = 0.42f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)
        val strokePx = strokeDp * density
        val inset = strokePx / 2f + density
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
        }

        paint.color = trackColor
        canvas.drawArc(rect, -90f, 360f, false, paint)

        val sweep = progressPercent.coerceIn(0, 100) * 3.6f
        if (sweep > 0f) {
            paint.color = progressColor
            canvas.drawArc(rect, -90f, sweep, false, paint)
        }

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, inset, strokePx * tickRadiusScale, tickPaint)

        return bitmap
    }
}
