package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.GodType
import com.game.roguelike.core.color
import com.game.roguelike.core.icon
import com.game.roguelike.entity.Player

class HUD {
    private val paint = Paint()
    private val bgPaint = Paint()
    private var hudX = 0f
    private var hudY = 0f
    private var barWidth = 400f
    private var barHeight = 36f
    private val scale = 2f // HUD整体放大倍数（再次放大）
    // 右上角返回按钮参数（放大为原来的3倍）
    private val backBtnWidth = 420f
    private val backBtnHeight = 150f
    private var backBtnX = 0f
    private var backBtnY = 30f

    // 检测返回按钮点击
    fun handleBackButtonClick(x: Float, y: Float): Boolean {
        return x >= backBtnX && x <= backBtnX + backBtnWidth &&
               y >= backBtnY && y <= backBtnY + backBtnHeight
    }

    fun updateLayout(screenW: Int, screenH: Int) {
        hudX = 30f
        hudY = 30f
        barWidth = screenW * 0.35f
        // 右上角返回按钮位置
        backBtnX = screenW - backBtnWidth - 30f
    }

    fun render(canvas: Canvas, player: Player, gold: Int, blessings: List<Blessing>, layerIndex: Int) {
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
        paint.textSize = 28f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("生命 ${player.health}/${player.maxHealth}", hudX + 12f, hudY + 26f, paint)

        // Gold
        paint.color = Color.parseColor("#FFD700")
        paint.textSize = 26f
        canvas.drawText("金币: $gold", hudX, hudY + 75f, paint)

        // Layer/Room info
        val layerName = when (layerIndex) {
            0 -> "塔耳塔洛斯"
            1 -> "阿斯福德"
            2 -> "伊利西昂"
            else -> "未知"
        }
        paint.color = Color.parseColor("#AABBCC")
        paint.textSize = 24f
        canvas.drawText(layerName, hudX, hudY + 115f, paint)

        // Blessing icons (god-colored)
        var bx = hudX
        val by = hudY + 145f
        paint.textSize = 20f
        for (blessing in blessings) {
            paint.color = blessing.god.color
            canvas.drawCircle(bx + 16f, by, 16f, paint)

            // Duo star
            if (blessing.rarity == BlessingRarity.DUO) {
                paint.color = Color.parseColor("#FFD700")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(bx + 16f, by, 24f, paint)
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 1f
            }

            paint.color = Color.WHITE
            val icon = blessing.god.icon
            canvas.drawText(icon, bx + 8f, by + 8f, paint)
            bx += 44f
        }

        // Cooldown indicators
        val cdX = hudX + barWidth + 40f
        val cdY = hudY

        // Special CD
        if (player.specialCooldownTimer > 0) {
            paint.color = Color.argb(150, 100, 100, 255)
            val ratio = player.specialCooldownTimer / player.specialCooldown
            canvas.drawRoundRect(RectF(cdX, cdY, cdX + 120f, cdY + 20f), 6f, 6f, bgPaint)
            paint.color = Color.argb(200, 80, 80, 220)
            canvas.drawRoundRect(RectF(cdX, cdY, cdX + 120f * (1f - ratio), cdY + 20f), 6f, 6f, paint)
        } else {
            paint.color = Color.parseColor("#4488FF")
            paint.textSize = 22f
            canvas.drawText("飞刀 就绪", cdX, cdY + 18f, paint)
        }

        // Dash CD
        if (player.dashCooldownTimer > 0) {
            val ratio = player.dashCooldownTimer / player.dashCooldown
            canvas.drawRoundRect(RectF(cdX, cdY + 30f, cdX + 120f, cdY + 50f), 6f, 6f, bgPaint)
            paint.color = Color.argb(200, 80, 220, 80)
            canvas.drawRoundRect(RectF(cdX, cdY + 30f, cdX + 120f * (1f - ratio), cdY + 50f), 6f, 6f, paint)
        } else {
            paint.color = Color.parseColor("#44FF88")
            paint.textSize = 22f
            canvas.drawText("冲刺 就绪", cdX, cdY + 48f, paint)
        }

        // Athena shield indicator
        if (player.athenaShieldActive) {
            paint.color = Color.parseColor("#FFAA44")
            paint.textSize = 22f
            canvas.drawText("神盾 激活", cdX, cdY + 78f, paint)
        }

        // Crit indicator
        if (player.critChance > 0f) {
            paint.color = Color.parseColor("#FF44AA")
            paint.textSize = 22f
            canvas.drawText("暴击${(player.critChance * 100).toInt()}%", cdX, cdY + 108f, paint)
        }

        // Slow indicator
        if (player.slowOnHit > 0f) {
            paint.color = Color.parseColor("#88CCFF")
            paint.textSize = 22f
            canvas.drawText("冰霜", cdX, cdY + 138f, paint)
        }

        // 右上角返回主界面按钮（已放大3倍）
        bgPaint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(RectF(backBtnX, backBtnY, backBtnX + backBtnWidth, backBtnY + backBtnHeight), 20f, 20f, bgPaint)
        paint.color = Color.parseColor("#FF6666")
        paint.textSize = 78f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("返回", backBtnX + backBtnWidth / 2f, backBtnY + 105f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

}