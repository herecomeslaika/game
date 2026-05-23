package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.blessing.BlessingRarity

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
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }
    private val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    fun updateLayout(screenW: Int, screenH: Int) {
        w = screenW.toFloat()
        h = screenH.toFloat()
        cardY = h / 2f - cardHeight / 2f
    }

    fun update(dt: Float) {}

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
        // Card background
        val bgColor = when (blessing.rarity) {
            BlessingRarity.COMMON -> Color.argb(220, 40, 40, 50)
            BlessingRarity.RARE -> Color.argb(220, 30, 30, 80)
            BlessingRarity.EPIC -> Color.argb(220, 60, 30, 60)
        }
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(x, y, x + cardWidth, y + cardHeight, 15f, 15f, paint)

        // Card border
        val borderColor = when (blessing.rarity) {
            BlessingRarity.COMMON -> Color.parseColor("#888888")
            BlessingRarity.RARE -> Color.parseColor("#4488FF")
            BlessingRarity.EPIC -> Color.parseColor("#FF44FF")
        }
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(x, y, x + cardWidth, y + cardHeight, 15f, 15f, paint)

        // Type icon
        typePaint.color = when (blessing.type) {
            com.game.roguelike.core.BlessingType.ATTACK -> Color.parseColor("#FF4444")
            com.game.roguelike.core.BlessingType.SPECIAL -> Color.parseColor("#4444FF")
            com.game.roguelike.core.BlessingType.DASH -> Color.parseColor("#44FF44")
            com.game.roguelike.core.BlessingType.SUPPORT -> Color.parseColor("#FFAA44")
        }
        val typeLabel = when (blessing.type) {
            com.game.roguelike.core.BlessingType.ATTACK -> "攻击"
            com.game.roguelike.core.BlessingType.SPECIAL -> "技能"
            com.game.roguelike.core.BlessingType.DASH -> "冲刺"
            com.game.roguelike.core.BlessingType.SUPPORT -> "辅助"
        }
        canvas.drawText(typeLabel, x + cardWidth / 2f, y + 40f, typePaint)

        // Icon circle
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x + cardWidth / 2f, y + 100f, 35f, typePaint)

        // Rarity label
        val rarityText = when (blessing.rarity) {
            BlessingRarity.COMMON -> "普通"
            BlessingRarity.RARE -> "稀有"
            BlessingRarity.EPIC -> "史诗"
        }
        typePaint.color = borderColor
        canvas.drawText(rarityText, x + cardWidth / 2f, y + 160f, typePaint)

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
