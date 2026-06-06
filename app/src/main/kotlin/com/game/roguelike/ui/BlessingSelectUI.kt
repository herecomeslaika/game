package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.color
import com.game.roguelike.core.icon

class BlessingSelectUI {
    private var w = 1920f
    private var h = 1080f
    private var itemHeight = 136f
    private var itemWidth = 960f
    private var itemGap = 24f
    private var listX = 0f
    private var listY = 0f
    private var panelTop = 0f
    private var panelBottom = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
    }

    fun updateLayout(screenW: Int, screenH: Int) {
        w = screenW.toFloat()
        h = screenH.toFloat()
        panelTop = h * 0.07f
        panelBottom = h * 0.93f
        itemWidth = (w * 0.62f).coerceIn(760f, 1180f)
        itemHeight = (h * 0.13f).coerceIn(118f, 168f)
        itemGap = (h * 0.024f).coerceIn(18f, 30f)
        listX = w / 2f - itemWidth / 2f
        listY = panelTop + h * 0.16f
    }

    fun update(dt: Float) {}

    fun render(canvas: Canvas, blessings: List<Blessing>) {
        paint.color = Color.argb(205, 10, 15, 30)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val panelLeft = listX - 44f
        val panelRight = listX + itemWidth + 44f

        paint.color = Color.argb(235, 20, 25, 45)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(panelLeft, panelTop, panelRight, panelBottom, 16f, 16f, paint)

        paint.color = Color.parseColor("#5544AA")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawRoundRect(panelLeft, panelTop, panelRight, panelBottom, 16f, 16f, paint)

        titlePaint.textSize = (h * 0.06f).coerceIn(54f, 78f)
        canvas.drawText("选择祝福", w / 2f, panelTop + h * 0.09f, titlePaint)

        for (i in blessings.indices) {
            drawBlessingItem(canvas, blessings[i], i)
        }
    }

    private fun drawBlessingItem(canvas: Canvas, blessing: Blessing, index: Int) {
        val iy = listY + index * (itemHeight + itemGap)
        val rarityColor = when (blessing.rarity) {
            BlessingRarity.COMMON -> Color.parseColor("#FFFFFF")
            BlessingRarity.RARE -> Color.parseColor("#4488FF")
            BlessingRarity.EPIC -> Color.parseColor("#AA44FF")
            BlessingRarity.DUO -> Color.parseColor("#FFD700")
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(160, 40, 35, 60)
        canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 12f, 12f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = rarityColor
        canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 12f, 12f, paint)

        val iconX = listX + 58f
        val iconY = iy + itemHeight / 2f
        paint.style = Paint.Style.FILL
        paint.color = blessing.god.color
        canvas.drawCircle(iconX, iconY, itemHeight * 0.28f, paint)

        namePaint.color = Color.WHITE
        namePaint.textSize = itemHeight * 0.22f
        namePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(blessing.god.icon, iconX, iconY + itemHeight * 0.08f, namePaint)

        namePaint.textAlign = Paint.Align.LEFT
        namePaint.color = rarityColor
        namePaint.textSize = itemHeight * 0.25f
        canvas.drawText(blessing.name, listX + 118f, iy + itemHeight * 0.40f, namePaint)

        descPaint.color = Color.parseColor("#AAAAAA")
        descPaint.textSize = itemHeight * 0.17f
        canvas.drawText(blessing.description, listX + 118f, iy + itemHeight * 0.66f, descPaint)

        if (blessing.rarity == BlessingRarity.DUO) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#FFD700")
            canvas.drawCircle(iconX, iconY, itemHeight * 0.34f, paint)
        }
    }

    fun handleTouch(x: Float, y: Float, blessings: List<Blessing>): Blessing? {
        for (i in blessings.indices) {
            val iy = listY + i * (itemHeight + itemGap)
            if (x >= listX && x <= listX + itemWidth &&
                y >= iy && y <= iy + itemHeight
            ) {
                return blessings[i]
            }
        }
        return null
    }
}
