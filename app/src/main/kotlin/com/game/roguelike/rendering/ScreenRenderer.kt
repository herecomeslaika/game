package com.game.roguelike.rendering

import android.content.Context
import android.graphics.*
import com.game.roguelike.R
import kotlin.math.sin
import kotlin.math.min

class ScreenRenderer(private val renderer: IsometricRenderer, private val context: Context) {

    // 加载主界面人物图片
    private val menuPlayerBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.people)

    // 菜单按钮点击区域
    val startBtnRect = RectF()
    val exitBtnRect = RectF()

    fun renderMenu(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // 左侧狂野暗黑风格艺术字标题
        renderer.titlePaint.textSize = 190f
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.textAlign = Paint.Align.LEFT
        // 暗黑风格：暗红色+粗黑描边+文字阴影
        renderer.titlePaint.setShadowLayer(15f, 6f, 6f, Color.BLACK)
        renderer.titlePaint.color = Color.parseColor("#990000")
        renderer.titlePaint.style = Paint.Style.FILL_AND_STROKE
        renderer.titlePaint.strokeWidth = 7f
        canvas.drawText("冥途", w * 0.08f, h * 0.32f, renderer.titlePaint)
        // 重置画笔样式避免影响其他绘制
        renderer.titlePaint.clearShadowLayer()
        renderer.titlePaint.strokeWidth = 0f
        renderer.titlePaint.style = Paint.Style.FILL

        // 菜单按钮列表 - 整体下移+增大上下间距
        val btnX = w * 0.08f
        var btnY = h * 0.5f
        val btnSpacing = 170f
        val btnWidth = 600f
        val btnHeight = 80f

        // 开始游戏按钮 - 去掉金色底纹，仅文字闪烁效果
        val alpha = ((sin(renderer.globalTime * 2f) + 1) * 127 + 128).toInt()
        renderer.subtitlePaint.color = Color.argb(alpha, 255, 215, 0)
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.textSize = 82f
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        startBtnRect.set(btnX - 20f, btnY - 60f, btnX + btnWidth, btnY + 20f)
        canvas.drawText("开始游戏", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        // 选项按钮（暂不实现）
        renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
        canvas.drawText("选项", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        // 退出游戏按钮
        exitBtnRect.set(btnX - 20f, btnY - 60f, btnX + btnWidth, btnY + 20f)
        canvas.drawText("退出游戏", btnX, btnY, renderer.subtitlePaint)

        // ========== 右侧替换为自定义人物图片 ==========
        val cx = w * 0.78f
        val cy = h * 0.52f
        val bitmapWidth = 1480f
        val bitmapHeight = 2060f
        val left = cx - bitmapWidth / 2
        val top = cy - bitmapHeight / 2
        val dstRect = RectF(left, top, left + bitmapWidth, top + bitmapHeight)
        canvas.drawBitmap(menuPlayerBitmap, null, dstRect, renderer.paint)

        // 底部版本信息
        renderer.subtitlePaint.textSize = 30f
        renderer.subtitlePaint.color = Color.parseColor("#666666")
        canvas.drawText("冥途 v1.0", w * 0.08f, h * 0.92f, renderer.subtitlePaint)

        // 恢复画笔默认设置
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT
        renderer.subtitlePaint.textSize = 54f
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(200, 20, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        renderer.titlePaint.color = Color.parseColor("#FF2222")
        canvas.drawText("游戏结束", w / 2f, h * 0.4f, renderer.titlePaint)

        renderer.subtitlePaint.color = Color.parseColor("#AA8888")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, renderer.subtitlePaint)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(200, 0, 20, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        renderer.titlePaint.color = Color.parseColor("#FFD700")
        canvas.drawText("通关!", w / 2f, h * 0.4f, renderer.titlePaint)

        renderer.subtitlePaint.color = Color.parseColor("#DDAA66")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, renderer.subtitlePaint)
    }

    fun drawFade(canvas: Canvas, alpha: Float) {
        renderer.paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, renderer.screenWidth.toFloat(), renderer.screenHeight.toFloat(), renderer.paint)
    }

    fun renderBossEntrance(canvas: Canvas, bossName: String, bossTitle: String, timer: Float, phase: Int, w: Int, h: Int) {
        when (phase) {
            0 -> {
                // Phase 0: dark overlay fading in
                val overlayAlpha = min(timer / 1.5f, 1f) * 180f
                renderer.paint.color = Color.argb(overlayAlpha.toInt(), 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Center glow
                if (timer > 0.5f) {
                    val glowAlpha = min((timer - 0.5f) / 1f, 1f) * 60f
                    renderer.paint.color = Color.argb(glowAlpha.toInt(), 170, 68, 255)
                    val glowRadius = 80f + timer * 60f
                    canvas.drawCircle(w / 2f, h / 2f, glowRadius, renderer.paint)
                }
            }
            1 -> {
                // Phase 1: dark overlay + boss name with gold outline
                renderer.paint.color = Color.argb(180, 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Glow behind name
                val glowPulse = (sin(timer * 4f) + 1f) * 0.5f
                renderer.paint.color = Color.argb((40 + glowPulse * 30).toInt(), 170, 68, 255)
                canvas.drawCircle(w / 2f, h * 0.42f, 120f + glowPulse * 30f, renderer.paint)

                // Boss name — scale from large to normal
                val nameProgress = min(timer / 0.6f, 1f)
                val nameScale = 1f + (1f - nameProgress) * 0.8f
                val nameAlpha = (nameProgress * 255).toInt()

                canvas.save()
                canvas.scale(nameScale, nameScale, w / 2f, h * 0.42f)

                // Gold outline
                renderer.titlePaint.color = Color.argb(nameAlpha, 255, 215, 0)
                renderer.titlePaint.style = Paint.Style.FILL_AND_STROKE
                renderer.titlePaint.strokeWidth = 4f
                canvas.drawText(bossName, w / 2f, h * 0.42f, renderer.titlePaint)
                renderer.titlePaint.style = Paint.Style.FILL
                renderer.titlePaint.strokeWidth = 0f

                canvas.restore()

                // Title text (subtitle)
                if (timer > 0.4f) {
                    val titleProgress = min((timer - 0.4f) / 0.5f, 1f)
                    val titleAlpha = (titleProgress * 200).toInt()
                    renderer.subtitlePaint.color = Color.argb(titleAlpha, 170, 130, 200)
                    canvas.drawText(bossTitle, w / 2f, h * 0.52f, renderer.subtitlePaint)
                }

                // Decorative lines
                val lineAlpha = (nameProgress * 120).toInt()
                renderer.paint.color = Color.argb(lineAlpha, 170, 68, 255)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 2f
                val lineW = min(timer / 0.8f, 1f) * w * 0.35f
                canvas.drawLine(w / 2f - lineW, h * 0.47f, w / 2f + lineW, h * 0.47f, renderer.paint)
                renderer.paint.style = Paint.Style.FILL
                renderer.paint.strokeWidth = 0f
            }
            2 -> {
                // Phase 2: fade out
                val fadeOut = min(timer / 0.5f, 1f)
                val overlayAlpha = 180f * (1f - fadeOut)
                renderer.paint.color = Color.argb(overlayAlpha.toInt(), 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Fading name
                val nameAlpha = ((1f - fadeOut) * 255).toInt()
                if (nameAlpha > 0) {
                    renderer.titlePaint.color = Color.argb(nameAlpha, 255, 215, 0)
                    canvas.drawText(bossName, w / 2f, h * 0.42f, renderer.titlePaint)

                    renderer.subtitlePaint.color = Color.argb((nameAlpha * 0.78f).toInt(), 170, 130, 200)
                    canvas.drawText(bossTitle, w / 2f, h * 0.52f, renderer.subtitlePaint)
                }
            }
        }
    }
}
