package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.core.BlessingType
import com.game.roguelike.entity.Player

class HUD {
    private val paint = Paint()
    private val bgPaint = Paint()
    private var hudX = 0f
    private var hudY = 0f
    private var barWidth = 200f
    private var barHeight = 16f

    fun updateLayout(screenW: Int, screenH: Int) {
        hudX = 20f
        hudY = 20f
        barWidth = screenW * 0.25f
    }

    fun render(canvas: Canvas, player: Player, gold: Int, blessings: List<Blessing>, layerIndex: Int, roomIndex: Int) {
        // HP bar background
        bgPaint.color = Color.argb(150, 0, 0, 0)
        paint.style = Paint.Style.FILL
        val hpBgRect = RectF(hudX, hudY, hudX + barWidth, hudY + barHeight)
        canvas.drawRoundRect(hpBgRect, 4f, 4f, bgPaint)

        // HP bar fill
        val hpRatio = player.health.toFloat() / player.maxHealth
        val hpColor = when {
            hpRatio > 0.6f -> Color.parseColor("#44CC44")
            hpRatio > 0.3f -> Color.parseColor("#CCCC44")
            else -> Color.parseColor("#CC4444")
        }
        paint.color = hpColor
        val hpFillRect = RectF(hudX, hudY, hudX + barWidth * hpRatio, hudY + barHeight)
        canvas.drawRoundRect(hpFillRect, 4f, 4f, paint)

        // HP text
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("生命 ${player.health}/${player.maxHealth}", hudX + 5f, hudY + 13f, paint)

        // Gold
        paint.color = Color.parseColor("#FFD700")
        paint.textSize = 14f
        canvas.drawText("金币: $gold", hudX, hudY + 35f, paint)

        // Layer/Room info
        val layerName = when (layerIndex) {
            0 -> "塔耳塔洛斯"
            1 -> "阿斯福德"
            2 -> "伊利西昂"
            else -> "未知"
        }
        paint.color = Color.parseColor("#AABBCC")
        paint.textSize = 12f
        canvas.drawText("$layerName - 第${roomIndex + 1}间", hudX, hudY + 55f, paint)

        // Blessing icons
        var bx = hudX
        val by = hudY + 65f
        paint.textSize = 10f
        for (blessing in blessings) {
            paint.color = when (blessing.type) {
                BlessingType.ATTACK -> Color.parseColor("#FF4444")
                BlessingType.SPECIAL -> Color.parseColor("#4488FF")
                BlessingType.DASH -> Color.parseColor("#44FF88")
                BlessingType.SUPPORT -> Color.parseColor("#FFAA44")
            }
            canvas.drawCircle(bx + 8f, by, 8f, paint)
            paint.color = Color.WHITE
            val icon = when (blessing.type) {
                BlessingType.ATTACK -> "攻"
                BlessingType.SPECIAL -> "技"
                BlessingType.DASH -> "冲"
                BlessingType.SUPPORT -> "援"
            }
            canvas.drawText(icon, bx + 4f, by + 4f, paint)
            bx += 22f
        }

        // Cooldown indicators
        val cdX = hudX + barWidth + 20f
        val cdY = hudY

        // Special CD
        if (player.specialCooldownTimer > 0) {
            paint.color = Color.argb(150, 100, 100, 255)
            val ratio = player.specialCooldownTimer / player.specialCooldown
            canvas.drawRoundRect(RectF(cdX, cdY, cdX + 60f, cdY + 10f), 3f, 3f, bgPaint)
            paint.color = Color.argb(200, 80, 80, 220)
            canvas.drawRoundRect(RectF(cdX, cdY, cdX + 60f * (1f - ratio), cdY + 10f), 3f, 3f, paint)
        } else {
            paint.color = Color.parseColor("#4488FF")
            canvas.drawText("飞刀 就绪", cdX, cdY + 10f, paint)
        }

        // Dash CD
        if (player.dashCooldownTimer > 0) {
            paint.color = Color.argb(150, 100, 255, 100)
            val ratio = player.dashCooldownTimer / player.dashCooldown
            canvas.drawRoundRect(RectF(cdX, cdY + 15f, cdX + 60f, cdY + 25f), 3f, 3f, bgPaint)
            paint.color = Color.argb(200, 80, 220, 80)
            canvas.drawRoundRect(RectF(cdX, cdY + 15f, cdX + 60f * (1f - ratio), cdY + 25f), 3f, 3f, paint)
        } else {
            paint.color = Color.parseColor("#44FF88")
            canvas.drawText("冲刺 就绪", cdX, cdY + 25f, paint)
        }

        // Athena shield indicator
        if (player.athenaShieldActive) {
            paint.color = Color.parseColor("#FFAA44")
            canvas.drawText("雅典娜之盾 激活", cdX, cdY + 40f, paint)
        }
    }
}