package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.shop.Shop
import com.game.roguelike.shop.ShopItem

class ShopUI {
    private var w = 1920f
    private var h = 1080f
    private var itemHeight = 60f
    private var itemWidth = 400f
    private var listX = 0f
    private var listY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 16f
    }
    private val costPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val closePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4444")
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    fun updateLayout(screenW: Int, screenH: Int) {
        w = screenW.toFloat()
        h = screenH.toFloat()
        listX = w / 2f - itemWidth / 2f
        listY = h / 2f - 200f
    }

    fun update(dt: Float) {}

    fun render(canvas: Canvas, shop: Shop) {
        // Dark overlay
        paint.color = Color.argb(200, 20, 15, 10)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        // Shop panel
        paint.color = Color.argb(230, 40, 30, 20)
        canvas.drawRoundRect(listX - 20f, listY - 60f, listX + itemWidth + 20f, listY + shop.items.size * (itemHeight + 15f) + 100f, 10f, 10f, paint)

        // Border
        paint.color = Color.parseColor("#886633")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(listX - 20f, listY - 60f, listX + itemWidth + 20f, listY + shop.items.size * (itemHeight + 15f) + 100f, 10f, 10f, paint)

        // Title
        canvas.drawText("商人", w / 2f, listY - 20f, titlePaint)

        // Items
        for (i in shop.items.indices) {
            val item = shop.items[i]
            val iy = listY + i * (itemHeight + 15f)

            // Item background
            paint.color = Color.argb(150, 60, 50, 30)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 8f, 8f, paint)

            // Item border
            paint.color = Color.argb(100, 136, 102, 51)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 8f, 8f, paint)

            // Item name
            canvas.drawText(item.name, listX + 15f, iy + 25f, namePaint)

            // Description
            canvas.drawText(item.description, listX + 15f, iy + 48f, descPaint)

            // Cost
            canvas.drawText("${item.cost}g", listX + itemWidth - 60f, iy + 35f, costPaint)
        }

        // Close button
        canvas.drawText("点击外部关闭", w / 2f, listY + shop.items.size * (itemHeight + 15f) + 80f, closePaint)
    }

    fun handleTouch(x: Float, y: Float, shop: Shop): ShopItem? {
        for (i in shop.items.indices) {
            val iy = listY + i * (itemHeight + 15f)
            if (x >= listX && x <= listX + itemWidth &&
                y >= iy && y <= iy + itemHeight
            ) {
                return shop.items[i]
            }
        }
        return null
    }
}
