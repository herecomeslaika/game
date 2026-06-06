package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.game.roguelike.core.Game
import com.game.roguelike.core.InputManager

class ActionButtons {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    var atkX = 0f; var atkY = 0f; var atkR = 0f
        private set
    var spcX = 0f; var spcY = 0f; var spcR = 0f
        private set
    var dshX = 0f; var dshY = 0f; var dshR = 0f
        private set

    fun updateLayout(screenW: Int, screenH: Int, inputManager: InputManager? = null) {
        val baseR = screenH * 0.07f
        atkR = baseR
        spcR = baseR * 0.85f
        dshR = baseR * 0.85f
        val rightX = screenW - baseR * 1.8f
        atkX = rightX
        atkY = screenH * 0.35f
        spcX = rightX - baseR * 2f
        spcY = screenH * 0.55f
        dshX = rightX
        dshY = screenH * 0.7f

        inputManager?.let { im ->
            im.atkX = atkX; im.atkY = atkY; im.atkR = atkR
            im.spcX = spcX; im.spcY = spcY; im.spcR = spcR
            im.dshX = dshX; im.dshY = dshY; im.dshR = dshR
        }
    }

    fun render(canvas: Canvas, game: Game) {
        drawButton(canvas, atkX, atkY, atkR, Color.rgb(185, 36, 48), Color.rgb(255, 232, 210))
        drawSwordIcon(canvas, atkX, atkY, atkR)

        drawButton(canvas, spcX, spcY, spcR, Color.rgb(44, 85, 190), Color.rgb(210, 228, 255))
        drawKnifeIcon(canvas, spcX, spcY, spcR)

        drawButton(canvas, dshX, dshY, dshR, Color.rgb(38, 150, 82), Color.rgb(218, 255, 225))
        drawDashIcon(canvas, dshX, dshY, dshR)
    }

    private fun drawButton(canvas: Canvas, x: Float, y: Float, r: Float, fillColor: Int, strokeColor: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(92, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor))
        canvas.drawCircle(x, y, r, paint)

        paint.color = Color.argb(36, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
        canvas.drawCircle(x, y, r * 1.12f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.035f
        paint.color = Color.argb(155, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
        canvas.drawCircle(x, y, r * 0.94f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(42, 255, 255, 255)
        canvas.drawCircle(x - r * 0.18f, y - r * 0.22f, r * 0.42f, paint)
    }

    private fun drawSwordIcon(canvas: Canvas, x: Float, y: Float, r: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(235, 255, 245, 220)
        path.reset()
        path.moveTo(x + r * 0.07f, y - r * 0.62f)
        path.lineTo(x + r * 0.25f, y - r * 0.08f)
        path.lineTo(x + r * 0.05f, y + r * 0.40f)
        path.lineTo(x - r * 0.15f, y - r * 0.08f)
        path.close()
        canvas.save()
        canvas.rotate(38f, x, y)
        canvas.drawPath(path, paint)

        paint.color = Color.argb(230, 255, 210, 120)
        canvas.drawRoundRect(x - r * 0.30f, y + r * 0.20f, x + r * 0.30f, y + r * 0.30f, r * 0.04f, r * 0.04f, paint)
        paint.color = Color.argb(235, 105, 58, 36)
        canvas.drawRoundRect(x - r * 0.07f, y + r * 0.27f, x + r * 0.07f, y + r * 0.58f, r * 0.04f, r * 0.04f, paint)
        canvas.restore()
    }

    private fun drawKnifeIcon(canvas: Canvas, x: Float, y: Float, r: Float) {
        canvas.save()
        canvas.rotate(-28f, x, y)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(235, 220, 240, 255)
        path.reset()
        path.moveTo(x - r * 0.10f, y - r * 0.58f)
        path.lineTo(x + r * 0.22f, y - r * 0.06f)
        path.lineTo(x - r * 0.02f, y + r * 0.18f)
        path.lineTo(x - r * 0.24f, y - r * 0.03f)
        path.close()
        canvas.drawPath(path, paint)

        paint.color = Color.argb(225, 80, 118, 230)
        canvas.drawRoundRect(x - r * 0.09f, y + r * 0.12f, x + r * 0.09f, y + r * 0.50f, r * 0.05f, r * 0.05f, paint)
        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.045f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.argb(185, 220, 235, 255)
        canvas.drawLine(x - r * 0.48f, y + r * 0.28f, x - r * 0.24f, y + r * 0.08f, paint)
        canvas.drawLine(x - r * 0.36f, y + r * 0.44f, x - r * 0.12f, y + r * 0.25f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawDashIcon(canvas: Canvas, x: Float, y: Float, r: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.10f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.argb(235, 225, 255, 232)

        canvas.drawLine(x - r * 0.46f, y - r * 0.18f, x + r * 0.18f, y - r * 0.18f, paint)
        canvas.drawLine(x - r * 0.34f, y + r * 0.18f, x + r * 0.30f, y + r * 0.18f, paint)
        paint.strokeCap = Paint.Cap.BUTT

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(235, 225, 255, 232)
        drawArrowHead(canvas, x + r * 0.38f, y - r * 0.18f, r * 0.22f)
        drawArrowHead(canvas, x + r * 0.50f, y + r * 0.18f, r * 0.22f)
    }

    private fun drawArrowHead(canvas: Canvas, x: Float, y: Float, size: Float) {
        path.reset()
        path.moveTo(x, y)
        path.lineTo(x - size, y - size * 0.72f)
        path.lineTo(x - size, y + size * 0.72f)
        path.close()
        canvas.drawPath(path, paint)
    }
}
