package com.game.roguelike.rendering

import android.graphics.*
import kotlin.math.sin

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
}
