package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.level.Room
import com.game.roguelike.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

class TileRenderer(private val renderer: IsometricRenderer) {

    fun renderRoom(canvas: Canvas, room: Room, playerPos: Vector2, dt: Float) {
        renderer.globalTime += dt
        renderer.updateCamera(playerPos, room)

        val theme = renderer.layerColors[room.layerIndex.coerceIn(0, 2)]

        // Compute visible tile range from camera for viewport culling
        val margin = 2 // extra tiles for walls that extend upward
        val (minGridX, minGridY) = renderer.screenToGrid(
            -renderer.tileWidth * margin.toFloat(),
            -renderer.tileHeight * 4f * margin.toFloat()
        )
        val (maxGridX, maxGridY) = renderer.screenToGrid(
            renderer.screenWidth + renderer.tileWidth * margin.toFloat(),
            renderer.screenHeight + renderer.tileHeight * 4f * margin.toFloat()
        )

        val minRow = maxOf(0, (minGridY.toInt() - margin))
        val maxRow = minOf(room.height - 1, (maxGridY.toInt() + margin))
        val minCol = maxOf(0, (minGridX.toInt() - margin))
        val maxCol = minOf(room.width - 1, (maxGridX.toInt() + margin))

        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val tile = room.getTile(col, row)
                val sx = (col - row) * renderer.tileWidth / 2f - renderer.cameraX + renderer.screenWidth / 2f
                val sy = (col + row) * renderer.tileHeight / 2f - renderer.cameraY + renderer.screenHeight / 2f

                when (tile) {
                    Room.TILE_FLOOR -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                    }
                    Room.TILE_WALL -> {
                        drawIsometricWall(canvas, sx, sy, theme.wall, theme.wallTop, theme.accent)
                    }
                    Room.TILE_OBSTACLE -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricObstacle(canvas, sx, sy, theme.accent)
                    }
                    Room.TILE_DOOR -> {
                        drawIsometricTile(canvas, sx, sy, Color.parseColor("#554400"), Color.parseColor("#665500"), col, row)
                    }
                    Room.TILE_PILLAR -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricPillar(canvas, sx, sy, theme.accent)
                    }
                    Room.TILE_CHEST -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricChest(canvas, sx, sy)
                    }
                    Room.TILE_LAVA -> {
                        drawIsometricLava(canvas, sx, sy)
                    }
                    Room.TILE_WATER -> {
                        drawIsometricWater(canvas, sx, sy)
                    }
                    Room.TILE_SPIKE -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricSpike(canvas, sx, sy, theme.accent)
                    }
                }
            }
        }
    }

    private fun drawIsometricTile(canvas: Canvas, sx: Float, sy: Float, c1: Int, c2: Int, col: Int, row: Int) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f
        val path = Path().apply {
            moveTo(sx, sy - hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx - hw, sy)
            close()
        }
        renderer.paint.color = if ((col + row) % 2 == 0) c1 else c2
        renderer.paint.style = Paint.Style.FILL
        canvas.drawPath(path, renderer.paint)

        // Subtle inner pattern for floor detail
        if ((col * 7 + row * 13) % 5 == 0) {
            renderer.paint.color = Color.argb(15, 255, 255, 255)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, 2f, renderer.paint)
        }

        // Tile border
        renderer.paint.color = Color.argb(25, 255, 255, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 1f
        canvas.drawPath(path, renderer.paint)
    }

    private fun drawIsometricWall(canvas: Canvas, sx: Float, sy: Float, sideColor: Int, topColor: Int, accentColor: Int) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f
        val wallHeight = 40f
        val leftFaceHeight = wallHeight * 0.4f

        // Left face
        renderer.paint.color = sideColor
        renderer.paint.style = Paint.Style.FILL
        val leftPath = Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx, sy + hh - leftFaceHeight)
            lineTo(sx - hw, sy - leftFaceHeight)
            close()
        }
        canvas.drawPath(leftPath, renderer.paint)

        // Right face
        renderer.paint.color = renderer.darken(sideColor, 0.7f)
        val rightPath = Path().apply {
            moveTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx + hw, sy - wallHeight)
            lineTo(sx, sy + hh - wallHeight)
            close()
        }
        canvas.drawPath(rightPath, renderer.paint)

        // Brick lines on right face
        renderer.paint.color = renderer.darken(sideColor, 0.5f)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 1f
        for (i in 1..3) {
            val yLine = sy + hh - wallHeight * i / 4f
            val xLeft = sx + (hw) * (1f - i / 4f)
            val xRight = sx + hw
            canvas.drawLine(xLeft, yLine, xRight, yLine, renderer.paint)
        }

        // Top face
        renderer.paint.color = topColor
        renderer.paint.style = Paint.Style.FILL
        val topPath = Path().apply {
            moveTo(sx, sy - hh - wallHeight)
            lineTo(sx + hw, sy - wallHeight)
            lineTo(sx, sy + hh - wallHeight)
            lineTo(sx - hw, sy - wallHeight)
            close()
        }
        canvas.drawPath(topPath, renderer.paint)

        // Top edge highlight
        renderer.paint.color = renderer.lighten(topColor, 1.2f)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 1f
        canvas.drawLine(sx - hw, sy - wallHeight, sx, sy - hh - wallHeight, renderer.paint)
        canvas.drawLine(sx, sy - hh - wallHeight, sx + hw, sy - wallHeight, renderer.paint)
    }

    private fun drawIsometricObstacle(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = renderer.tileWidth / 3f
        val hh = renderer.tileHeight / 3f
        val height = 20f

        // Pillar base
        renderer.paint.color = color
        renderer.paint.style = Paint.Style.FILL
        val path = Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy - hh)
            close()
        }
        canvas.drawPath(path, renderer.paint)

        // Top
        renderer.paint.color = renderer.lighten(color, 1.3f)
        val topPath = Path().apply {
            moveTo(sx - hw, sy - height)
            lineTo(sx, sy + hh - height)
            lineTo(sx + hw, sy - height)
            lineTo(sx, sy - hh - height)
            close()
        }
        canvas.drawPath(topPath, renderer.paint)

        // Left side
        renderer.paint.color = renderer.darken(color, 0.8f)
        canvas.drawPath(Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx, sy + hh - height)
            lineTo(sx - hw, sy - height)
            close()
        }, renderer.paint)

        // Right side
        renderer.paint.color = renderer.darken(color, 0.6f)
        canvas.drawPath(Path().apply {
            moveTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx + hw, sy - height)
            lineTo(sx, sy + hh - height)
            close()
        }, renderer.paint)

        // Gem/crystal on top
        val gemPulse = (sin(renderer.globalTime * 3f) * 0.3f + 0.7f)
        renderer.paint.color = Color.argb((180 * gemPulse).toInt(), Color.red(renderer.lighten(color, 1.8f)), Color.green(renderer.lighten(color, 1.8f)), Color.blue(renderer.lighten(color, 1.8f)))
        canvas.drawCircle(sx, sy - height - hh - 2f, 3f, renderer.paint)
    }

    private fun drawIsometricPillar(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = renderer.tileWidth / 2.5f
        val hh = renderer.tileHeight / 2.5f
        val height = 28f

        // Base diamond
        renderer.paint.color = renderer.lighten(color, 1.1f)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy - hh)
            close()
        }, renderer.paint)

        // Left face
        renderer.paint.color = color
        canvas.drawPath(Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx, sy + hh - height)
            lineTo(sx - hw, sy - height)
            close()
        }, renderer.paint)

        // Right face
        renderer.paint.color = renderer.darken(color, 0.75f)
        canvas.drawPath(Path().apply {
            moveTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx + hw, sy - height)
            lineTo(sx, sy + hh - height)
            close()
        }, renderer.paint)

        // Top face
        renderer.paint.color = renderer.lighten(color, 1.4f)
        canvas.drawPath(Path().apply {
            moveTo(sx - hw, sy - height)
            lineTo(sx, sy + hh - height)
            lineTo(sx + hw, sy - height)
            lineTo(sx, sy - hh - height)
            close()
        }, renderer.paint)

        // Top highlight ring
        renderer.paint.color = renderer.lighten(color, 1.8f)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawOval(sx - hw * 0.5f, sy - height - hh * 0.8f, sx + hw * 0.5f, sy - height - hh * 0.2f, renderer.paint)
        renderer.paint.strokeWidth = 1f
        renderer.paint.style = Paint.Style.FILL
    }

    private fun drawIsometricChest(canvas: Canvas, sx: Float, sy: Float) {
        val cw = renderer.tileWidth / 3f
        val ch = renderer.tileHeight / 3f
        val height = 12f

        // Chest body
        renderer.paint.color = Color.parseColor("#8B5E3C")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(sx - cw, sy)
            lineTo(sx, sy + ch)
            lineTo(sx + cw, sy)
            lineTo(sx, sy - ch)
            close()
        }, renderer.paint)

        // Left side
        renderer.paint.color = Color.parseColor("#6B3E1C")
        canvas.drawPath(Path().apply {
            moveTo(sx - cw, sy)
            lineTo(sx, sy + ch)
            lineTo(sx, sy + ch - height)
            lineTo(sx - cw, sy - height)
            close()
        }, renderer.paint)

        // Right side
        renderer.paint.color = Color.parseColor("#5B2E0C")
        canvas.drawPath(Path().apply {
            moveTo(sx, sy + ch)
            lineTo(sx + cw, sy)
            lineTo(sx + cw, sy - height)
            lineTo(sx, sy + ch - height)
            close()
        }, renderer.paint)

        // Top lid
        renderer.paint.color = Color.parseColor("#A0724C")
        canvas.drawPath(Path().apply {
            moveTo(sx - cw, sy - height)
            lineTo(sx, sy + ch - height)
            lineTo(sx + cw, sy - height)
            lineTo(sx, sy - ch - height)
            close()
        }, renderer.paint)

        // Gold lock
        renderer.paint.color = Color.parseColor("#FFD700")
        val lockPulse = (sin(renderer.globalTime * 2f) * 0.3f + 0.7f)
        renderer.paint.color = Color.argb((255 * lockPulse).toInt(), 255, 215, 0)
        canvas.drawCircle(sx, sy - height - ch * 0.5f, 3f, renderer.paint)

        // Sparkle
        renderer.paint.color = Color.argb((150 * lockPulse).toInt(), 255, 255, 150)
        canvas.drawCircle(sx, sy - height - ch * 0.5f - 6f, 2f, renderer.paint)
    }

    private fun drawIsometricLava(canvas: Canvas, sx: Float, sy: Float) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f

        // Lava pool base (dark red)
        val lavaBase = Color.argb(220, 120, 20, 0)
        renderer.paint.color = lavaBase
        renderer.paint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(sx, sy - hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx - hw, sy)
            close()
        }, renderer.paint)

        // Lava surface glow (animated)
        val pulse = sin(renderer.globalTime * 4f) * 0.3f + 0.7f
        renderer.paint.color = Color.argb((180 * pulse).toInt(), 255, 80, 0)
        val inset = 4f
        canvas.drawPath(Path().apply {
            moveTo(sx, sy - hh + inset)
            lineTo(sx + hw - inset, sy)
            lineTo(sx, sy + hh - inset)
            lineTo(sx - hw + inset, sy)
            close()
        }, renderer.paint)

        // Bright center
        renderer.paint.color = Color.argb((120 * pulse).toInt(), 255, 160, 0)
        val inset2 = 8f
        canvas.drawPath(Path().apply {
            moveTo(sx, sy - hh + inset2)
            lineTo(sx + hw - inset2, sy)
            lineTo(sx, sy + hh - inset2)
            lineTo(sx - hw + inset2, sy)
            close()
        }, renderer.paint)

        // Flame particles
        for (i in 0..2) {
            val fx = sx + sin(renderer.globalTime * 3f + i * 2.1f) * 6f
            val fy = sy - 8f + cos(renderer.globalTime * 4f + i * 1.7f) * 4f
            val fAlpha = (sin(renderer.globalTime * 5f + i) * 0.4f + 0.6f) * 150
            renderer.paint.color = Color.argb(fAlpha.toInt(), 255, 200, 50)
            canvas.drawCircle(fx, fy, 2f, renderer.paint)
        }
    }

    private fun drawIsometricWater(canvas: Canvas, sx: Float, sy: Float) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f

        // Water pool base (deep blue)
        renderer.paint.color = Color.argb(200, 20, 50, 120)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(sx, sy - hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx - hw, sy)
            close()
        }, renderer.paint)

        // Water surface (lighter, animated ripple)
        val ripple = sin(renderer.globalTime * 2f) * 0.2f + 0.8f
        renderer.paint.color = Color.argb((150 * ripple).toInt(), 40, 100, 180)
        val inset = 3f
        canvas.drawPath(Path().apply {
            moveTo(sx, sy - hh + inset)
            lineTo(sx + hw - inset, sy)
            lineTo(sx, sy + hh - inset)
            lineTo(sx - hw + inset, sy)
            close()
        }, renderer.paint)

        // Ripple lines
        renderer.paint.color = Color.argb(80, 120, 180, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 1f
        val rippleOffset = sin(renderer.globalTime * 3f) * 4f
        canvas.drawLine(sx - 6f + rippleOffset, sy - 2f, sx + 6f + rippleOffset, sy - 2f, renderer.paint)
        canvas.drawLine(sx - 4f - rippleOffset, sy + 2f, sx + 4f - rippleOffset, sy + 2f, renderer.paint)
        renderer.paint.style = Paint.Style.FILL

        // Light reflections
        val refPulse = sin(renderer.globalTime * 1.5f) * 0.3f + 0.5f
        renderer.paint.color = Color.argb((60 * refPulse).toInt(), 180, 220, 255)
        canvas.drawCircle(sx - 4f, sy - 3f, 2f, renderer.paint)
        canvas.drawCircle(sx + 5f, sy + 1f, 1.5f, renderer.paint)
    }

    private fun drawIsometricSpike(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        // Multiple spike points
        val spikeColor = renderer.darken(color, 0.5f)
        val spikeHighlight = renderer.lighten(color, 1.5f)

        for (i in 0..2) {
            val ox = (i - 1) * 8f
            val spikeHeight = 14f + sin(renderer.globalTime * 6f + i * 1.5f).toFloat() * 2f

            // Spike triangle
            renderer.paint.color = spikeColor
            renderer.paint.style = Paint.Style.FILL
            canvas.drawPath(Path().apply {
                moveTo(sx + ox - 3f, sy)
                lineTo(sx + ox, sy - spikeHeight)
                lineTo(sx + ox + 3f, sy)
                close()
            }, renderer.paint)

            // Spike highlight edge
            renderer.paint.color = spikeHighlight
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 1f
            canvas.drawLine(sx + ox, sy - spikeHeight, sx + ox + 2f, sy - 2f, renderer.paint)
            renderer.paint.style = Paint.Style.FILL
        }

        // Danger pulse glow
        val dangerPulse = sin(renderer.globalTime * 4f) * 0.3f + 0.7f
        renderer.paint.color = Color.argb((30 * dangerPulse).toInt(), 255, 50, 50)
        canvas.drawCircle(sx, sy - 5f, 10f, renderer.paint)
    }
}
