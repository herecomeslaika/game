package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.level.Room
import com.game.roguelike.util.Vector2
import kotlin.math.cos
import kotlin.math.sin

class TileRenderer(private val renderer: IsometricRenderer) {

    private val p: Paint get() = renderer.paint
    private val useShaders: Boolean get() = renderer.enableShaders

    fun renderRoom(canvas: Canvas, room: Room, playerPos: Vector2, dt: Float) {
        renderer.globalTime += dt
        renderer.updateCamera(playerPos, room)

        val theme = renderer.layerColors[room.layerIndex.coerceIn(0, 2)]

        // Collect light sources from special tiles
        renderer.lightSources.clear()
        for (row in 0 until room.height) {
            for (col in 0 until room.width) {
                val tile = room.getTile(col, row)
                val wx = col * renderer.tileWidth + renderer.tileWidth / 2f
                val wy = row * renderer.tileHeight + renderer.tileHeight / 2f
                when (tile) {
                    Room.TILE_LAVA -> renderer.lightSources.add(IsometricRenderer.LightSource(wx, wy, 80f, Color.parseColor("#FF6600"), 1f))
                    Room.TILE_PILLAR -> renderer.lightSources.add(IsometricRenderer.LightSource(wx, wy, 60f, Color.parseColor("#7B5EA7"), 0.4f))
                }
            }
        }

        for (row in 0 until room.height) {
            for (col in 0 until room.width) {
                val tile = room.getTile(col, row)
                val sx = (col - row) * renderer.tileWidth / 2f - renderer.cameraX + renderer.screenWidth / 2f
                val sy = (col + row) * renderer.tileHeight / 2f - renderer.cameraY + renderer.screenHeight / 2f

                if (sx < -renderer.tileWidth * 2 || sx > renderer.screenWidth + renderer.tileWidth * 2 ||
                    sy < -renderer.tileHeight * 4 || sy > renderer.screenHeight + renderer.tileHeight * 4
                ) continue

                when (tile) {
                    Room.TILE_FLOOR -> drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                    Room.TILE_WALL -> drawIsometricWall(canvas, sx, sy, theme.wall, theme.wallTop, theme.accent, col, row)
                    Room.TILE_OBSTACLE -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricObstacle(canvas, sx, sy, theme.accent)
                    }
                    Room.TILE_DOOR -> drawIsometricTile(canvas, sx, sy, Color.parseColor("#554400"), Color.parseColor("#665500"), col, row)
                    Room.TILE_PILLAR -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricPillar(canvas, sx, sy, theme.accent)
                    }
                    Room.TILE_CHEST -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricChest(canvas, sx, sy)
                    }
                    Room.TILE_LAVA -> drawIsometricLava(canvas, sx, sy)
                    Room.TILE_WATER -> drawIsometricWater(canvas, sx, sy)
                    Room.TILE_SPIKE -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricSpike(canvas, sx, sy, theme.accent)
                    }
                    Room.TILE_EVENT_SHRINE -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                        drawIsometricEventShrine(canvas, sx, sy, theme.accent)
                    }
                }
            }
        }

        // Render light source overlays
        for (light in renderer.lightSources) {
            val (lx, ly) = renderer.worldToScreen(light.x, light.y)
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(lx, ly, light.radius,
                    Color.argb((light.intensity * 50).toInt(), Color.red(light.color), Color.green(light.color), Color.blue(light.color)),
                    Color.argb(0, Color.red(light.color), Color.green(light.color), Color.blue(light.color)))
            } else {
                p.color = Color.argb((light.intensity * 30).toInt(), Color.red(light.color), Color.green(light.color), Color.blue(light.color))
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(lx, ly, light.radius, p)
            p.shader = null
        }
    }

    private fun drawIsometricTile(canvas: Canvas, sx: Float, sy: Float, c1: Int, c2: Int, col: Int, row: Int) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f
        val path = renderer.obtainPath()
        path.moveTo(sx, sy - hh)
        path.lineTo(sx + hw, sy)
        path.lineTo(sx, sy + hh)
        path.lineTo(sx - hw, sy)
        path.close()

        val baseColor = if ((col + row) % 2 == 0) c1 else c2
        reset()
        p.color = baseColor
        p.style = Paint.Style.FILL
        canvas.drawPath(path, p)

        // Subtle floor detail
        if ((col * 7 + row * 13) % 5 == 0) {
            reset()
            p.color = Color.argb(20, 255, 255, 255)
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, 2f, p)
        }

        // Tile border
        reset()
        p.color = Color.argb(25, 255, 255, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        canvas.drawPath(path, p)

        renderer.recyclePath(path)
    }

    private fun drawIsometricWall(canvas: Canvas, sx: Float, sy: Float, sideColor: Int, topColor: Int, accentColor: Int, col: Int, row: Int) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f
        val wallHeight = 40f
        val leftFaceHeight = wallHeight * 0.4f

        // Left face
        reset()
        p.color = sideColor
        p.style = Paint.Style.FILL
        val leftPath = renderer.obtainPath()
        leftPath.moveTo(sx - hw, sy)
        leftPath.lineTo(sx, sy + hh)
        leftPath.lineTo(sx, sy + hh - leftFaceHeight)
        leftPath.lineTo(sx - hw, sy - leftFaceHeight)
        leftPath.close()
        canvas.drawPath(leftPath, p)
        renderer.recyclePath(leftPath)

        // Right face
        reset()
        p.color = renderer.darken(sideColor, 0.7f)
        p.style = Paint.Style.FILL
        val rightPath = renderer.obtainPath()
        rightPath.moveTo(sx, sy + hh)
        rightPath.lineTo(sx + hw, sy)
        rightPath.lineTo(sx + hw, sy - wallHeight)
        rightPath.lineTo(sx, sy + hh - wallHeight)
        rightPath.close()
        canvas.drawPath(rightPath, p)
        renderer.recyclePath(rightPath)

        // Brick lines with staggered offset
        reset()
        p.color = renderer.darken(sideColor, 0.5f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        for (i in 1..3) {
            val yLine = sy + hh - wallHeight * i / 4f
            val offset = if (i % 2 == 0) hw * 0.3f else 0f
            canvas.drawLine(sx + offset, yLine, sx + hw, yLine, p)
        }

        // Top face
        reset()
        p.color = topColor
        p.style = Paint.Style.FILL
        val topPath = renderer.obtainPath()
        topPath.moveTo(sx, sy - hh - wallHeight)
        topPath.lineTo(sx + hw, sy - wallHeight)
        topPath.lineTo(sx, sy + hh - wallHeight)
        topPath.lineTo(sx - hw, sy - wallHeight)
        topPath.close()
        canvas.drawPath(topPath, p)
        renderer.recyclePath(topPath)

        // Top edge highlight
        reset()
        p.color = renderer.lighten(topColor, 1.2f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        canvas.drawLine(sx - hw, sy - wallHeight, sx, sy - hh - wallHeight, p)
        canvas.drawLine(sx, sy - hh - wallHeight, sx + hw, sy - wallHeight, p)

        // Moss/crack details on some walls
        val hash = (col * 7 + row * 13) % 11
        if (hash < 3) {
            reset()
            p.color = Color.argb(30, 60, 100, 40)
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx + hw * 0.3f, sy - wallHeight * 0.5f, 2f, p)
            canvas.drawCircle(sx + hw * 0.6f, sy - wallHeight * 0.7f, 1.5f, p)
        } else if (hash < 5) {
            reset()
            p.color = Color.argb(25, 0, 0, 0)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.5f
            canvas.drawLine(sx + hw * 0.4f, sy - wallHeight * 0.3f, sx + hw * 0.6f, sy - wallHeight * 0.5f, p)
        }
    }

    private fun drawIsometricObstacle(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = renderer.tileWidth / 3f
        val hh = renderer.tileHeight / 3f
        val height = 20f

        // Base diamond
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - hw, sy, sx + hw, sy, renderer.lighten(color, 1.1f), renderer.darken(color, 0.8f))
        } else {
            p.color = color
        }
        p.style = Paint.Style.FILL
        val basePath = renderer.obtainPath()
        basePath.moveTo(sx - hw, sy); basePath.lineTo(sx, sy + hh); basePath.lineTo(sx + hw, sy); basePath.lineTo(sx, sy - hh); basePath.close()
        canvas.drawPath(basePath, p)
        renderer.recyclePath(basePath)
        p.shader = null

        // Top
        reset()
        p.color = renderer.lighten(color, 1.3f)
        p.style = Paint.Style.FILL
        val topPath = renderer.obtainPath()
        topPath.moveTo(sx - hw, sy - height); topPath.lineTo(sx, sy + hh - height); topPath.lineTo(sx + hw, sy - height); topPath.lineTo(sx, sy - hh - height); topPath.close()
        canvas.drawPath(topPath, p)
        renderer.recyclePath(topPath)

        // Left side with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - hw, sy, sx, sy - height, renderer.darken(color, 0.85f), renderer.darken(color, 0.7f))
        } else {
            p.color = renderer.darken(color, 0.8f)
        }
        p.style = Paint.Style.FILL
        val leftSide = renderer.obtainPath()
        leftSide.moveTo(sx - hw, sy); leftSide.lineTo(sx, sy + hh); leftSide.lineTo(sx, sy + hh - height); leftSide.lineTo(sx - hw, sy - height); leftSide.close()
        canvas.drawPath(leftSide, p)
        renderer.recyclePath(leftSide)
        p.shader = null

        // Right side with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy, sx + hw, sy - height, renderer.darken(color, 0.65f), renderer.darken(color, 0.5f))
        } else {
            p.color = renderer.darken(color, 0.6f)
        }
        p.style = Paint.Style.FILL
        val rightSide = renderer.obtainPath()
        rightSide.moveTo(sx, sy + hh); rightSide.lineTo(sx + hw, sy); rightSide.lineTo(sx + hw, sy - height); rightSide.lineTo(sx, sy + hh - height); rightSide.close()
        canvas.drawPath(rightSide, p)
        renderer.recyclePath(rightSide)
        p.shader = null

        // Gem/crystal on top with radial gradient
        val gemPulse = (sin(renderer.globalTime * 3f) * 0.3f + 0.7f)
        val lightColor = renderer.lighten(color, 1.8f)
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - height - hh - 2f, 5f,
                Color.argb((200 * gemPulse).toInt(), Color.red(lightColor), Color.green(lightColor), Color.blue(lightColor)),
                Color.argb(0, Color.red(lightColor), Color.green(lightColor), Color.blue(lightColor)))
        } else {
            p.color = Color.argb((180 * gemPulse).toInt(), Color.red(lightColor), Color.green(lightColor), Color.blue(lightColor))
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - height - hh - 2f, 3f, p)
        p.shader = null
    }

    private fun drawIsometricPillar(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = renderer.tileWidth / 2.5f
        val hh = renderer.tileHeight / 2.5f
        val height = 28f

        // Base diamond
        reset()
        p.color = renderer.lighten(color, 1.1f)
        p.style = Paint.Style.FILL
        val baseP = renderer.obtainPath()
        baseP.moveTo(sx - hw, sy); baseP.lineTo(sx, sy + hh); baseP.lineTo(sx + hw, sy); baseP.lineTo(sx, sy - hh); baseP.close()
        canvas.drawPath(baseP, p)
        renderer.recyclePath(baseP)

        // Left face with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - hw, sy, sx, sy - height, renderer.lighten(color, 1.05f), renderer.darken(color, 0.8f))
        } else {
            p.color = color
        }
        p.style = Paint.Style.FILL
        val leftFace = renderer.obtainPath()
        leftFace.moveTo(sx - hw, sy); leftFace.lineTo(sx, sy + hh); leftFace.lineTo(sx, sy + hh - height); leftFace.lineTo(sx - hw, sy - height); leftFace.close()
        canvas.drawPath(leftFace, p)
        renderer.recyclePath(leftFace)
        p.shader = null

        // Right face with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy, sx + hw, sy - height, renderer.darken(color, 0.8f), renderer.darken(color, 0.6f))
        } else {
            p.color = renderer.darken(color, 0.75f)
        }
        p.style = Paint.Style.FILL
        val rightFace = renderer.obtainPath()
        rightFace.moveTo(sx, sy + hh); rightFace.lineTo(sx + hw, sy); rightFace.lineTo(sx + hw, sy - height); rightFace.lineTo(sx, sy + hh - height); rightFace.close()
        canvas.drawPath(rightFace, p)
        renderer.recyclePath(rightFace)
        p.shader = null

        // Top face
        reset()
        p.color = renderer.lighten(color, 1.4f)
        p.style = Paint.Style.FILL
        val topFace = renderer.obtainPath()
        topFace.moveTo(sx - hw, sy - height); topFace.lineTo(sx, sy + hh - height); topFace.lineTo(sx + hw, sy - height); topFace.lineTo(sx, sy - hh - height); topFace.close()
        canvas.drawPath(topFace, p)
        renderer.recyclePath(topFace)

        // Top highlight ring
        reset()
        p.color = renderer.lighten(color, 1.8f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawOval(sx - hw * 0.5f, sy - height - hh * 0.8f, sx + hw * 0.5f, sy - height - hh * 0.2f, p)
        p.strokeWidth = 1f
        p.style = Paint.Style.FILL
    }

    private fun drawIsometricChest(canvas: Canvas, sx: Float, sy: Float) {
        val cw = renderer.tileWidth / 3f
        val ch = renderer.tileHeight / 3f
        val height = 12f

        // Chest body
        reset()
        p.color = Color.parseColor("#8B5E3C")
        p.style = Paint.Style.FILL
        val bodyPath = renderer.obtainPath()
        bodyPath.moveTo(sx - cw, sy); bodyPath.lineTo(sx, sy + ch); bodyPath.lineTo(sx + cw, sy); bodyPath.lineTo(sx, sy - ch); bodyPath.close()
        canvas.drawPath(bodyPath, p)
        renderer.recyclePath(bodyPath)

        // Left side with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - cw, sy, sx, sy - height, Color.parseColor("#6B3E1C"), Color.parseColor("#5B2E0C"))
        } else {
            p.color = Color.parseColor("#6B3E1C")
        }
        p.style = Paint.Style.FILL
        val leftP = renderer.obtainPath()
        leftP.moveTo(sx - cw, sy); leftP.lineTo(sx, sy + ch); leftP.lineTo(sx, sy + ch - height); leftP.lineTo(sx - cw, sy - height); leftP.close()
        canvas.drawPath(leftP, p)
        renderer.recyclePath(leftP)
        p.shader = null

        // Right side with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx, sy, sx + cw, sy - height, Color.parseColor("#5B2E0C"), Color.parseColor("#4B1E00"))
        } else {
            p.color = Color.parseColor("#5B2E0C")
        }
        p.style = Paint.Style.FILL
        val rightP = renderer.obtainPath()
        rightP.moveTo(sx, sy + ch); rightP.lineTo(sx + cw, sy); rightP.lineTo(sx + cw, sy - height); rightP.lineTo(sx, sy + ch - height); rightP.close()
        canvas.drawPath(rightP, p)
        renderer.recyclePath(rightP)
        p.shader = null

        // Top lid with gradient
        reset()
        if (useShaders) {
            p.shader = renderer.makeLinearGradient(sx - cw, sy - height, sx + cw, sy - ch - height, renderer.lighten(Color.parseColor("#A0724C"), 1.1f), Color.parseColor("#A0724C"))
        } else {
            p.color = Color.parseColor("#A0724C")
        }
        p.style = Paint.Style.FILL
        val lidPath = renderer.obtainPath()
        lidPath.moveTo(sx - cw, sy - height); lidPath.lineTo(sx, sy + ch - height); lidPath.lineTo(sx + cw, sy - height); lidPath.lineTo(sx, sy - ch - height); lidPath.close()
        canvas.drawPath(lidPath, p)
        renderer.recyclePath(lidPath)
        p.shader = null

        // Gold lock with radial gradient
        val lockPulse = (sin(renderer.globalTime * 2f) * 0.3f + 0.7f)
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - height - ch * 0.5f, 5f, Color.argb((255 * lockPulse).toInt(), 255, 215, 0), Color.argb((100 * lockPulse).toInt(), 200, 170, 0))
        } else {
            p.color = Color.argb((255 * lockPulse).toInt(), 255, 215, 0)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - height - ch * 0.5f, 3f, p)
        p.shader = null

        // Sparkle
        reset()
        p.color = Color.argb((150 * lockPulse).toInt(), 255, 255, 150)
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - height - ch * 0.5f - 6f, 2f, p)
    }

    private fun drawIsometricLava(canvas: Canvas, sx: Float, sy: Float) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f

        // Lava pool base
        reset()
        p.color = Color.argb(220, 120, 20, 0)
        p.style = Paint.Style.FILL
        val lavaBase = renderer.obtainPath()
        lavaBase.moveTo(sx, sy - hh); lavaBase.lineTo(sx + hw, sy); lavaBase.lineTo(sx, sy + hh); lavaBase.lineTo(sx - hw, sy); lavaBase.close()
        canvas.drawPath(lavaBase, p)
        renderer.recyclePath(lavaBase)

        // Lava surface glow (animated) with radial gradient
        val pulse = sin(renderer.globalTime * 4f) * 0.3f + 0.7f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy, hw - 4f, Color.argb((200 * pulse).toInt(), 255, 120, 0), Color.argb((100 * pulse).toInt(), 200, 40, 0))
        } else {
            p.color = Color.argb((180 * pulse).toInt(), 255, 80, 0)
        }
        p.style = Paint.Style.FILL
        val inset = 4f
        val lavaGlow = renderer.obtainPath()
        lavaGlow.moveTo(sx, sy - hh + inset); lavaGlow.lineTo(sx + hw - inset, sy); lavaGlow.lineTo(sx, sy + hh - inset); lavaGlow.lineTo(sx - hw + inset, sy); lavaGlow.close()
        canvas.drawPath(lavaGlow, p)
        renderer.recyclePath(lavaGlow)
        p.shader = null

        // Bright center
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy, hw - 8f, Color.argb((150 * pulse).toInt(), 255, 200, 0), Color.argb(0, 255, 160, 0))
        } else {
            p.color = Color.argb((120 * pulse).toInt(), 255, 160, 0)
        }
        p.style = Paint.Style.FILL
        val inset2 = 8f
        val lavaCenter = renderer.obtainPath()
        lavaCenter.moveTo(sx, sy - hh + inset2); lavaCenter.lineTo(sx + hw - inset2, sy); lavaCenter.lineTo(sx, sy + hh - inset2); lavaCenter.lineTo(sx - hw + inset2, sy); lavaCenter.close()
        canvas.drawPath(lavaCenter, p)
        renderer.recyclePath(lavaCenter)
        p.shader = null

        // Flame particles
        for (i in 0..2) {
            val fx = sx + sin(renderer.globalTime * 3f + i * 2.1f) * 6f
            val fy = sy - 8f + cos(renderer.globalTime * 4f + i * 1.7f) * 4f
            val fAlpha = (sin(renderer.globalTime * 5f + i) * 0.4f + 0.6f) * 150
            reset()
            if (useShaders) {
                p.shader = renderer.makeRadialGradient(fx, fy, 3f, Color.argb(fAlpha.toInt(), 255, 220, 50), Color.argb(0, 255, 200, 50))
            } else {
                p.color = Color.argb(fAlpha.toInt(), 255, 200, 50)
            }
            p.style = Paint.Style.FILL
            canvas.drawCircle(fx, fy, 2f, p)
            p.shader = null
        }
    }

    private fun drawIsometricWater(canvas: Canvas, sx: Float, sy: Float) {
        val hw = renderer.tileWidth / 2f
        val hh = renderer.tileHeight / 2f

        // Water pool base
        reset()
        p.color = Color.argb(200, 20, 50, 120)
        p.style = Paint.Style.FILL
        val waterBase = renderer.obtainPath()
        waterBase.moveTo(sx, sy - hh); waterBase.lineTo(sx + hw, sy); waterBase.lineTo(sx, sy + hh); waterBase.lineTo(sx - hw, sy); waterBase.close()
        canvas.drawPath(waterBase, p)
        renderer.recyclePath(waterBase)

        // Water surface with radial gradient
        val ripple = sin(renderer.globalTime * 2f) * 0.2f + 0.8f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy, hw - 3f, Color.argb((160 * ripple).toInt(), 60, 120, 200), Color.argb((80 * ripple).toInt(), 30, 80, 160))
        } else {
            p.color = Color.argb((150 * ripple).toInt(), 40, 100, 180)
        }
        p.style = Paint.Style.FILL
        val inset = 3f
        val waterSurface = renderer.obtainPath()
        waterSurface.moveTo(sx, sy - hh + inset); waterSurface.lineTo(sx + hw - inset, sy); waterSurface.lineTo(sx, sy + hh - inset); waterSurface.lineTo(sx - hw + inset, sy); waterSurface.close()
        canvas.drawPath(waterSurface, p)
        renderer.recyclePath(waterSurface)
        p.shader = null

        // Ripple lines
        reset()
        p.color = Color.argb(80, 120, 180, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        val rippleOffset = sin(renderer.globalTime * 3f) * 4f
        canvas.drawLine(sx - 6f + rippleOffset, sy - 2f, sx + 6f + rippleOffset, sy - 2f, p)
        canvas.drawLine(sx - 4f - rippleOffset, sy + 2f, sx + 4f - rippleOffset, sy + 2f, p)
        p.style = Paint.Style.FILL

        // Light reflections
        val refPulse = sin(renderer.globalTime * 1.5f) * 0.3f + 0.5f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx - 4f, sy - 3f, 3f, Color.argb((80 * refPulse).toInt(), 200, 230, 255), Color.argb(0, 180, 220, 255))
        } else {
            p.color = Color.argb((60 * refPulse).toInt(), 180, 220, 255)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx - 4f, sy - 3f, 2f, p)
        canvas.drawCircle(sx + 5f, sy + 1f, 1.5f, p)
        p.shader = null
    }

    private fun drawIsometricSpike(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val spikeColor = renderer.darken(color, 0.5f)
        val spikeHighlight = renderer.lighten(color, 1.5f)

        for (i in 0..2) {
            val ox = (i - 1) * 8f
            val spikeHeight = 14f + sin(renderer.globalTime * 6f + i * 1.5f).toFloat() * 2f

            // Spike triangle with gradient
            reset()
            if (useShaders) {
                p.shader = renderer.makeLinearGradient(sx + ox, sy, sx + ox, sy - spikeHeight, spikeColor, spikeHighlight)
            } else {
                p.color = spikeColor
            }
            p.style = Paint.Style.FILL
            val spikePath = renderer.obtainPath()
            spikePath.moveTo(sx + ox - 3f, sy)
            spikePath.lineTo(sx + ox, sy - spikeHeight)
            spikePath.lineTo(sx + ox + 3f, sy)
            spikePath.close()
            canvas.drawPath(spikePath, p)
            renderer.recyclePath(spikePath)
            p.shader = null

            // Spike highlight edge
            reset()
            p.color = spikeHighlight
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            canvas.drawLine(sx + ox, sy - spikeHeight, sx + ox + 2f, sy - 2f, p)
            p.style = Paint.Style.FILL
        }

        // Danger pulse glow
        val dangerPulse = sin(renderer.globalTime * 4f) * 0.3f + 0.7f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, sy - 5f, 12f, Color.argb((40 * dangerPulse).toInt(), 255, 50, 50), Color.argb(0, 255, 50, 50))
        } else {
            p.color = Color.argb((30 * dangerPulse).toInt(), 255, 50, 50)
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - 5f, 10f, p)
        p.shader = null
    }

    private fun drawIsometricEventShrine(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = renderer.tileWidth / 2.5f
        val hh = renderer.tileHeight / 2.5f
        val height = 24f
        val pulse = sin(renderer.globalTime * 2f) * 0.3f + 0.7f

        // Base platform
        reset()
        p.color = renderer.darken(color, 0.6f)
        p.style = Paint.Style.FILL
        val basePath = renderer.obtainPath()
        basePath.moveTo(sx - hw, sy); basePath.lineTo(sx, sy + hh); basePath.lineTo(sx + hw, sy); basePath.lineTo(sx, sy - hh); basePath.close()
        canvas.drawPath(basePath, p)
        renderer.recyclePath(basePath)

        // Shrine body (tapered column)
        reset()
        p.color = renderer.lighten(color, 1.1f)
        p.style = Paint.Style.FILL
        val bodyW = hw * 0.4f
        canvas.drawRect(sx - bodyW, sy - height, sx + bodyW, sy, p)

        // Top orb with radial glow
        val orbY = sy - height - 6f
        reset()
        if (useShaders) {
            p.shader = renderer.makeRadialGradient(sx, orbY, 8f,
                Color.argb((255 * pulse).toInt(), Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb((60 * pulse).toInt(), Color.red(color), Color.green(color), Color.blue(color)))
        } else {
            p.color = Color.argb((220 * pulse).toInt(), Color.red(color), Color.green(color), Color.blue(color))
        }
        p.style = Paint.Style.FILL
        canvas.drawCircle(sx, orbY, 6f, p)
        p.shader = null

        // Outer glow ring
        reset()
        p.color = Color.argb((40 * pulse).toInt(), Color.red(renderer.lighten(color, 1.5f)), Color.green(renderer.lighten(color, 1.5f)), Color.blue(renderer.lighten(color, 1.5f)))
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawCircle(sx, orbY, 10f + sin(renderer.globalTime * 3f) * 2f, p)
        p.style = Paint.Style.FILL
        p.strokeWidth = 1f
    }

    private fun reset() {
        renderer.resetPaint()
    }
}
