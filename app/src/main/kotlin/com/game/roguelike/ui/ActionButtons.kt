package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.core.Game
import com.game.roguelike.core.InputManager

class ActionButtons {
    private val paint = Paint()
    var atkX = 0f; var atkY = 0f; var atkR = 0f
        private set
    var spcX = 0f; var spcY = 0f; var spcR = 0f
        private set
    var dshX = 0f; var dshY = 0f; var dshR = 0f
        private set

    fun updateLayout(screenW: Int, screenH: Int, inputManager: InputManager? = null) {
        val baseR = screenH * 0.07f
        atkR = baseR; spcR = baseR * 0.85f; dshR = baseR * 0.85f
        val rightX = screenW - baseR * 1.8f
        atkX = rightX; atkY = screenH * 0.35f
        spcX = rightX - baseR * 2f; spcY = screenH * 0.55f
        dshX = rightX; dshY = screenH * 0.7f

        // Push button positions to InputManager so it can do accurate hit-testing
        inputManager?.let { im ->
            im.atkX = atkX; im.atkY = atkY; im.atkR = atkR
            im.spcX = spcX; im.spcY = spcY; im.spcR = spcR
            im.dshX = dshX; im.dshY = dshY; im.dshR = dshR
        }
    }

    fun render(canvas: Canvas, game: Game) {
        paint.style = Paint.Style.FILL

        // Attack button
        paint.color = Color.argb(100, 220, 60, 60)
        canvas.drawCircle(atkX, atkY, atkR, paint)
        paint.color = Color.WHITE; paint.textSize = atkR * 0.5f
        paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("攻", atkX, atkY + atkR * 0.18f, paint)

        // Special button
        paint.color = Color.argb(100, 60, 100, 220)
        canvas.drawCircle(spcX, spcY, spcR, paint)
        paint.color = Color.WHITE; paint.textSize = spcR * 0.45f
        canvas.drawText("技", spcX, spcY + spcR * 0.18f, paint)

        // Dash button
        paint.color = Color.argb(100, 60, 200, 100)
        canvas.drawCircle(dshX, dshY, dshR, paint)
        paint.color = Color.WHITE; paint.textSize = dshR * 0.45f
        canvas.drawText("冲", dshX, dshY + dshR * 0.18f, paint)

        paint.typeface = Typeface.DEFAULT
    }
}