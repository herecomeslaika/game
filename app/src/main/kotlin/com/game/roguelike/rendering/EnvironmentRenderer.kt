package com.game.roguelike.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.game.roguelike.level.Room
import kotlin.math.abs
import kotlin.math.sin

class EnvironmentRenderer(private val renderer: IsometricRenderer) {

    private val p: Paint get() = renderer.paint
    private val useShaders: Boolean get() = renderer.enableShaders

    fun render(canvas: Canvas, room: Room) {
        val layer = room.layerIndex.coerceIn(0, 2)
        val theme = EnvironmentTheme.forLayer(layer)

        drawBackdrop(canvas, theme)
        drawDistantTerrain(canvas, room, theme, layer)

        when (layer) {
            0 -> drawAbyssAtmosphere(canvas, room, theme)
            1 -> drawInfernoAtmosphere(canvas, room, theme)
            else -> drawGardenAtmosphere(canvas, room, theme)
        }
    }

    private fun drawBackdrop(canvas: Canvas, theme: EnvironmentTheme) {
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(
                0f,
                0f,
                0f,
                renderer.screenHeight.toFloat(),
                theme.skyTop,
                theme.skyBottom
            )
        } else {
            p.color = theme.skyBottom
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, renderer.screenWidth.toFloat(), renderer.screenHeight.toFloat(), p)
        p.shader = null
    }

    private fun drawDistantTerrain(canvas: Canvas, room: Room, theme: EnvironmentTheme, layer: Int) {
        val margin = 9
        for (row in -margin until room.height + margin step 2) {
            for (col in -margin until room.width + margin step 2) {
                if (col in 0 until room.width && row in 0 until room.height) continue

                val distance = distanceOutsideRoom(col, row, room)
                if (distance > margin) continue

                val hash = stableHash(col, row, layer)
                if (hash % 7 > 3) continue

                val wx = col * renderer.tileWidth + renderer.tileWidth / 2f
                val wy = row * renderer.tileHeight + renderer.tileHeight / 2f
                val (sx, sy) = renderer.worldToScreen(wx, wy)
                if (sx < -160f || sx > renderer.screenWidth + 160f ||
                    sy < -120f || sy > renderer.screenHeight + 140f
                ) continue

                val base = if ((col + row) % 4 == 0) theme.distantGroundAlt else theme.distantGround
                val alpha = (88 - distance * 8).coerceIn(18, 88)
                drawDiamond(canvas, sx, sy, base, alpha, 1f)

                if (hash % 13 == 0) {
                    drawDistantBlock(canvas, sx, sy, theme.accent, (alpha * 0.75f).toInt())
                }
            }
        }
    }

    private fun drawAbyssAtmosphere(canvas: Canvas, room: Room, theme: EnvironmentTheme) {
        val time = renderer.globalTime
        for (i in 0 until 8) {
            val x = ((i * 257f + time * 9f) % (renderer.screenWidth + 240f)) - 120f
            val y = renderer.screenHeight * (0.16f + i * 0.09f) + sin(time * 0.8f + i) * 10f
            reset()
            p.color = Color.argb(24, Color.red(theme.glow), Color.green(theme.glow), Color.blue(theme.glow))
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.5f
            canvas.drawLine(x - 90f, y, x + 90f, y + 18f, p)
        }

        for (i in 0 until 5) {
            val col = -7 + i * 10
            val row = room.height + 4 + (i % 3)
            val (sx, sy) = renderer.worldToScreen(
                col * renderer.tileWidth + renderer.tileWidth / 2f,
                row * renderer.tileHeight + renderer.tileHeight / 2f
            )
            drawGlow(canvas, sx, sy, 72f + i * 8f, theme.glow, 24)
        }

        drawGrave(canvas, -5, 6, 0.95f, theme)
        drawGrave(canvas, 5, room.height + 4, 0.82f, theme)
        drawGrave(canvas, room.width + 4, 7, 0.9f, theme)
        drawGrave(canvas, room.width - 4, -5, 0.72f, theme)
        drawBonePile(canvas, -6, room.height - 2, 0.9f)
        drawBonePile(canvas, room.width + 5, room.height - 4, 0.78f)
        drawBonePile(canvas, 10, -6, 0.7f)
        drawGhost(canvas, -8, 12, 0.85f, theme, 0)
        drawGhost(canvas, room.width + 7, 11, 0.76f, theme, 1)
        drawGhost(canvas, 15, room.height + 6, 0.68f, theme, 2)
    }

    private fun drawInfernoAtmosphere(canvas: Canvas, room: Room, theme: EnvironmentTheme) {
        val time = renderer.globalTime
        drawLavaPool(canvas, -7, 8, 0, theme)
        drawLavaPool(canvas, 8, room.height + 5, 1, theme)
        drawLavaPool(canvas, room.width + 5, 8, 2, theme)
        drawLavaPool(canvas, room.width - 6, -5, 3, theme)
        drawLavaPool(canvas, room.width + 3, room.height - 3, 4, theme)
        drawLavaBeast(canvas, -7, 8, 0.9f, theme, 0)
        drawLavaBeast(canvas, room.width + 5, 8, 0.76f, theme, 1)
        drawLavaBeast(canvas, room.width + 3, room.height - 3, 0.68f, theme, 2)

        for (i in 0 until 24) {
            val x = ((i * 83f + time * (24f + i % 5)) % (renderer.screenWidth + 80f)) - 40f
            val y = renderer.screenHeight - ((i * 47f + time * 38f) % (renderer.screenHeight + 80f))
            val size = 1.5f + (i % 4)
            reset()
            p.color = Color.argb(80, 255, 116, 42)
            p.style = Paint.Style.FILL
            canvas.drawCircle(x, y, size, p)
        }
    }

    private fun drawGardenAtmosphere(canvas: Canvas, room: Room, theme: EnvironmentTheme) {
        drawWaterPatch(canvas, -6, 4, 0, theme)
        drawWaterPatch(canvas, room.width + 5, 8, 1, theme)
        drawWaterPatch(canvas, 4, room.height + 5, 2, theme)
        drawWaterPatch(canvas, room.width - 3, -6, 3, theme)
        drawCastle(canvas, -8, 11, 0.82f, theme)
        drawCastle(canvas, room.width + 8, 9, 0.74f, theme)
        drawCastle(canvas, room.width - 4, -7, 0.58f, theme)
        drawBanner(canvas, -5, room.height - 3, 0.88f, theme, 0)
        drawBanner(canvas, room.width + 4, room.height - 5, 0.8f, theme, 1)
        drawBanner(canvas, 9, -6, 0.72f, theme, 2)

        val time = renderer.globalTime
        for (i in 0 until 18) {
            val x = ((i * 101f + sin(time + i) * 22f) % (renderer.screenWidth + 120f)) - 60f
            val y = ((i * 59f + time * 12f) % (renderer.screenHeight + 80f)) - 40f
            reset()
            p.color = Color.argb(58, Color.red(theme.glow), Color.green(theme.glow), Color.blue(theme.glow))
            p.style = Paint.Style.FILL
            canvas.drawCircle(x, y, 2.2f + (i % 3), p)
        }
    }

    private fun drawGrave(canvas: Canvas, col: Int, row: Int, scale: Float, theme: EnvironmentTheme) {
        val (sx, sy) = tileToScreen(col, row)
        if (!isVisible(sx, sy, 160f)) return

        drawDiamond(canvas, sx, sy + 8f * scale, renderer.darken(theme.distantGround, 0.72f), 115, 0.72f * scale)
        reset()
        p.color = Color.argb(178, 96, 91, 126)
        p.style = Paint.Style.FILL
        canvas.drawRoundRect(sx - 12f * scale, sy - 34f * scale, sx + 12f * scale, sy + 4f * scale, 5f * scale, 5f * scale, p)
        reset()
        p.color = Color.argb(210, 158, 150, 190)
        canvas.drawRoundRect(sx - 9f * scale, sy - 39f * scale, sx + 9f * scale, sy - 24f * scale, 7f * scale, 7f * scale, p)
        reset()
        p.color = Color.argb(95, 20, 16, 34)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * scale
        canvas.drawLine(sx, sy - 29f * scale, sx, sy - 12f * scale, p)
        canvas.drawLine(sx - 6f * scale, sy - 22f * scale, sx + 6f * scale, sy - 22f * scale, p)
        drawGlow(canvas, sx, sy - 18f * scale, 32f * scale, theme.glow, 18)
    }

    private fun drawBonePile(canvas: Canvas, col: Int, row: Int, scale: Float) {
        val (sx, sy) = tileToScreen(col, row)
        if (!isVisible(sx, sy, 150f)) return

        drawDiamond(canvas, sx, sy + 6f * scale, Color.rgb(22, 18, 28), 105, 0.65f * scale)
        drawBone(canvas, sx - 16f * scale, sy - 5f * scale, sx + 16f * scale, sy + 6f * scale, scale)
        drawBone(canvas, sx - 7f * scale, sy + 9f * scale, sx + 20f * scale, sy - 11f * scale, scale * 0.86f)
        drawBone(canvas, sx - 22f * scale, sy + 7f * scale, sx + 5f * scale, sy + 16f * scale, scale * 0.72f)
    }

    private fun drawBone(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, scale: Float) {
        reset()
        p.color = Color.argb(190, 210, 204, 184)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f * scale
        p.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x0, y0, x1, y1, p)
        reset()
        p.color = Color.argb(215, 230, 224, 203)
        p.style = Paint.Style.FILL
        val r = 4.2f * scale
        canvas.drawCircle(x0, y0, r, p)
        canvas.drawCircle(x1, y1, r, p)
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun drawGhost(canvas: Canvas, col: Int, row: Int, scale: Float, theme: EnvironmentTheme, index: Int) {
        val (sx, baseY) = tileToScreen(col, row)
        if (!isVisible(sx, baseY, 180f)) return

        val bob = sin(renderer.globalTime * 1.7f + index) * 5f * scale
        val sy = baseY + bob
        drawGlow(canvas, sx, sy - 18f * scale, 54f * scale, theme.glow, 45)

        val path = renderer.obtainPath()
        val w = 17f * scale
        val h = 38f * scale
        path.moveTo(sx - w, sy - h * 0.1f)
        path.cubicTo(sx - w * 1.1f, sy - h * 0.78f, sx - w * 0.55f, sy - h, sx, sy - h)
        path.cubicTo(sx + w * 0.55f, sy - h, sx + w * 1.1f, sy - h * 0.78f, sx + w, sy - h * 0.1f)
        path.lineTo(sx + w * 0.58f, sy + h * 0.18f)
        path.lineTo(sx + w * 0.18f, sy + h * 0.03f)
        path.lineTo(sx - w * 0.24f, sy + h * 0.18f)
        path.lineTo(sx - w * 0.62f, sy + h * 0.02f)
        path.close()

        reset()
        p.color = Color.argb(132, 94, 255, 170)
        p.style = Paint.Style.FILL
        canvas.drawPath(path, p)
        reset()
        p.color = Color.argb(180, 192, 255, 220)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * scale
        canvas.drawPath(path, p)
        renderer.recyclePath(path)

        reset()
        p.color = Color.argb(190, 9, 40, 28)
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 5f * scale, sy - 23f * scale, 2.2f * scale, p)
        canvas.drawCircle(sx + 5f * scale, sy - 23f * scale, 2.2f * scale, p)
    }

    private fun drawLavaPool(canvas: Canvas, col: Int, row: Int, index: Int, theme: EnvironmentTheme) {
        drawLavaCell(canvas, col, row, index, theme, 1.28f)
        drawLavaCell(canvas, col + 1, row, index + 1, theme, 1.12f)
        drawLavaCell(canvas, col - 1, row + 1, index + 2, theme, 1.08f)
        drawLavaCell(canvas, col + 2, row + 1, index + 3, theme, 0.96f)
        drawLavaCell(canvas, col, row + 2, index + 4, theme, 0.9f)

        val (sx, sy) = tileToScreen(col, row)
        if (!isVisible(sx, sy, 220f)) return
        reset()
        p.color = Color.argb(95, 255, 188, 62)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.2f
        val wave = sin(renderer.globalTime * 2.4f + index) * 6f
        canvas.drawLine(sx - 48f, sy + wave, sx + 64f, sy - 14f - wave, p)
        canvas.drawLine(sx - 16f, sy + 22f - wave, sx + 78f, sy + 4f + wave, p)
    }

    private fun drawLavaCell(canvas: Canvas, col: Int, row: Int, phase: Int, theme: EnvironmentTheme, scale: Float) {
        val (sx, sy) = tileToScreen(col, row)
        if (!isVisible(sx, sy, 170f)) return

        val pulse = ((sin(renderer.globalTime * 2.6f + phase * 0.73f) + 1f) * 0.5f)
        drawGlow(canvas, sx, sy, 72f * scale, theme.glow, (46 + pulse * 42f).toInt())
        drawDiamond(canvas, sx, sy, Color.rgb(79, 18, 6), 190, 1.05f * scale)
        drawDiamond(canvas, sx, sy, theme.accent, (145 + pulse * 80f).toInt(), 0.82f * scale)
        drawDiamond(canvas, sx, sy - 2f * scale, theme.glow, (75 + pulse * 65f).toInt(), 0.48f * scale)
    }

    private fun drawLavaBeast(canvas: Canvas, col: Int, row: Int, scale: Float, theme: EnvironmentTheme, index: Int) {
        val (sx, baseY) = tileToScreen(col, row)
        if (!isVisible(sx, baseY, 180f)) return

        val rise = sin(renderer.globalTime * 1.35f + index) * 4f * scale
        val sy = baseY - 10f * scale + rise
        drawGlow(canvas, sx, sy, 58f * scale, theme.accent, 46)
        reset()
        p.color = Color.argb(220, 42, 12, 8)
        p.style = Paint.Style.FILL
        canvas.drawOval(sx - 22f * scale, sy - 13f * scale, sx + 22f * scale, sy + 17f * scale, p)
        drawTriangle(canvas, sx - 16f * scale, sy - 10f * scale, sx - 30f * scale, sy - 28f * scale, sx - 6f * scale, sy - 18f * scale, Color.argb(215, 35, 9, 7))
        drawTriangle(canvas, sx + 16f * scale, sy - 10f * scale, sx + 30f * scale, sy - 28f * scale, sx + 6f * scale, sy - 18f * scale, Color.argb(215, 35, 9, 7))
        reset()
        p.color = Color.argb(245, 255, 214, 70)
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 8f * scale, sy - 2f * scale, 3f * scale, p)
        canvas.drawCircle(sx + 8f * scale, sy - 2f * scale, 3f * scale, p)
        reset()
        p.color = Color.argb(165, 255, 86, 34)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * scale
        canvas.drawLine(sx - 12f * scale, sy + 8f * scale, sx + 12f * scale, sy + 9f * scale, p)
    }

    private fun drawCastle(canvas: Canvas, col: Int, row: Int, scale: Float, theme: EnvironmentTheme) {
        val (sx, sy) = tileToScreen(col, row)
        if (!isVisible(sx, sy, 220f)) return

        drawDiamond(canvas, sx, sy + 10f * scale, renderer.darken(theme.distantGroundAlt, 0.72f), 125, 1.28f * scale)
        val wall = Color.argb(188, 85, 116, 93)
        val light = Color.argb(185, 146, 180, 142)
        reset()
        p.color = wall
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 42f * scale, sy - 34f * scale, sx + 42f * scale, sy + 8f * scale, p)
        canvas.drawRect(sx - 55f * scale, sy - 55f * scale, sx - 30f * scale, sy + 8f * scale, p)
        canvas.drawRect(sx + 30f * scale, sy - 55f * scale, sx + 55f * scale, sy + 8f * scale, p)
        canvas.drawRect(sx - 13f * scale, sy - 66f * scale, sx + 13f * scale, sy + 8f * scale, p)

        reset()
        p.color = light
        p.style = Paint.Style.FILL
        for (i in -2..2) {
            canvas.drawRect(sx + i * 17f * scale - 5f * scale, sy - 42f * scale, sx + i * 17f * scale + 5f * scale, sy - 33f * scale, p)
        }
        canvas.drawRect(sx - 59f * scale, sy - 63f * scale, sx - 26f * scale, sy - 54f * scale, p)
        canvas.drawRect(sx + 26f * scale, sy - 63f * scale, sx + 59f * scale, sy - 54f * scale, p)
        drawGlow(canvas, sx, sy - 24f * scale, 74f * scale, theme.glow, 18)
    }

    private fun drawBanner(canvas: Canvas, col: Int, row: Int, scale: Float, theme: EnvironmentTheme, index: Int) {
        val (sx, sy) = tileToScreen(col, row)
        if (!isVisible(sx, sy, 180f)) return

        val wave = sin(renderer.globalTime * 2.1f + index) * 5f * scale
        reset()
        p.color = Color.argb(190, 174, 184, 148)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * scale
        p.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(sx, sy + 10f * scale, sx, sy - 48f * scale, p)

        val flag = renderer.obtainPath()
        flag.moveTo(sx + 2f * scale, sy - 47f * scale)
        flag.lineTo(sx + 36f * scale, sy - 39f * scale + wave)
        flag.lineTo(sx + 16f * scale, sy - 27f * scale)
        flag.lineTo(sx + 2f * scale, sy - 31f * scale)
        flag.close()
        reset()
        p.color = Color.argb(205, Color.red(theme.glow), Color.green(theme.glow), Color.blue(theme.glow))
        p.style = Paint.Style.FILL
        canvas.drawPath(flag, p)
        reset()
        p.color = Color.argb(125, 255, 255, 230)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * scale
        canvas.drawPath(flag, p)
        renderer.recyclePath(flag)
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun drawWaterPatch(canvas: Canvas, col: Int, row: Int, index: Int, theme: EnvironmentTheme) {
        val (sx, sy) = renderer.worldToScreen(
            col * renderer.tileWidth + renderer.tileWidth / 2f,
            row * renderer.tileHeight + renderer.tileHeight / 2f
        )
        drawGlow(canvas, sx, sy, 82f, theme.glow, 28)
        drawDiamond(canvas, sx, sy, Color.rgb(25, 87, 104), 150, 1.2f)
        drawDiamond(canvas, sx + renderer.tileWidth / 2f, sy + renderer.tileHeight / 2f, Color.rgb(30, 112, 126), 112, 1.2f)

        reset()
        p.color = Color.argb(70, 180, 255, 210)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        canvas.drawLine(sx - 24f, sy + 2f + index * 1.5f, sx + 28f, sy - 5f + index * 1.5f, p)
    }

    private fun drawDistantBlock(canvas: Canvas, sx: Float, sy: Float, color: Int, alpha: Int) {
        val width = renderer.tileWidth * 0.28f
        val height = renderer.tileHeight * 0.55f
        reset()
        p.color = withAlpha(renderer.darken(color, 0.55f), alpha.coerceIn(0, 255))
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - width, sy - height, sx + width, sy, p)
        reset()
        p.color = withAlpha(renderer.lighten(color, 1.14f), (alpha * 0.75f).toInt().coerceIn(0, 255))
        canvas.drawRect(sx - width, sy - height - 4f, sx + width, sy - height + 3f, p)
    }

    private fun drawDiamond(canvas: Canvas, sx: Float, sy: Float, color: Int, alpha: Int, scale: Float) {
        val hw = renderer.tileWidth * 0.5f * scale
        val hh = renderer.tileHeight * 0.5f * scale
        val path = renderer.obtainPath()
        path.moveTo(sx, sy - hh)
        path.lineTo(sx + hw, sy)
        path.lineTo(sx, sy + hh)
        path.lineTo(sx - hw, sy)
        path.close()

        reset()
        p.color = withAlpha(color, alpha.coerceIn(0, 255))
        p.style = Paint.Style.FILL
        canvas.drawPath(path, p)

        reset()
        p.color = Color.argb((alpha * 0.22f).toInt().coerceIn(0, 255), 255, 255, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.75f
        canvas.drawPath(path, p)
        renderer.recyclePath(path)
    }

    private fun drawGlow(canvas: Canvas, sx: Float, sy: Float, radius: Float, color: Int, alpha: Int) {
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(
                sx,
                sy,
                radius,
                Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
            )
        } else {
            p.color = Color.argb((alpha * 0.45f).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy, radius, p)
        p.shader = null
    }

    private fun drawTriangle(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val path = renderer.obtainPath()
        path.moveTo(x0, y0)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.close()
        reset()
        p.color = color
        p.style = Paint.Style.FILL
        canvas.drawPath(path, p)
        renderer.recyclePath(path)
    }

    private fun tileToScreen(col: Int, row: Int): Pair<Float, Float> {
        return renderer.worldToScreen(
            col * renderer.tileWidth + renderer.tileWidth / 2f,
            row * renderer.tileHeight + renderer.tileHeight / 2f
        )
    }

    private fun isVisible(sx: Float, sy: Float, padding: Float): Boolean {
        return sx >= -padding &&
            sx <= renderer.screenWidth + padding &&
            sy >= -padding &&
            sy <= renderer.screenHeight + padding
    }

    private fun distanceOutsideRoom(col: Int, row: Int, room: Room): Int {
        val dx = when {
            col < 0 -> abs(col)
            col >= room.width -> col - room.width + 1
            else -> 0
        }
        val dy = when {
            row < 0 -> abs(row)
            row >= room.height -> row - room.height + 1
            else -> 0
        }
        return maxOf(dx, dy)
    }

    private fun stableHash(col: Int, row: Int, layer: Int): Int {
        return (col * 73856093 xor row * 19349663 xor layer * 83492791) and Int.MAX_VALUE
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun reset() {
        renderer.resetPaint()
    }
}
