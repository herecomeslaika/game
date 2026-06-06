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
    private companion object {
        const val SIZE_SCALE = 0.8f
    }

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
        panelTop = h * 0.156f
        panelBottom = h * 0.844f
        itemWidth = (w * 0.62f * SIZE_SCALE).coerceIn(608f, 944f)
        itemHeight = (h * 0.13f * SIZE_SCALE).coerceIn(94f, 134f)
        itemGap = (h * 0.024f * SIZE_SCALE).coerceIn(14f, 24f)
        listX = w / 2f - itemWidth / 2f
        listY = panelTop + h * 0.128f
    }

    fun update(dt: Float) {}

    fun render(canvas: Canvas, blessings: List<Blessing>) {
        paint.color = Color.argb(205, 10, 15, 30)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val panelPadding = 44f * SIZE_SCALE
        val panelRadius = 16f * SIZE_SCALE
        val panelLeft = listX - panelPadding
        val panelRight = listX + itemWidth + panelPadding

        paint.color = Color.argb(235, 20, 25, 45)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(panelLeft, panelTop, panelRight, panelBottom, panelRadius, panelRadius, paint)

        paint.color = Color.parseColor("#5544AA")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f * SIZE_SCALE
        canvas.drawRoundRect(panelLeft, panelTop, panelRight, panelBottom, panelRadius, panelRadius, paint)

        titlePaint.textSize = (h * 0.06f * SIZE_SCALE).coerceIn(43f, 62f)
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
        canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * SIZE_SCALE
        paint.color = rarityColor
        canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 10f, 10f, paint)

        val iconX = listX + 58f * SIZE_SCALE
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
        val textX = listX + 118f * SIZE_SCALE
        canvas.drawText(blessing.name, textX, iy + itemHeight * 0.40f, namePaint)

        descPaint.color = Color.parseColor("#AAAAAA")
        descPaint.textSize = itemHeight * 0.17f
        canvas.drawText(blessing.description, textX, iy + itemHeight * 0.66f, descPaint)

        if (blessing.rarity == BlessingRarity.DUO) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * SIZE_SCALE
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
