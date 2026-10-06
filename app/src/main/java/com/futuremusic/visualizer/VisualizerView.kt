package com.futuremusic.visualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()

    private var levels = FloatArray(12) { 0f }
    private var autoPulse = 0f

    fun updateLevels(newLevels: FloatArray) {
        if (newLevels.size != levels.size) {
            levels = FloatArray(newLevels.size) { index -> newLevels.getOrNull(index) ?: 0f }
        } else {
            newLevels.copyInto(levels)
        }
        invalidate()
    }

    fun setAutoPulse(level: Float) {
        autoPulse = level
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 24f
        val gap = 12f
        val totalBars = levels.size.coerceAtLeast(1)
        val barWidth = (width - padding * 2 - gap * (totalBars - 1)) / totalBars

        val gradientColors = intArrayOf(
            Color.parseColor("#00E5FF"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#FF4ECD"),
            Color.parseColor("#6DFFB0")
        )

        val bg = Paint().apply {
            color = Color.argb(80, 15, 20, 30)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            28f,
            28f,
            bg
        )

        for (i in levels.indices) {
            val normalized = levels[i].coerceIn(0f, 1f)
            val targetHeight = max(26f, normalized * (height - padding * 2) + autoPulse * 120f)
            val x = padding + i * (barWidth + gap)
            val y = height - padding - targetHeight
            val rect = RectF(x, y, x + barWidth, height - padding)

            val gradient = LinearGradient(
                x,
                y,
                x + barWidth,
                height - padding,
                gradientColors,
                null,
                Shader.TileMode.CLAMP
            )
            barPaint.shader = gradient
            barPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 18f, 18f, barPaint)

            glowPaint.color = Color.argb(110, 0, 229, 255)
            glowPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                RectF(x - 5f, y - 8f, x + barWidth + 5f, height - padding + 8f),
                18f,
                18f,
                glowPaint
            )
        }
    }
}
