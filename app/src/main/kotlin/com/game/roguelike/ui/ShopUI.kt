package com.game.roguelike.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.game.roguelike.shop.Shop
import com.game.roguelike.shop.ShopItem
import com.game.roguelike.shop.ShopItemRarity

enum class ShopTouchResult {
    PURCHASED, CANT_AFFORD, OUTSIDE
}

class ShopUI {
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

    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 18f
    }

    private val costPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val soldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val closePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4444")
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val failedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4444")
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    var purchaseFailedTimer = 0f

    fun updateLayout(screenW: Int, screenH: Int) {
        w = screenW.toFloat()
        h = screenH.toFloat()
        listX = w / 2f - itemWidth / 2f
        listY = h / 2f - 250f
    }

    fun update(dt: Float) {
        if (purchaseFailedTimer > 0) purchaseFailedTimer -= dt
    }

    fun render(canvas: Canvas, shop: Shop, currentGold: Int) {
        // Dark overlay
        paint.color = Color.argb(200, 20, 15, 10)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val panelTop = listY - 80f
        val panelBottom = listY + shop.items.size * (itemHeight + 15f) + 120f

        // Shop panel
        paint.color = Color.argb(230, 40, 30, 20)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(listX - 30f, panelTop, listX + itemWidth + 30f, panelBottom, 12f, 12f, paint)

        // Border
        paint.color = Color.parseColor("#886633")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(listX - 30f, panelTop, listX + itemWidth + 30f, panelBottom, 12f, 12f, paint)

        // Title
        canvas.drawText("商人", w / 2f, listY - 35f, titlePaint)

        // Gold display
        goldPaint.textSize = 30f
        goldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("金币: $currentGold", listX, listY - 10f, goldPaint)

        // Items
        for (i in shop.items.indices) {
            val item = shop.items[i]
            val iy = listY + 25f + i * (itemHeight + 15f)
            val isAffordable = currentGold >= item.cost && !item.sold

            // Rarity color mapping
            val rarityColor = when (item.rarity) {
                ShopItemRarity.COMMON -> Color.parseColor("#FFFFFF")
                ShopItemRarity.RARE -> Color.parseColor("#4488FF")
                ShopItemRarity.EPIC -> Color.parseColor("#FFD700")
            }

            // Item background
            if (item.sold) {
                paint.color = Color.argb(80, 40, 40, 40)
            } else if (!isAffordable) {
                paint.color = Color.argb(100, 60, 30, 30)
            } else {
                paint.color = Color.argb(150, 60, 50, 30)
            }
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 8f, 8f, paint)

            // Item border with rarity tint
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (item.rarity == ShopItemRarity.EPIC && !item.sold) 3f else 2f
            paint.color = if (item.sold) Color.argb(60, 80, 80, 80) else rarityColor
            canvas.drawRoundRect(listX, iy, listX + itemWidth, iy + itemHeight, 8f, 8f, paint)

            if (item.sold) {
                // Sold: gray out name + show "已购买"
                namePaint.color = Color.parseColor("#666666")
                canvas.drawText(item.name, listX + 15f, iy + 30f, namePaint)
                descPaint.color = Color.parseColor("#555555")
                canvas.drawText(item.description, listX + 15f, iy + 55f, descPaint)
                soldPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("已购买", listX + itemWidth - 120f, iy + 45f, soldPaint)
            } else {
                // Name with rarity color
                namePaint.color = rarityColor
                canvas.drawText(item.name, listX + 15f, iy + 30f, namePaint)

                // Description
                descPaint.color = if (isAffordable) Color.parseColor("#AAAAAA") else Color.parseColor("#886666")
                canvas.drawText(item.description, listX + 15f, iy + 55f, descPaint)

                // Cost
                costPaint.color = if (isAffordable) rarityColor else Color.parseColor("#AA6666")
                canvas.drawText("${item.cost}g", listX + itemWidth - 70f, iy + 35f, costPaint)

                // Insufficient gold hint
                if (!isAffordable) {
                    descPaint.color = Color.parseColor("#FF6666")
                    descPaint.textSize = 14f
                    canvas.drawText("金币不足", listX + itemWidth - 70f, iy + 58f, descPaint)
                    descPaint.textSize = 18f
                }
            }
        }

        // Purchase failed flash message
        if (purchaseFailedTimer > 0) {
            failedPaint.color = Color.argb(
                (purchaseFailedTimer * 255f / 0.8f).toInt().coerceIn(0, 255),
                255, 68, 68
            )
            canvas.drawText("金币不足!", w / 2f, listY + 25f + shop.items.size * (itemHeight + 15f) + 55f, failedPaint)
        }

        // Close button
        canvas.drawText("点击外部关闭", w / 2f, listY + 25f + shop.items.size * (itemHeight + 15f) + 100f, closePaint)
    }

    fun handleTouch(x: Float, y: Float, shop: Shop, currentGold: Int): Pair<ShopItem?, ShopTouchResult> {
        for (i in shop.items.indices) {
            val item = shop.items[i]
            if (item.sold) continue
            val iy = listY + 25f + i * (itemHeight + 15f)
            if (x >= listX && x <= listX + itemWidth &&
                y >= iy && y <= iy + itemHeight
            ) {
                return if (currentGold >= item.cost) Pair(item, ShopTouchResult.PURCHASED) else Pair(item, ShopTouchResult.CANT_AFFORD)
            }
        }
        return Pair(null, ShopTouchResult.OUTSIDE)
    }
}