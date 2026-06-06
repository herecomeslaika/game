package com.game.roguelike.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.game.roguelike.core.BossEntranceTimeline
import com.game.roguelike.entity.Enemy
import com.game.roguelike.entity.EnemyType
import com.game.roguelike.level.Room
import com.game.roguelike.util.Vector2
import kotlin.math.sin

class BossEntranceCinematicRenderer(private val renderer: IsometricRenderer) {

    private val p: Paint get() = renderer.paint
    private val useShaders: Boolean get() = renderer.enableShaders

    fun render(canvas: Canvas, room: Room, layerIndex: Int, timer: Float) {
        val progress = (timer / BossEntranceTimeline.CINEMATIC_DURATION).coerceIn(0f, 1f)
        val worldX = room.width * 32f
        val worldY = room.height * 16f
        val (sx, sy) = renderer.worldToScreen(worldX, worldY)

        when (layerIndex.coerceIn(0, 2)) {
            0 -> drawGraveRise(canvas, sx, sy, worldX, worldY, layerIndex, progress)
            1 -> drawLavaBirth(canvas, sx, sy, worldX, worldY, layerIndex, progress)
            else -> drawChampionDrop(canvas, sx, sy, worldX, worldY, layerIndex, progress)
        }
    }

    private fun drawGraveRise(canvas: Canvas, sx: Float, sy: Float, worldX: Float, worldY: Float, layerIndex: Int, progress: Float) {
        val open = smoothStep(progress, 0.12f, 0.42f)
        val rise = smoothStep(progress, 0.34f, 0.9f)
        val pulse = ((sin(renderer.globalTime * 6f) + 1f) * 0.5f)

        drawGlow(canvas, sx, sy - 18f, 110f + pulse * 38f, Color.rgb(80, 255, 170), (45 + pulse * 42f).toInt())
        drawCrackedGround(canvas, sx, sy, Color.rgb(82, 64, 112), 0.95f)

        reset()
        p.color = Color.argb(210, 72, 65, 96)
        p.style = Paint.Style.FILL
        canvas.save()
        canvas.rotate(-10f - open * 44f, sx - 18f, sy - 20f)
        canvas.drawRoundRect(sx - 58f, sy - 58f, sx + 8f, sy - 10f, 5f, 5f, p)
        canvas.restore()

        reset()
        p.color = Color.argb(230, 102, 94, 132)
        canvas.save()
        canvas.rotate(8f + open * 38f, sx + 18f, sy - 20f)
        canvas.drawRoundRect(sx - 8f, sy - 58f, sx + 58f, sy - 10f, 5f, 5f, p)
        canvas.restore()

        renderPreviewBoss(
            canvas = canvas,
            type = EnemyType.MEGA_SKELETON,
            worldX = worldX,
            worldY = worldY,
            layerIndex = layerIndex,
            screenOffsetY = 54f - rise * 96f,
            scale = 0.82f + rise * 0.18f
        )

        if (progress > 0.72f) {
            drawShockwave(canvas, sx, sy + 4f, smoothStep(progress, 0.72f, 1f), Color.rgb(98, 255, 178))
        }
    }

    private fun drawLavaBirth(canvas: Canvas, sx: Float, sy: Float, worldX: Float, worldY: Float, layerIndex: Int, progress: Float) {
        val lift = smoothStep(progress, 0.1f, 0.55f)
        val form = smoothStep(progress, 0.48f, 0.92f)
        val pulse = ((sin(renderer.globalTime * 8f) + 1f) * 0.5f)

        drawLavaPool(canvas, sx, sy + 10f, 1.25f, pulse)
        drawGlow(canvas, sx, sy - lift * 78f, 130f + pulse * 48f, Color.rgb(255, 96, 36), (62 + pulse * 54f).toInt())

        val sphereY = sy - lift * 80f
        val sphereRadius = 44f + (1f - form) * 26f + pulse * 6f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(
                sx - 12f,
                sphereY - 16f,
                sphereRadius,
                Color.rgb(255, 222, 76),
                Color.rgb(154, 30, 10)
            )
        } else {
            p.color = Color.rgb(236, 82, 28)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sphereY, sphereRadius, p)
        p.shader = null

        if (form > 0.03f) {
            renderPreviewBoss(
                canvas = canvas,
                type = EnemyType.INFERNO_TITAN,
                worldX = worldX,
                worldY = worldY,
                layerIndex = layerIndex,
                screenOffsetY = -10f - (1f - form) * 54f,
                scale = 0.66f + form * 0.34f
            )
        }
        if (progress > 0.68f) {
            drawShockwave(canvas, sx, sy + 10f, smoothStep(progress, 0.68f, 1f), Color.rgb(255, 116, 45))
        }

        for (i in 0 until 12) {
            val angle = i * 30f + renderer.globalTime * 70f
            val dist = 34f + lift * 72f + (i % 3) * 8f
            val x = sx + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * dist
            val y = sphereY + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * dist * 0.45f
            reset()
            p.color = Color.argb(165, 255, 150, 55)
            p.style = Paint.Style.FILL
            canvas.drawCircle(x, y, 2.5f + i % 3, p)
        }
    }

    private fun drawChampionDrop(canvas: Canvas, sx: Float, sy: Float, worldX: Float, worldY: Float, layerIndex: Int, progress: Float) {
        val fall = smoothStep(progress, 0.08f, 0.62f)
        val impact = smoothStep(progress, 0.62f, 0.88f)
        val bossOffsetY = -360f * (1f - fall)

        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(
                sx,
                0f,
                sx,
                sy + 50f,
                Color.argb(120, 205, 230, 255),
                Color.argb(0, 205, 230, 255)
            )
        } else {
            p.color = Color.argb(50, 180, 220, 255)
        }
        p.style = Paint.Style.FILL
        canvas.drawRect(sx - 34f, 0f, sx + 34f, sy + 50f, p)
        p.shader = null

        renderPreviewBoss(
            canvas = canvas,
            type = EnemyType.CHAMPION,
            worldX = worldX,
            worldY = worldY,
            layerIndex = layerIndex,
            screenOffsetY = bossOffsetY,
            scale = 0.92f + fall * 0.08f
        )

        if (progress > 0.58f) {
            drawShockwave(canvas, sx, sy + 8f, impact, Color.rgb(180, 218, 255))
            drawCrackedGround(canvas, sx, sy + 6f, Color.rgb(155, 188, 145), 0.7f + impact * 0.6f)
        }
    }

    private fun renderPreviewBoss(
        canvas: Canvas,
        type: EnemyType,
        worldX: Float,
        worldY: Float,
        layerIndex: Int,
        screenOffsetY: Float,
        scale: Float
    ) {
        val boss = Enemy(type, Vector2(worldX, worldY), layerIndex, isBoss = true)
        boss.idleTime = renderer.globalTime
        boss.moveAnimPhase = (renderer.globalTime * 0.22f) % 1f

        val (sx, sy) = renderer.worldToScreen(worldX, worldY)
        canvas.save()
        canvas.translate(0f, screenOffsetY)
        canvas.scale(scale, scale, sx, sy)
        renderer.renderEnemy(canvas, boss)
        canvas.restore()
    }

    private fun drawLavaPool(canvas: Canvas, sx: Float, sy: Float, scale: Float, pulse: Float) {
        drawGlow(canvas, sx, sy, 116f * scale, Color.rgb(255, 120, 35), (52 + pulse * 50f).toInt())
        val path = renderer.obtainPath()
        path.moveTo(sx, sy - 34f * scale)
        path.lineTo(sx + 92f * scale, sy)
        path.lineTo(sx + 18f * scale, sy + 44f * scale)
        path.lineTo(sx - 92f * scale, sy + 8f * scale)
        path.close()
        reset()
        p.color = Color.argb(225, 92, 20, 8)
        p.style = Paint.Style.FILL
        canvas.drawPath(path, p)
        reset()
        p.color = Color.argb(180, 255, 114, 32)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        canvas.drawPath(path, p)
        renderer.recyclePath(path)
    }

    private fun drawCrackedGround(canvas: Canvas, sx: Float, sy: Float, color: Int, scale: Float) {
        reset()
        p.color = Color.argb(155, Color.red(color), Color.green(color), Color.blue(color))
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        for (i in 0 until 7) {
            val angle = -65f + i * 22f
            val len = (38f + (i % 3) * 20f) * scale
            val x = sx + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * len
            val y = sy + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * len * 0.55f
            canvas.drawLine(sx, sy, x, y, p)
        }
    }

    private fun drawShockwave(canvas: Canvas, sx: Float, sy: Float, progress: Float, color: Int) {
        val wave = progress.coerceIn(0f, 1f)
        reset()
        p.color = Color.argb(((1f - wave) * 170f).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        val radiusX = 40f + wave * 190f
        val radiusY = 16f + wave * 70f
        canvas.drawOval(sx - radiusX, sy - radiusY, sx + radiusX, sy + radiusY, p)
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

    private fun smoothStep(value: Float, start: Float, end: Float): Float {
        val t = ((value - start) / (end - start)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun reset() {
        renderer.resetPaint()
    }
}
