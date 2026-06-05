package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.color
import com.game.roguelike.core.icon
import com.game.roguelike.entity.Player

class HUD {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var hudX = 0f
    private var hudY = 0f
    private var barWidth = 400f
    private var barHeight = 36f

    private val backBtnRect = RectF()
    private val blessingPanelBtnRect = RectF()
    private val blessingPanelRect = RectF()
    private val blessingCloseRect = RectF()

    var isBlessingPanelOpen = false
        private set

    fun handleBackButtonClick(x: Float, y: Float): Boolean = backBtnRect.contains(x, y)

    fun handleBlessingPanelClick(x: Float, y: Float): Boolean {
        if (blessingPanelBtnRect.contains(x, y)) {
            isBlessingPanelOpen = !isBlessingPanelOpen
            return true
        }
        if (isBlessingPanelOpen && blessingCloseRect.contains(x, y)) {
            isBlessingPanelOpen = false
            return true
        }
        return false
    }

    fun closeBlessingPanel() {
        isBlessingPanelOpen = false
    }

    fun updateLayout(screenW: Int, screenH: Int) {
        hudX = 30f
        hudY = 30f
        barWidth = screenW * 0.35f
        barHeight = 36f

        val backBtnWidth = 190f
        val backBtnHeight = 78f
        backBtnRect.set(
            screenW - backBtnWidth - 24f,
            24f,
            screenW - 24f,
            24f + backBtnHeight
        )

        blessingPanelBtnRect.set(hudX - 8f, hudY + 128f, hudX + 280f, hudY + 210f)

        val panelWidth = screenW * 0.42f
        val panelHeight = screenH * 0.58f
        blessingPanelRect.set(
            32f,
            32f,
            32f + panelWidth,
            32f + panelHeight
        )
        blessingCloseRect.set(
            blessingPanelRect.right - 70f,
            blessingPanelRect.top + 18f,
            blessingPanelRect.right - 20f,
            blessingPanelRect.top + 68f
        )
    }

    fun render(canvas: Canvas, player: Player, gold: Int, blessings: List<Blessing>, layerIndex: Int) {
        bgPaint.color = Color.argb(150, 0, 0, 0)
        paint.style = Paint.Style.FILL

        val hpBgRect = RectF(hudX, hudY, hudX + barWidth, hudY + barHeight)
        canvas.drawRoundRect(hpBgRect, 4f, 4f, bgPaint)

        val hpRatio = player.health.toFloat() / player.maxHealth
        val hpColor = when {
            hpRatio > 0.6f -> Color.parseColor("#44CC44")
            hpRatio > 0.3f -> Color.parseColor("#CCCC44")
            else -> Color.parseColor("#CC4444")
        }
        paint.color = hpColor
        val hpFillRect = RectF(hudX, hudY, hudX + barWidth * hpRatio, hudY + barHeight)
        canvas.drawRoundRect(hpFillRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("生命 ${player.health}/${player.maxHealth}", hudX + 12f, hudY + 26f, paint)

        paint.color = Color.parseColor("#FFD700")
        paint.textSize = 26f
        canvas.drawText("金币: $gold", hudX, hudY + 75f, paint)

        val layerName = when (layerIndex) {
            0 -> "塔耳塔罗斯"
            1 -> "阿斯福德"
            2 -> "伊利西亚"
            else -> "未知"
        }
        paint.color = Color.parseColor("#AABBCC")
        paint.textSize = 24f
        canvas.drawText(layerName, hudX, hudY + 115f, paint)

        renderBlessingStrip(canvas, blessings)
        renderCooldowns(canvas, player)
        renderBackButton(canvas)

        if (isBlessingPanelOpen) {
            renderBlessingPanel(canvas, blessings)
        }
    }

    private fun renderBlessingStrip(canvas: Canvas, blessings: List<Blessing>) {
        bgPaint.color = if (isBlessingPanelOpen) {
            Color.argb(170, 35, 45, 70)
        } else {
            Color.argb(110, 15, 20, 35)
        }
        canvas.drawRoundRect(blessingPanelBtnRect, 14f, 14f, bgPaint)

        paint.color = Color.argb(180, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(blessingPanelBtnRect, 14f, 14f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#DDDDDD")
        paint.textSize = 22f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("祝福 ${blessings.size} 个", hudX + 6f, hudY + 152f, paint)

        var bx = hudX + 8f
        val by = hudY + 185f
        paint.textSize = 20f
        for (blessing in blessings.take(6)) {
            paint.color = blessing.god.color
            canvas.drawCircle(bx + 16f, by, 16f, paint)

            if (blessing.rarity == BlessingRarity.DUO) {
                paint.color = Color.parseColor("#FFD700")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(bx + 16f, by, 24f, paint)
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 1f
            }

            paint.color = Color.WHITE
            canvas.drawText(blessing.god.icon, bx + 8f, by + 8f, paint)
            bx += 44f
        }

        if (blessings.size > 6) {
            paint.color = Color.parseColor("#BBBBBB")
            paint.textSize = 18f
            canvas.drawText("+${blessings.size - 6}", bx + 4f, by + 6f, paint)
        }
    }

    private fun renderCooldowns(canvas: Canvas, player: Player) {
        val cdX = hudX + barWidth + 40f
        val cdY = hudY

        if (player.specialCooldownTimer > 0) {
            val ratio = player.specialCooldownTimer / player.specialCooldown
            canvas.drawRoundRect(RectF(cdX, cdY, cdX + 120f, cdY + 20f), 6f, 6f, bgPaint)
            paint.color = Color.argb(200, 80, 80, 220)
            canvas.drawRoundRect(RectF(cdX, cdY, cdX + 120f * (1f - ratio), cdY + 20f), 6f, 6f, paint)
        } else {
            paint.color = Color.parseColor("#4488FF")
            paint.textSize = 22f
            canvas.drawText("飞刀 就绪", cdX, cdY + 18f, paint)
        }

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

        if (player.athenaShieldActive) {
            paint.color = Color.parseColor("#FFAA44")
            paint.textSize = 22f
            canvas.drawText("神盾 激活", cdX, cdY + 78f, paint)
        }

        if (player.critChance > 0f) {
            paint.color = Color.parseColor("#FF44AA")
            paint.textSize = 22f
            canvas.drawText("暴击${(player.critChance * 100).toInt()}%", cdX, cdY + 108f, paint)
        }

        if (player.slowOnHit > 0f) {
            paint.color = Color.parseColor("#88CCFF")
            paint.textSize = 22f
            canvas.drawText("冰霜", cdX, cdY + 138f, paint)
        }
    }

    private fun renderBackButton(canvas: Canvas) {
        bgPaint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(backBtnRect, 16f, 16f, bgPaint)

        paint.color = Color.argb(180, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(backBtnRect, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#FF8888")
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("返回", backBtnRect.centerX(), backBtnRect.centerY() + 14f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun renderBlessingPanel(canvas: Canvas, blessings: List<Blessing>) {
        bgPaint.color = Color.argb(210, 12, 18, 30)
        canvas.drawRoundRect(blessingPanelRect, 18f, 18f, bgPaint)

        paint.color = Color.argb(200, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(blessingPanelRect, 18f, 18f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("当前祝福", blessingPanelRect.left + 24f, blessingPanelRect.top + 42f, paint)

        paint.color = Color.parseColor("#FF7777")
        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("×", blessingCloseRect.centerX(), blessingCloseRect.centerY() + 10f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT

        if (blessings.isEmpty()) {
            paint.color = Color.parseColor("#AAAAAA")
            paint.textSize = 24f
            canvas.drawText("还没有获得祝福", blessingPanelRect.left + 24f, blessingPanelRect.top + 100f, paint)
            return
        }

        var y = blessingPanelRect.top + 92f
        val itemHeight = 72f
        for (blessing in blessings.take(6)) {
            val itemRect = RectF(
                blessingPanelRect.left + 18f,
                y - 34f,
                blessingPanelRect.right - 18f,
                y + 28f
            )
            bgPaint.color = Color.argb(95, 255, 255, 255)
            canvas.drawRoundRect(itemRect, 12f, 12f, bgPaint)

            paint.color = blessing.god.color
            canvas.drawCircle(itemRect.left + 24f, y - 4f, 14f, paint)

            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(blessing.name, itemRect.left + 48f, y, paint)

            paint.color = when (blessing.rarity) {
                BlessingRarity.COMMON -> Color.parseColor("#DDDDDD")
                BlessingRarity.RARE -> Color.parseColor("#66AAFF")
                BlessingRarity.EPIC -> Color.parseColor("#CC88FF")
                BlessingRarity.DUO -> Color.parseColor("#FFD700")
            }
            paint.textSize = 18f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(blessing.rarity.name, itemRect.right - 90f, y, paint)

            paint.color = Color.parseColor("#BBBBBB")
            paint.textSize = 18f
            canvas.drawText(blessing.description.take(24), itemRect.left + 48f, y + 24f, paint)

            y += itemHeight
        }
    }
}