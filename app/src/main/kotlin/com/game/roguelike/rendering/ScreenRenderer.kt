package com.game.roguelike.rendering

import android.graphics.*
import kotlin.math.sin
import kotlin.math.min

class ScreenRenderer(private val renderer: IsometricRenderer) {

    fun renderMenu(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.parseColor("#0A0515")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // Decorative pillars
        renderer.paint.color = Color.parseColor("#1A0F2D")
        canvas.drawRect(w * 0.05f, h * 0.1f, w * 0.08f, h * 0.9f, renderer.paint)
        canvas.drawRect(w * 0.92f, h * 0.1f, w * 0.95f, h * 0.9f, renderer.paint)
        // Pillar detail
        renderer.paint.color = Color.parseColor("#2A1F3D")
        for (i in 0..4) {
            val y = h * 0.15f + i * h * 0.15f
            canvas.drawRect(w * 0.05f, y, w * 0.08f, y + 10f, renderer.paint)
            canvas.drawRect(w * 0.92f, y, w * 0.95f, y + 10f, renderer.paint)
        }

        // Background flames
        for (i in 0..5) {
            val fx = w * (0.1f + i * 0.15f)
            val fy = h * 0.85f
            val flameH = 30f + sin(renderer.globalTime * 3f + i * 1.5f) * 15f
            renderer.paint.color = Color.argb(30, 255, 80, 0)
            canvas.drawOval(fx - 15f, fy - flameH, fx + 15f, fy + 5f, renderer.paint)
            renderer.paint.color = Color.argb(20, 255, 150, 0)
            canvas.drawOval(fx - 8f, fy - flameH * 0.7f, fx + 8f, fy, renderer.paint)
        }

        // Title
        canvas.drawText("哈迪斯", w / 2f, h * 0.35f, renderer.titlePaint)

        // Subtitle
        canvas.drawText("地狱逃脱", w / 2f, h * 0.45f, renderer.subtitlePaint)

        // Tap to start
        val alpha = ((sin(renderer.globalTime * 3f) + 1) * 127).toInt()
        renderer.subtitlePaint.color = Color.argb(alpha, 200, 180, 150)
        canvas.drawText("点击开始", w / 2f, h * 0.65f, renderer.subtitlePaint)
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(200, 20, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        renderer.titlePaint.color = Color.parseColor("#FF2222")
        canvas.drawText("游戏结束", w / 2f, h * 0.4f, renderer.titlePaint)

        renderer.subtitlePaint.color = Color.parseColor("#AA8888")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, renderer.subtitlePaint)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(200, 0, 20, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        renderer.titlePaint.color = Color.parseColor("#FFD700")
        canvas.drawText("通关!", w / 2f, h * 0.4f, renderer.titlePaint)

        renderer.subtitlePaint.color = Color.parseColor("#DDAA66")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, renderer.subtitlePaint)
    }

    fun drawFade(canvas: Canvas, alpha: Float) {
        renderer.paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, renderer.screenWidth.toFloat(), renderer.screenHeight.toFloat(), renderer.paint)
    }

    fun renderBossEntrance(canvas: Canvas, bossName: String, bossTitle: String, timer: Float, phase: Int, w: Int, h: Int) {
        when (phase) {
            0 -> {
                // Phase 0: dark overlay fading in
                val overlayAlpha = min(timer / 1.5f, 1f) * 180f
                renderer.paint.color = Color.argb(overlayAlpha.toInt(), 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Center glow
                if (timer > 0.5f) {
                    val glowAlpha = min((timer - 0.5f) / 1f, 1f) * 60f
                    renderer.paint.color = Color.argb(glowAlpha.toInt(), 170, 68, 255)
                    val glowRadius = 80f + timer * 60f
                    canvas.drawCircle(w / 2f, h / 2f, glowRadius, renderer.paint)
                }
            }
            1 -> {
                // Phase 1: dark overlay + boss name with gold outline
                renderer.paint.color = Color.argb(180, 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Glow behind name
                val glowPulse = (sin(timer * 4f) + 1f) * 0.5f
                renderer.paint.color = Color.argb((40 + glowPulse * 30).toInt(), 170, 68, 255)
                canvas.drawCircle(w / 2f, h * 0.42f, 120f + glowPulse * 30f, renderer.paint)

                // Boss name — scale from large to normal
                val nameProgress = min(timer / 0.6f, 1f)
                val nameScale = 1f + (1f - nameProgress) * 0.8f
                val nameAlpha = (nameProgress * 255).toInt()

                canvas.save()
                canvas.scale(nameScale, nameScale, w / 2f, h * 0.42f)

                // Gold outline
                renderer.titlePaint.color = Color.argb(nameAlpha, 255, 215, 0)
                renderer.titlePaint.style = Paint.Style.FILL_AND_STROKE
                renderer.titlePaint.strokeWidth = 4f
                canvas.drawText(bossName, w / 2f, h * 0.42f, renderer.titlePaint)
                renderer.titlePaint.style = Paint.Style.FILL
                renderer.titlePaint.strokeWidth = 0f

                canvas.restore()

                // Title text (subtitle)
                if (timer > 0.4f) {
                    val titleProgress = min((timer - 0.4f) / 0.5f, 1f)
                    val titleAlpha = (titleProgress * 200).toInt()
                    renderer.subtitlePaint.color = Color.argb(titleAlpha, 170, 130, 200)
                    canvas.drawText(bossTitle, w / 2f, h * 0.52f, renderer.subtitlePaint)
                }

                // Decorative lines
                val lineAlpha = (nameProgress * 120).toInt()
                renderer.paint.color = Color.argb(lineAlpha, 170, 68, 255)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 2f
                val lineW = min(timer / 0.8f, 1f) * w * 0.35f
                canvas.drawLine(w / 2f - lineW, h * 0.47f, w / 2f + lineW, h * 0.47f, renderer.paint)
                renderer.paint.style = Paint.Style.FILL
                renderer.paint.strokeWidth = 0f
            }
            2 -> {
                // Phase 2: fade out
                val fadeOut = min(timer / 0.5f, 1f)
                val overlayAlpha = 180f * (1f - fadeOut)
                renderer.paint.color = Color.argb(overlayAlpha.toInt(), 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Fading name
                val nameAlpha = ((1f - fadeOut) * 255).toInt()
                if (nameAlpha > 0) {
                    renderer.titlePaint.color = Color.argb(nameAlpha, 255, 215, 0)
                    canvas.drawText(bossName, w / 2f, h * 0.42f, renderer.titlePaint)

                    renderer.subtitlePaint.color = Color.argb((nameAlpha * 0.78f).toInt(), 170, 130, 200)
                    canvas.drawText(bossTitle, w / 2f, h * 0.52f, renderer.subtitlePaint)
                }
            }
        }
    }
}
