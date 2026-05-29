package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.GodType
import com.game.roguelike.core.color
import com.game.roguelike.core.icon

class BlessingSelectUI {
    private var w = 1920f
    private var h = 1080f
    private var itemHeight = 80f
    private var itemWidth = 600f
    private var listX = 0f
    private var listY = 0f

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
    }
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 18f
    }

    fun updateLayout(screenW: Int, screenH: Int) {
        w = screenW.toFloat()
        h = screenH.toFloat()
        listX = w / 2f - itemWidth / 2f
        listY = h / 2f - 250f
    }

    fun update(dt: Float) {}

    fun render(canvas: Canvas, blessings: List<Blessing>) {
        // Dark overlay
        paint.color = Color.argb(200, 10, 15, 30)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val panelTop = listY - 80f
        val panelBottom = listY + blessings.size * (itemHeight + 12f) + 30f

        // Panel background
        paint.color = Color.argb(230, 20, 25, 45)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(listX - 30f, panelTop, listX + itemWidth + 30f, panelBottom, 12f, 12f, paint)

        // Panel border
        paint.color = Color.parseColor("#5544AA")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(listX - 30f, panelTop, listX + itemWidth + 30f, panelBottom, 12f, 12f, paint)

        // Title
        canvas.drawText("选择祝福", w / 2f, listY - 35f, titlePaint)

        // Blessing items
        for (i in blessings.indices) {
            val blessing = blessings[i]
            val iy = listY + 10f + i * (itemHeight + 12f)

            val rarityColor = when (blessing.rarity) {
                BlessingRarity.COMMON -> Color.parseColor("#FFFFFF")
                BlessingRarity.RARE -> Color.parseColor("#4488FF")
                BlessingRarity.EPIC -> Color.parseColor("#AA44FF")
                BlessingRarity.DUO -> Color.parseColor("#FFD700")
            }

            // Item bg
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(150, 40, 35, 60)
            canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 8f, 8f, paint)

            // Rarity border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = rarityColor
            canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 8f, 8f, paint)

            // God icon circle
            paint.style = Paint.Style.FILL
            paint.color = blessing.god.color
            canvas.drawCircle(listX + 35f, iy + itemHeight / 2f, 22f, paint)

            // God icon text
            namePaint.color = Color.WHITE
            namePaint.textSize = 18f
            canvas.drawText(blessing.god.icon, listX + 27f, iy + itemHeight / 2f + 7f, namePaint)

            // Blessing name
            namePaint.color = rarityColor
            namePaint.textSize = 24f
            canvas.drawText(blessing.name, listX + 70f, iy + 32f, namePaint)

            // Blessing description
            descPaint.color = Color.parseColor("#AAAAAA")
            descPaint.textSize = 16f
            canvas.drawText(blessing.description, listX + 70f, iy + 55f, descPaint)

            // Duo star indicator
            if (blessing.rarity == BlessingRarity.DUO) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.parseColor("#FFD700")
                canvas.drawCircle(listX + 35f, iy + itemHeight / 2f, 28f, paint)
            }
        }
    }

    fun handleTouch(x: Float, y: Float, blessings: List<Blessing>): Blessing? {
        for (i in blessings.indices) {
            val iy = listY + 10f + i * (itemHeight + 12f)
            if (x >= listX && x <= listX + itemWidth &&
                y >= iy && y <= iy + itemHeight
            ) {
                return blessings[i]
            }
        }
        return null
    }
}
