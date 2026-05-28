package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.GodType
import com.game.roguelike.core.color
import com.game.roguelike.core.icon
import com.game.roguelike.core.displayName

class BlessingSelectUI {
    private var w = 1920f
    private var h = 1080f
    private var cardWidth = 250f
    private var cardHeight = 350f
    private var cardY = 0f
    private var cards = listOf<Pair<Float, Blessing>>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 48f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }
    private val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private var animTime = 0f

    fun updateLayout(screenW: Int, screenH: Int) {
        w = screenW.toFloat()
        h = screenH.toFloat()
        cardY = h / 2f - cardHeight / 2f
    }

    fun update(dt: Float) {
        animTime += dt
    }

    fun render(canvas: Canvas, offerings: List<Blessing>) {
        // Dark overlay
        paint.color = Color.argb(180, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        // Title
        canvas.drawText("选择祝福", w / 2f, cardY - 40f, titlePaint)

        // Calculate card positions
        val totalWidth = offerings.size * cardWidth + (offerings.size - 1) * 30f
        val startX = (w - totalWidth) / 2f

        cards = offerings.mapIndexed { i, blessing ->
            startX + i * (cardWidth + 30f) to blessing
        }

        for ((x, blessing) in cards) {
            drawBlessingCard(canvas, x, cardY, blessing)
        }
    }

    private fun drawBlessingCard(canvas: Canvas, x: Float, y: Float, blessing: Blessing) {
        val isDuo = blessing.rarity == BlessingRarity.DUO

        // Card background
        val bgColor = when (blessing.rarity) {
            BlessingRarity.COMMON -> Color.argb(220, 40, 40, 50)
            BlessingRarity.RARE -> Color.argb(220, 30, 30, 80)
            BlessingRarity.EPIC -> Color.argb(220, 60, 30, 60)
            BlessingRarity.DUO -> Color.argb(220, 50, 30, 70)
        }
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(x, y, x + cardWidth, y + cardHeight, 15f, 15f, paint)

        // Card border
        val borderColor = when (blessing.rarity) {
            BlessingRarity.COMMON -> Color.parseColor("#888888")
            BlessingRarity.RARE -> Color.parseColor("#4488FF")
            BlessingRarity.EPIC -> Color.parseColor("#FF44FF")
            BlessingRarity.DUO -> Color.parseColor("#FFD700")
        }
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (isDuo) 4f else 3f
        canvas.drawRoundRect(x, y, x + cardWidth, y + cardHeight, 15f, 15f, paint)

        // Duo glow effect
        if (isDuo) {
            val glowPulse = (kotlin.math.sin(animTime * 3f) * 0.3f + 0.7f)
            paint.color = Color.argb((60 * glowPulse).toInt(), 255, 215, 0)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(x - 5f, y - 5f, x + cardWidth + 5f, y + cardHeight + 5f, 20f, 20f, paint)
            // Re-draw card over glow
            paint.color = bgColor
            canvas.drawRoundRect(x, y, x + cardWidth, y + cardHeight, 15f, 15f, paint)
            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawRoundRect(x, y, x + cardWidth, y + cardHeight, 15f, 15f, paint)
        }

        paint.strokeWidth = 1f
        paint.style = Paint.Style.FILL

        // God symbol circle
        val godClr = blessing.god.color
        typePaint.color = godClr
        canvas.drawCircle(x + cardWidth / 2f, y + 100f, 35f, typePaint)

        // God icon in circle
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(blessing.god.icon, x + cardWidth / 2f, y + 107f, paint)

        // God name
        typePaint.color = godClr
        typePaint.textSize = 16f
        canvas.drawText(blessing.god.displayName, x + cardWidth / 2f, y + 40f, typePaint)

        // Rarity label
        val rarityText = when (blessing.rarity) {
            BlessingRarity.COMMON -> "普通"
            BlessingRarity.RARE -> "稀有"
            BlessingRarity.EPIC -> "史诗"
            BlessingRarity.DUO -> "DUO!"
        }
        typePaint.color = borderColor
        canvas.drawText(rarityText, x + cardWidth / 2f, y + 160f, typePaint)

        // Duo pair indicator
        if (isDuo && blessing.duoPair != null) {
            val (g1, g2) = blessing.duoPair!!
            typePaint.textSize = 12f
            typePaint.color = Color.argb(180, 255, 215, 0)
            canvas.drawText("${g1.displayName} + ${g2.displayName}", x + cardWidth / 2f, y + 175f, typePaint)
            typePaint.textSize = 16f
        }

        // Name
        namePaint.color = Color.WHITE
        canvas.drawText(blessing.name, x + cardWidth / 2f, y + 210f, namePaint)

        // Description (word wrap)
        val words = blessing.description.split(" ")
        var line = ""
        var lineY = y + 250f
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (descPaint.measureText(test) > cardWidth - 20f) {
                canvas.drawText(line, x + cardWidth / 2f, lineY, descPaint)
                line = word
                lineY += 22f
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x + cardWidth / 2f, lineY, descPaint)
        }
    }

    fun handleTouch(x: Float, y: Float, offerings: List<Blessing>): Blessing? {
        for (i in cards.indices) {
            val (cardX, _) = cards[i]
            if (x >= cardX && x <= cardX + cardWidth &&
                y >= cardY && y <= cardY + cardHeight
            ) {
                return offerings.getOrNull(i)
            }
        }
        return null
    }

}