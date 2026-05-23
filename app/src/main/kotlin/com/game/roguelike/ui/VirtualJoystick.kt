package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.util.Vector2

class VirtualJoystick {
    var centerX = 200f
    var centerY = 400f
    private val baseRadius = 80f
    private val knobRadius = 35f
    var knobX = centerX
    var knobY = centerY

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun updateLayout(screenW: Int, screenH: Int) {
        centerX = screenW * 0.15f
        centerY = screenH * 0.65f
        knobX = centerX
        knobY = centerY
    }

    fun updateKnob(direction: Vector2) {
        knobX = centerX + direction.x * baseRadius
        knobY = centerY + direction.y * baseRadius
    }

    fun render(canvas: Canvas) {
        // Base circle
        paint.color = Color.argb(40, 255, 255, 255)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, baseRadius, paint)

        // Base border
        paint.color = Color.argb(80, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(centerX, centerY, baseRadius, paint)

        // Knob
        paint.color = Color.argb(100, 200, 200, 255)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(knobX, knobY, knobRadius, paint)

        paint.color = Color.argb(150, 220, 220, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(knobX, knobY, knobRadius, paint)
    }
}
