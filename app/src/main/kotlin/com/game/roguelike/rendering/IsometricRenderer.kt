package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.combat.Projectile
import com.game.roguelike.core.PlayerState
import com.game.roguelike.core.RoomType
import com.game.roguelike.entity.*
import com.game.roguelike.level.Door
import com.game.roguelike.level.Room
import com.game.roguelike.util.Vector2
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.core.EnemyState
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos

class IsometricRenderer {

    var screenWidth = 1920
    var screenHeight = 1080
    var tileWidth = 64
    var tileHeight = 32

    var cameraX = 0f
    var cameraY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 0, 0, 0)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 80f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCAA88")
        textSize = 36f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    // Global animation time for ambient effects
    private var globalTime = 0f

    // Layer color themes
    private val layerColors = arrayOf(
        LayerTheme(
            floor = Color.parseColor("#2A1F3D"),
            floorAlt = Color.parseColor("#322548"),
            wall = Color.parseColor("#1A0F2D"),
            wallTop = Color.parseColor("#3D2E5A"),
            accent = Color.parseColor("#7B5EA7")
        ),
        LayerTheme(
            floor = Color.parseColor("#3D1F0A"),
            floorAlt = Color.parseColor("#4D2A14"),
            wall = Color.parseColor("#2A0F00"),
            wallTop = Color.parseColor("#6B3A1A"),
            accent = Color.parseColor("#FF6633")
        ),
        LayerTheme(
            floor = Color.parseColor("#1A3D1F"),
            floorAlt = Color.parseColor("#224D2A"),
            wall = Color.parseColor("#0A2D0F"),
            wallTop = Color.parseColor("#3A6B3A"),
            accent = Color.parseColor("#44BB66")
        )
    )

    fun updateScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun updateCamera(target: Vector2, room: Room) {
        val gridX = target.x / tileWidth
        val gridY = target.y / tileHeight
        val targetCamX = (gridX - gridY) * tileWidth / 2f
        val targetCamY = (gridX + gridY) * tileHeight / 2f

        if (cameraX == 0f && cameraY == 0f) {
            cameraX = targetCamX
            cameraY = targetCamY
            return
        }
        val lerp = 0.12f
        cameraX += (targetCamX - cameraX) * lerp
        cameraY += (targetCamY - cameraY) * lerp
    }

    fun worldToScreen(worldX: Float, worldY: Float): Pair<Float, Float> {
        val gridX = worldX / tileWidth
        val gridY = worldY / tileHeight
        val sx = (gridX - gridY) * tileWidth / 2f - cameraX + screenWidth / 2f
        val sy = (gridX + gridY) * tileHeight / 2f - cameraY + screenHeight / 2f
        return Pair(sx, sy)
    }

    fun worldToScreen(v: Vector2): Pair<Float, Float> = worldToScreen(v.x, v.y)

    fun renderRoom(canvas: Canvas, room: Room, playerPos: Vector2, dt: Float) {
        globalTime += dt
        updateCamera(playerPos, room)

        val theme = layerColors[room.layerIndex.coerceIn(0, 2)]

        for (row in 0 until room.height) {
            for (col in 0 until room.width) {
                val tile = room.getTile(col, row)
                val sx = (col - row) * tileWidth / 2f - cameraX + screenWidth / 2f
                val sy = (col + row) * tileHeight / 2f - cameraY + screenHeight / 2f

                if (sx < -tileWidth * 2 || sx > screenWidth + tileWidth * 2 ||
                    sy < -tileHeight * 4 || sy > screenHeight + tileHeight * 4
                ) continue

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
                }
            }
        }
    }

    private fun drawIsometricTile(canvas: Canvas, sx: Float, sy: Float, c1: Int, c2: Int, col: Int, row: Int) {
        val hw = tileWidth / 2f
        val hh = tileHeight / 2f
        val path = Path().apply {
            moveTo(sx, sy - hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx - hw, sy)
            close()
        }
        paint.color = if ((col + row) % 2 == 0) c1 else c2
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // Subtle inner pattern for floor detail
        if ((col * 7 + row * 13) % 5 == 0) {
            paint.color = Color.argb(15, 255, 255, 255)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, 2f, paint)
        }

        // Tile border
        paint.color = Color.argb(25, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawPath(path, paint)
    }

    private fun drawIsometricWall(canvas: Canvas, sx: Float, sy: Float, sideColor: Int, topColor: Int, accentColor: Int) {
        val hw = tileWidth / 2f
        val hh = tileHeight / 2f
        val wallHeight = 40f
        val leftFaceHeight = wallHeight * 0.4f

        // Left face
        paint.color = sideColor
        paint.style = Paint.Style.FILL
        val leftPath = Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx, sy + hh - leftFaceHeight)
            lineTo(sx - hw, sy - leftFaceHeight)
            close()
        }
        canvas.drawPath(leftPath, paint)

        // Right face
        paint.color = darken(sideColor, 0.7f)
        val rightPath = Path().apply {
            moveTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx + hw, sy - wallHeight)
            lineTo(sx, sy + hh - wallHeight)
            close()
        }
        canvas.drawPath(rightPath, paint)

        // Brick lines on right face
        paint.color = darken(sideColor, 0.5f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (i in 1..3) {
            val yLine = sy + hh - wallHeight * i / 4f
            val xLeft = sx + (hw) * (1f - i / 4f)
            val xRight = sx + hw
            canvas.drawLine(xLeft, yLine, xRight, yLine, paint)
        }

        // Top face
        paint.color = topColor
        paint.style = Paint.Style.FILL
        val topPath = Path().apply {
            moveTo(sx, sy - hh - wallHeight)
            lineTo(sx + hw, sy - wallHeight)
            lineTo(sx, sy + hh - wallHeight)
            lineTo(sx - hw, sy - wallHeight)
            close()
        }
        canvas.drawPath(topPath, paint)

        // Top edge highlight
        paint.color = lighten(topColor, 1.2f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(sx - hw, sy - wallHeight, sx, sy - hh - wallHeight, paint)
        canvas.drawLine(sx, sy - hh - wallHeight, sx + hw, sy - wallHeight, paint)
    }

    private fun drawIsometricObstacle(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = tileWidth / 3f
        val hh = tileHeight / 3f
        val height = 20f

        // Pillar base
        paint.color = color
        paint.style = Paint.Style.FILL
        val path = Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx, sy - hh)
            close()
        }
        canvas.drawPath(path, paint)

        // Top
        paint.color = lighten(color, 1.3f)
        val topPath = Path().apply {
            moveTo(sx - hw, sy - height)
            lineTo(sx, sy + hh - height)
            lineTo(sx + hw, sy - height)
            lineTo(sx, sy - hh - height)
            close()
        }
        canvas.drawPath(topPath, paint)

        // Left side
        paint.color = darken(color, 0.8f)
        canvas.drawPath(Path().apply {
            moveTo(sx - hw, sy)
            lineTo(sx, sy + hh)
            lineTo(sx, sy + hh - height)
            lineTo(sx - hw, sy - height)
            close()
        }, paint)

        // Right side
        paint.color = darken(color, 0.6f)
        canvas.drawPath(Path().apply {
            moveTo(sx, sy + hh)
            lineTo(sx + hw, sy)
            lineTo(sx + hw, sy - height)
            lineTo(sx, sy + hh - height)
            close()
        }, paint)

        // Gem/crystal on top
        val gemPulse = (sin(globalTime * 3f) * 0.3f + 0.7f)
        paint.color = Color.argb((180 * gemPulse).toInt(), Color.red(lighten(color, 1.8f)), Color.green(lighten(color, 1.8f)), Color.blue(lighten(color, 1.8f)))
        canvas.drawCircle(sx, sy - height - hh - 2f, 3f, paint)
    }

    fun renderShadow(canvas: Canvas, entity: Entity) {
        val (sx, sy) = worldToScreen(entity.position)
        val shadowW = entity.width * 0.8f
        val shadowH = entity.height * 0.3f
        canvas.drawOval(
            sx - shadowW / 2, sy - shadowH / 2 + entity.height / 3,
            sx + shadowW / 2, sy + shadowH / 2 + entity.height / 3,
            shadowPaint
        )
    }

    fun renderPlayer(canvas: Canvas, player: Player) {
        val (sx, sy) = worldToScreen(player.position)
        val facingRight = player.facingRight
        val isRunning = player.stateMachine.currentState == PlayerState.RUN
        val isIdle = player.stateMachine.currentState == PlayerState.IDLE
        val isHurt = player.stateMachine.currentState == PlayerState.HURT
        val isDashing = player.isDashing

        canvas.save()
        if (!facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        // Animation values
        val bob = if (isRunning) (sin(player.moveAnimPhase * Math.PI * 2) * 3f).toFloat() else 0f
        val legSwing = if (isRunning) (sin(player.moveAnimPhase * Math.PI * 2) * 6f).toFloat() else 0f
        val breathe = if (isIdle) (sin(player.idleTime * 2.5f) * 1f).toFloat() else 0f
        val bodyOffset = bob + breathe

        // Flash when hurt
        val bodyColor = if (isHurt && player.hurtTimer % 0.1f < 0.05f) Color.WHITE else Color.parseColor("#CC3333")

        // === LEGS ===
        paint.style = Paint.Style.FILL
        // Left leg
        paint.color = Color.parseColor("#992222")
        canvas.drawRect(sx - 8f, sy - 6f, sx - 2f, sy + 8f + legSwing, paint)
        // Right leg
        canvas.drawRect(sx + 2f, sy - 6f, sx + 8f, sy + 8f - legSwing, paint)
        // Boots
        paint.color = Color.parseColor("#661111")
        canvas.drawRect(sx - 9f, sy + 5f + legSwing, sx - 1f, sy + 10f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy + 5f - legSwing, sx + 9f, sy + 10f - legSwing, paint)

        // === CAPE ===
        paint.color = Color.parseColor("#881111")
        val capeSwing = if (isRunning) (sin(player.moveAnimPhase * Math.PI * 2 + 1f) * 4f).toFloat() else (sin(player.idleTime * 1.5f) * 1f).toFloat()
        // Cape left side
        canvas.drawRect(sx - 13f + capeSwing, sy - 36f + bodyOffset, sx - 6f + capeSwing * 0.5f, sy - 8f + bodyOffset, paint)
        // Cape right side
        canvas.drawRect(sx + 6f - capeSwing * 0.5f, sy - 36f + bodyOffset, sx + 13f - capeSwing, sy - 8f + bodyOffset, paint)
        // Cape trim
        paint.color = Color.parseColor("#AA2222")
        canvas.drawRect(sx - 13f + capeSwing, sy - 36f + bodyOffset, sx - 12f + capeSwing, sy - 8f + bodyOffset, paint)
        canvas.drawRect(sx + 12f - capeSwing, sy - 36f + bodyOffset, sx + 13f - capeSwing, sy - 8f + bodyOffset, paint)

        // === BODY (torso) ===
        paint.color = bodyColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 38f + bodyOffset, sx + 10f, sy - 6f + bodyOffset, paint)
        // Armor plate
        paint.color = Color.parseColor("#DD4444")
        canvas.drawRect(sx - 8f, sy - 36f + bodyOffset, sx + 8f, sy - 22f + bodyOffset, paint)
        // Armor trim
        paint.color = Color.parseColor("#EE6666")
        canvas.drawRect(sx - 8f, sy - 36f + bodyOffset, sx + 8f, sy - 34f + bodyOffset, paint)
        // Belt
        paint.color = Color.parseColor("#886633")
        canvas.drawRect(sx - 10f, sy - 10f + bodyOffset, sx + 10f, sy - 6f + bodyOffset, paint)
        // Belt buckle
        paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 2f, sy - 10f + bodyOffset, sx + 2f, sy - 6f + bodyOffset, paint)

        // === ARMS ===
        paint.color = bodyColor
        // Left arm
        val armSwing = if (isRunning) (sin(player.moveAnimPhase * Math.PI * 2 + 0.5f) * 3f).toFloat() else 0f
        canvas.drawRect(sx - 14f, sy - 34f + bodyOffset, sx - 10f, sy - 14f + bodyOffset + armSwing, paint)
        // Right arm (holds sword)
        canvas.drawRect(sx + 10f, sy - 34f + bodyOffset, sx + 14f, sy - 14f + bodyOffset - armSwing, paint)
        // Gauntlets
        paint.color = Color.parseColor("#DD4444")
        canvas.drawRect(sx - 15f, sy - 18f + bodyOffset + armSwing, sx - 9f, sy - 14f + bodyOffset + armSwing, paint)
        canvas.drawRect(sx + 9f, sy - 18f + bodyOffset - armSwing, sx + 15f, sy - 14f + bodyOffset - armSwing, paint)

        // === HEAD ===
        paint.color = Color.parseColor("#FFCC99")
        paint.style = Paint.Style.FILL
        canvas.drawOval(sx - 7f, sy - 50f + bodyOffset, sx + 7f, sy - 38f + bodyOffset, paint)

        // === HAIR ===
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(sx - 8f, sy - 52f + bodyOffset, sx + 8f, sy - 44f + bodyOffset, paint)
        // Hair strands flowing
        val hairSwing = if (isRunning) (sin(player.moveAnimPhase * Math.PI * 2 + 2f) * 2f).toFloat() else 0f
        canvas.drawRect(sx - 10f + hairSwing, sy - 50f + bodyOffset, sx - 6f + hairSwing, sy - 40f + bodyOffset, paint)
        canvas.drawRect(sx + 6f - hairSwing, sy - 50f + bodyOffset, sx + 10f - hairSwing, sy - 42f + bodyOffset, paint)
        // Hair highlight
        paint.color = Color.parseColor("#EEEEFF")
        canvas.drawRect(sx - 4f, sy - 51f + bodyOffset, sx + 2f, sy - 46f + bodyOffset, paint)

        // === FACE ===
        // Eyes
        paint.color = Color.parseColor("#222266")
        canvas.drawCircle(sx + 3f, sy - 45f + bodyOffset, 1.5f, paint)
        // Eye glow (determined look)
        paint.color = Color.parseColor("#4444AA")
        canvas.drawCircle(sx + 3f, sy - 45.5f + bodyOffset, 0.8f, paint)
        // Mouth (slight smirk)
        paint.color = Color.parseColor("#CC8866")
        canvas.drawPoint(sx + 2f, sy - 41f + bodyOffset, paint)

        // === SWORD ===
        drawPlayerSword(canvas, sx, sy + bodyOffset, player)

        // === DASH TRAIL ===
        if (isDashing) {
            paint.color = Color.argb(80, 100, 150, 255)
            paint.style = Paint.Style.FILL
            canvas.drawOval(sx - 18f, sy - 35f, sx + 18f, sy + 8f, paint)
            // Afterimage
            paint.color = Color.argb(40, 150, 180, 255)
            canvas.drawOval(sx - 14f, sy - 30f, sx + 14f, sy + 4f, paint)
        }

        // === RUNNING EFFECTS ===
        if (isRunning) {
            // Dust particles
            val dustAlpha = (abs(legSwing) / 6f * 80f).toInt().coerceIn(0, 80)
            paint.color = Color.argb(dustAlpha, 180, 160, 140)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx - 6f, sy + 12f, 2.5f, paint)
            canvas.drawCircle(sx + 6f, sy + 12f, 2.5f, paint)
            // Speed lines
            paint.color = Color.argb(dustAlpha / 2, 200, 180, 160)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(sx - 16f, sy - 20f + bodyOffset, sx - 22f, sy - 18f + bodyOffset, paint)
            canvas.drawLine(sx - 16f, sy - 10f + bodyOffset, sx - 22f, sy - 8f + bodyOffset, paint)
        }

        // === IDLE GLOW ===
        if (isIdle) {
            val glowPulse = (sin(player.idleTime * 2f) * 0.3f + 0.7f)
            paint.color = Color.argb((20 * glowPulse).toInt(), 255, 200, 100)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 25f + bodyOffset, 18f, paint)
        }

        canvas.restore()

        // Health bar above player (not flipped)
        if (player.health < player.maxHealth) {
            val barW = 30f
            val barH = 4f
            val hpRatio = player.health.toFloat() / player.maxHealth
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx + barW / 2, sy - 58f + barH + bodyOffset, paint)
            paint.color = Color.GREEN
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx - barW / 2 + barW * hpRatio, sy - 58f + barH + bodyOffset, paint)
            // Health bar border
            paint.color = Color.argb(100, 255, 255, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(sx - barW / 2, sy - 58f + bodyOffset, sx + barW / 2, sy - 58f + barH + bodyOffset, paint)
        }
    }

    private fun drawPlayerSword(canvas: Canvas, sx: Float, sy: Float, player: Player) {
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.ROUND

        when {
            player.isAttacking1 -> {
                // Slash forward - blade
                paint.color = Color.parseColor("#EEEEEE")
                paint.strokeWidth = 4f
                canvas.drawLine(sx + 10f, sy - 32f, sx + 38f, sy - 18f, paint)
                // Blade edge highlight
                paint.color = Color.parseColor("#FFFFFF")
                paint.strokeWidth = 2f
                canvas.drawLine(sx + 12f, sy - 31f, sx + 36f, sy - 19f, paint)
                // Handle
                paint.color = Color.parseColor("#886633")
                paint.strokeWidth = 5f
                canvas.drawLine(sx + 8f, sy - 34f, sx + 10f, sy - 28f, paint)
                // Pommel
                paint.color = Color.parseColor("#FFD700")
                paint.style = Paint.Style.FILL
                canvas.drawCircle(sx + 8f, sy - 35f, 2f, paint)
                // Slash effect arc
                paint.color = Color.argb(180, 255, 200, 100)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawArc(sx - 5f, sy - 48f, sx + 42f, sy + 5f, -50f, 100f, false, paint)
                // Inner arc glow
                paint.color = Color.argb(100, 255, 255, 200)
                paint.strokeWidth = 2f
                canvas.drawArc(sx, sy - 42f, sx + 36f, sy, -45f, 90f, false, paint)
            }
            player.isAttacking2 -> {
                // Upward slash - blade
                paint.color = Color.parseColor("#EEEEEE")
                paint.strokeWidth = 4f
                canvas.drawLine(sx + 8f, sy - 16f, sx + 4f, sy - 48f, paint)
                // Blade highlight
                paint.color = Color.parseColor("#FFFFFF")
                paint.strokeWidth = 2f
                canvas.drawLine(sx + 9f, sy - 18f, sx + 5f, sy - 46f, paint)
                // Handle
                paint.color = Color.parseColor("#886633")
                paint.strokeWidth = 5f
                canvas.drawLine(sx + 9f, sy - 14f, sx + 8f, sy - 18f, paint)
                // Pommel
                paint.color = Color.parseColor("#FFD700")
                paint.style = Paint.Style.FILL
                canvas.drawCircle(sx + 9f, sy - 13f, 2f, paint)
                // Slash arc
                paint.color = Color.argb(180, 255, 180, 80)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawArc(sx - 15f, sy - 55f, sx + 28f, sy, -65f, 130f, false, paint)
            }
            player.isAttacking3 -> {
                // Spin slash - blade
                paint.color = Color.parseColor("#EEEEEE")
                paint.strokeWidth = 5f
                canvas.drawLine(sx - 18f, sy - 26f, sx + 22f, sy - 26f, paint)
                // Blade highlight
                paint.color = Color.parseColor("#FFFFFF")
                paint.strokeWidth = 2f
                canvas.drawLine(sx - 16f, sy - 27f, sx + 20f, sy - 27f, paint)
                // Handle
                paint.color = Color.parseColor("#886633")
                paint.strokeWidth = 6f
                canvas.drawLine(sx - 20f, sy - 26f, sx - 18f, sy - 26f, paint)
                // Spin effect
                paint.color = Color.argb(200, 255, 150, 50)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(sx, sy - 26f, 28f, paint)
                // Inner glow
                paint.color = Color.argb(120, 255, 220, 100)
                paint.strokeWidth = 2f
                canvas.drawCircle(sx, sy - 26f, 22f, paint)
            }
            else -> {
                // Idle sword - held at side
                paint.color = Color.parseColor("#CCCCCC")
                paint.strokeWidth = 3f
                canvas.drawLine(sx + 12f, sy - 22f, sx + 16f, sy - 2f, paint)
                // Blade tip
                paint.color = Color.parseColor("#EEEEEE")
                paint.strokeWidth = 2f
                canvas.drawLine(sx + 15f, sy - 6f, sx + 16f, sy - 2f, paint)
                // Handle
                paint.color = Color.parseColor("#886633")
                paint.style = Paint.Style.FILL
                canvas.drawRect(sx + 10f, sy - 24f, sx + 18f, sy - 20f, paint)
                // Pommel gem
                paint.color = Color.parseColor("#FF4444")
                canvas.drawCircle(sx + 14f, sy - 22f, 1.5f, paint)
                // Cross guard
                paint.color = Color.parseColor("#FFD700")
                canvas.drawRect(sx + 9f, sy - 20f, sx + 19f, sy - 18f, paint)
            }
        }
        paint.strokeWidth = 1f
        paint.strokeCap = Paint.Cap.BUTT
    }

    fun renderEnemy(canvas: Canvas, enemy: Enemy) {
        val (sx, sy) = worldToScreen(enemy.position)

        canvas.save()
        if (!enemy.facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        val isMoving = enemy.stateMachine.currentState == EnemyState.CHASE || enemy.stateMachine.currentState == EnemyState.PATROL
        val isIdle = enemy.stateMachine.currentState == EnemyState.IDLE
        val isHurt = enemy.stateMachine.currentState == EnemyState.HURT
        val bob = if (isMoving) (sin(enemy.moveAnimPhase * Math.PI * 2) * 2f).toFloat() else 0f
        val breathe = if (isIdle) (sin(enemy.idleTime * 2f) * 1f).toFloat() else 0f
        val bodyOffset = bob + breathe
        val legSwing = if (isMoving) (sin(enemy.moveAnimPhase * Math.PI * 2) * 4f).toFloat() else 0f

        // Hurt flash
        val hurtFlash = isHurt && enemy.stateTimer % 0.1f < 0.05f

        // Enemy body - drawn by type
        when (enemy.type) {
            EnemyType.SKELETON -> drawSkeleton(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.WRAITH -> drawWraith(canvas, sx, sy, bodyOffset, isMoving, isIdle, hurtFlash)
            EnemyType.MEGA_SKELETON -> drawMegaSkeleton(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.FLAME_DANCER -> drawFlameDancer(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.LAVA_CASTER -> drawLavaCaster(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.INFERNO_TITAN -> drawInfernoTitan(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.SHIELD_BEARER -> drawShieldBearer(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash, enemy.shieldDirection)
            EnemyType.SPEAR_THROWER -> drawSpearThrower(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
            EnemyType.CHAMPION -> drawChampion(canvas, sx, sy, bodyOffset, legSwing, isMoving, isIdle, hurtFlash)
        }

        canvas.restore()

        // Health bar
        if (enemy.health < enemy.maxHealth) {
            val barW = enemy.width.toFloat()
            val barH = 4f
            val hpRatio = enemy.health.toFloat() / enemy.maxHealth
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx + barW / 2, sy - enemy.height - 8f + barH, paint)
            paint.color = Color.GREEN
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx - barW / 2 + barW * hpRatio, sy - enemy.height - 8f + barH, paint)
            // Border
            paint.color = Color.argb(80, 255, 255, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(sx - barW / 2, sy - enemy.height - 8f, sx + barW / 2, sy - enemy.height - 8f + barH, paint)
        }

        // Boss name
        if (enemy.isBoss) {
            textPaint.color = Color.parseColor("#FF6644")
            textPaint.textSize = 28f
            canvas.drawText(enemy.name, sx - textPaint.measureText(enemy.name) / 2, sy - enemy.height - 16f, textPaint)
        }
    }

    private fun drawSkeleton(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs (bone-like)
        paint.color = flash ?: Color.parseColor("#B0A480")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 2f, sy + 6f + legSwing, paint)
        canvas.drawRect(sx + 2f, sy - 4f, sx + 6f, sy + 6f - legSwing, paint)
        // Feet
        paint.color = flash ?: Color.parseColor("#908060")
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 8f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 8f - legSwing, paint)

        // Ribcage body
        paint.color = flash ?: Color.parseColor("#D4C8A0")
        canvas.drawRect(sx - 8f, sy - 30f + bob, sx + 8f, sy - 4f + bob, paint)
        // Rib lines
        paint.color = flash ?: Color.parseColor("#B0A480")
        for (i in 0..2) {
            val ribY = sy - 26f + i * 7f + bob
            canvas.drawLine(sx - 6f, ribY, sx + 6f, ribY, paint)
        }
        // Spine
        canvas.drawLine(sx, sy - 28f + bob, sx, sy - 6f + bob, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#C4B890")
        canvas.drawRect(sx - 12f, sy - 26f + bob, sx - 8f, sy - 10f + bob, paint)
        canvas.drawRect(sx + 8f, sy - 26f + bob, sx + 12f, sy - 10f + bob, paint)

        // Skull
        paint.color = flash ?: Color.parseColor("#E8DCC0")
        canvas.drawCircle(sx, sy - 36f + bob, 8f, paint)
        // Jaw
        paint.color = flash ?: Color.parseColor("#D8CCB0")
        canvas.drawRect(sx - 5f, sy - 32f + bob, sx + 5f, sy - 29f + bob, paint)
        // Eye sockets
        paint.color = Color.RED
        canvas.drawCircle(sx - 3f, sy - 37f + bob, 2.5f, paint)
        canvas.drawCircle(sx + 3f, sy - 37f + bob, 2.5f, paint)
        // Eye glow
        paint.color = Color.parseColor("#FF4444")
        canvas.drawCircle(sx - 3f, sy - 37f + bob, 1.2f, paint)
        canvas.drawCircle(sx + 3f, sy - 37f + bob, 1.2f, paint)
        // Nose hole
        paint.color = flash ?: Color.parseColor("#A09070")
        canvas.drawPoint(sx, sy - 34f + bob, paint)

        // Sword
        paint.color = Color.parseColor("#888888")
        paint.strokeWidth = 2f
        canvas.drawLine(sx + 12f, sy - 20f + bob, sx + 22f, sy - 8f + bob, paint)
        // Sword edge
        paint.color = Color.parseColor("#AAAAAA")
        paint.strokeWidth = 1f
        canvas.drawLine(sx + 13f, sy - 19f + bob, sx + 21f, sy - 9f + bob, paint)
        // Handle
        paint.color = Color.parseColor("#554433")
        paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 10f, sy - 22f + bob, sx + 14f, sy - 18f + bob, paint)

        // Idle sway
        if (isIdle) {
            paint.color = Color.argb(30, 255, 100, 100)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 20f + bob, 12f, paint)
        }
    }

    private fun drawWraith(canvas: Canvas, sx: Float, sy: Float, bob: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val floatBob = bob + sin(globalTime * 3f) * 2f
        val flash = if (hurtFlash) 220 else 160

        // Ghostly body with wavy edges
        paint.color = Color.argb(flash, 100, 50, 150)
        val waveOffset = sin(globalTime * 4f) * 2f
        val path = Path().apply {
            moveTo(sx - 10f, sy - 4f + floatBob)
            lineTo(sx - 10f, sy - 30f + floatBob)
            quadTo(sx, sy - 50f + floatBob, sx + 10f, sy - 30f + floatBob)
            lineTo(sx + 10f, sy - 4f + floatBob)
            // Wavy bottom
            lineTo(sx + 7f, sy + 4f + waveOffset + floatBob)
            lineTo(sx + 3f, sy - 2f - waveOffset + floatBob)
            lineTo(sx, sy + 5f + waveOffset + floatBob)
            lineTo(sx - 3f, sy - 2f - waveOffset + floatBob)
            lineTo(sx - 7f, sy + 4f + waveOffset + floatBob)
            close()
        }
        canvas.drawPath(path, paint)

        // Inner glow
        paint.color = Color.argb(flash / 2, 150, 100, 200)
        val innerPath = Path().apply {
            moveTo(sx - 6f, sy - 8f + floatBob)
            lineTo(sx - 6f, sy - 26f + floatBob)
            quadTo(sx, sy - 42f + floatBob, sx + 6f, sy - 26f + floatBob)
            lineTo(sx + 6f, sy - 8f + floatBob)
            close()
        }
        canvas.drawPath(innerPath, paint)

        // Eyes
        paint.color = Color.parseColor("#FF44FF")
        canvas.drawCircle(sx - 4f, sy - 35f + floatBob, 3.5f, paint)
        canvas.drawCircle(sx + 4f, sy - 35f + floatBob, 3.5f, paint)
        // Eye inner glow
        paint.color = Color.parseColor("#FF88FF")
        canvas.drawCircle(sx - 4f, sy - 35f + floatBob, 1.5f, paint)
        canvas.drawCircle(sx + 4f, sy - 35f + floatBob, 1.5f, paint)

        // Floating particles around wraith
        for (i in 0..2) {
            val px = sx + sin(globalTime * 2f + i * 2.1f) * 14f
            val py = sy - 20f + cos(globalTime * 2.5f + i * 1.7f) * 12f + floatBob
            paint.color = Color.argb(60, 180, 100, 255)
            canvas.drawCircle(px, py, 2f, paint)
        }
    }

    private fun drawMegaSkeleton(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        paint.color = flash ?: Color.parseColor("#988858")
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f + legSwing, paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f - legSwing, paint)
        // Feet
        paint.color = flash ?: Color.parseColor("#787038")
        canvas.drawRect(sx - 14f, sy + 6f + legSwing, sx - 3f, sy + 12f + legSwing, paint)
        canvas.drawRect(sx + 3f, sy + 6f - legSwing, sx + 14f, sy + 12f - legSwing, paint)

        // Large body
        paint.color = flash ?: Color.parseColor("#B8A878")
        canvas.drawRect(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, paint)
        // Rib lines
        paint.color = flash ?: Color.parseColor("#988858")
        for (i in 0..3) {
            val ribY = sy - 44f + i * 10f + bob
            canvas.drawLine(sx - 12f, ribY, sx + 12f, ribY, paint)
        }
        // Spine
        canvas.drawLine(sx, sy - 48f + bob, sx, sy - 6f + bob, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#A89868")
        canvas.drawRect(sx - 22f, sy - 44f + bob, sx - 16f, sy - 16f + bob, paint)
        canvas.drawRect(sx + 16f, sy - 44f + bob, sx + 22f, sy - 16f + bob, paint)
        // Fists
        paint.color = flash ?: Color.parseColor("#C8B888")
        canvas.drawCircle(sx - 19f, sy - 14f + bob, 4f, paint)
        canvas.drawCircle(sx + 19f, sy - 14f + bob, 4f, paint)

        // Skull
        paint.color = flash ?: Color.parseColor("#D8C898")
        canvas.drawCircle(sx, sy - 58f + bob, 14f, paint)
        // Eye sockets (glowing)
        paint.color = Color.parseColor("#FF4400")
        canvas.drawCircle(sx - 5f, sy - 60f + bob, 4f, paint)
        canvas.drawCircle(sx + 5f, sy - 60f + bob, 4f, paint)
        // Eye inner fire
        paint.color = Color.parseColor("#FF8800")
        canvas.drawCircle(sx - 5f, sy - 60f + bob, 2f, paint)
        canvas.drawCircle(sx + 5f, sy - 60f + bob, 2f, paint)
        // Jaw
        paint.color = flash ?: Color.parseColor("#A89868")
        canvas.drawRect(sx - 10f, sy - 50f + bob, sx + 10f, sy - 46f + bob, paint)
        // Teeth
        paint.color = flash ?: Color.parseColor("#E8D8A8")
        for (i in -3..3) {
            canvas.drawRect(sx + i * 3f - 1f, sy - 50f + bob, sx + i * 3f + 1f, sy - 47f + bob, paint)
        }

        // Giant sword
        paint.color = Color.parseColor("#666666")
        paint.strokeWidth = 5f
        canvas.drawLine(sx + 22f, sy - 48f + bob, sx + 38f, sy - 8f + bob, paint)
        // Sword edge
        paint.color = Color.parseColor("#888888")
        paint.strokeWidth = 2f
        canvas.drawLine(sx + 23f, sy - 46f + bob, sx + 37f, sy - 10f + bob, paint)
        paint.strokeWidth = 1f
        // Handle
        paint.color = Color.parseColor("#443322")
        paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 18f, sy - 50f + bob, sx + 26f, sy - 46f + bob, paint)
    }

    private fun drawFlameDancer(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        paint.color = flash ?: Color.parseColor("#CC4400")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, paint)
        // Burning feet
        paint.color = Color.argb(150, 255, 100, 0)
        canvas.drawCircle(sx - 4f, sy + 7f + legSwing, 3f, paint)
        canvas.drawCircle(sx + 4f, sy + 7f - legSwing, 3f, paint)

        // Body
        paint.color = flash ?: Color.parseColor("#FF6622")
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, paint)
        // Lava cracks on body
        paint.color = Color.parseColor("#FFAA00")
        canvas.drawRect(sx - 2f, sy - 28f + bob, sx + 2f, sy - 10f + bob, paint)
        canvas.drawRect(sx - 5f, sy - 20f + bob, sx + 5f, sy - 18f + bob, paint)

        // Arms with flame
        paint.color = flash ?: Color.parseColor("#FF5511")
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, paint)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, paint)
        // Hand flames
        val flamePulse = sin(globalTime * 8f) * 2f
        paint.color = Color.parseColor("#FFAA00")
        canvas.drawCircle(sx - 10f, sy - 14f + bob + flamePulse, 4f, paint)
        canvas.drawCircle(sx + 10f, sy - 14f + bob - flamePulse, 4f, paint)
        paint.color = Color.parseColor("#FFDD44")
        canvas.drawCircle(sx - 10f, sy - 14f + bob + flamePulse, 2f, paint)
        canvas.drawCircle(sx + 10f, sy - 14f + bob - flamePulse, 2f, paint)

        // Head
        paint.color = flash ?: Color.parseColor("#FF8844")
        canvas.drawCircle(sx, sy - 38f + bob, 7f, paint)
        // Flame crown (animated)
        paint.color = Color.parseColor("#FFAA00")
        val crownWave = sin(globalTime * 6f)
        canvas.drawCircle(sx - 4f, sy - 45f + bob + crownWave, 4f, paint)
        canvas.drawCircle(sx + 4f, sy - 45f + bob - crownWave, 4f, paint)
        canvas.drawCircle(sx, sy - 48f + bob + crownWave * 0.5f, 5f, paint)
        // Crown tips
        paint.color = Color.parseColor("#FFDD44")
        canvas.drawCircle(sx - 4f, sy - 46f + bob + crownWave, 2f, paint)
        canvas.drawCircle(sx + 4f, sy - 46f + bob - crownWave, 2f, paint)
        canvas.drawCircle(sx, sy - 49f + bob + crownWave * 0.5f, 2.5f, paint)

        // Eyes
        paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 3f, sy - 39f + bob, 1.5f, paint)
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, paint)

        // Fire trail glow
        paint.color = Color.argb(100, 255, 100, 0)
        canvas.drawOval(sx - 14f, sy + 2f, sx + 14f, sy + 12f, paint)
    }

    private fun drawLavaCaster(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs (hidden under robe, slight peek)
        paint.color = flash ?: Color.parseColor("#551100")
        canvas.drawRect(sx - 5f, sy - 2f, sx - 1f, sy + 5f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy - 2f, sx + 5f, sy + 5f - legSwing, paint)

        // Robe body (trapezoid)
        paint.color = flash ?: Color.parseColor("#882200")
        val robe = Path().apply {
            moveTo(sx - 10f, sy + 6f)
            lineTo(sx - 8f, sy - 28f + bob)
            lineTo(sx + 8f, sy - 28f + bob)
            lineTo(sx + 10f, sy + 6f)
            close()
        }
        canvas.drawPath(robe, paint)
        // Robe pattern
        paint.color = Color.argb(40, 255, 100, 0)
        canvas.drawRect(sx - 4f, sy - 24f + bob, sx + 4f, sy - 8f + bob, paint)
        // Robe trim
        paint.color = Color.parseColor("#AA4400")
        canvas.drawRect(sx - 10f, sy + 4f, sx + 10f, sy + 6f, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#772200")
        canvas.drawRect(sx - 13f, sy - 24f + bob, sx - 9f, sy - 10f + bob, paint)
        canvas.drawRect(sx + 9f, sy - 24f + bob, sx + 13f, sy - 10f + bob, paint)

        // Head
        paint.color = flash ?: Color.parseColor("#FF4400")
        canvas.drawCircle(sx, sy - 34f + bob, 7f, paint)
        // Glowing eyes
        paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 3f, sy - 35f + bob, 2f, paint)
        canvas.drawCircle(sx + 3f, sy - 35f + bob, 2f, paint)
        // Eye glow aura
        paint.color = Color.argb(60, 255, 255, 0)
        canvas.drawCircle(sx - 3f, sy - 35f + bob, 4f, paint)
        canvas.drawCircle(sx + 3f, sy - 35f + bob, 4f, paint)

        // Hood
        paint.color = flash ?: Color.parseColor("#661100")
        val hood = Path().apply {
            moveTo(sx - 9f, sy - 30f + bob)
            quadTo(sx, sy - 48f + bob, sx + 9f, sy - 30f + bob)
            close()
        }
        canvas.drawPath(hood, paint)

        // Staff
        paint.color = Color.parseColor("#553300")
        paint.strokeWidth = 3f
        canvas.drawLine(sx + 12f, sy - 38f + bob, sx + 12f, sy + 5f, paint)
        paint.strokeWidth = 1f
        // Staff orb
        val orbPulse = sin(globalTime * 4f) * 2f
        paint.color = Color.parseColor("#FF6600")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 6f, paint)
        paint.color = Color.parseColor("#FFAA00")
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 4f, paint)
        paint.color = Color.parseColor("#FFFF44")
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 2f, paint)
        // Orb glow
        paint.color = Color.argb(40, 255, 150, 0)
        canvas.drawCircle(sx + 12f, sy - 40f + bob + orbPulse, 10f, paint)
    }

    private fun drawInfernoTitan(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        paint.color = flash ?: Color.parseColor("#661100")
        canvas.drawRect(sx - 14f, sy - 4f, sx - 4f, sy + 12f + legSwing, paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 14f, sy + 12f - legSwing, paint)
        // Lava on legs
        paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 10f, sy - 2f, sx - 6f, sy + 8f + legSwing, paint)
        canvas.drawRect(sx + 6f, sy - 2f, sx + 10f, sy + 8f - legSwing, paint)

        // Massive body
        paint.color = flash ?: Color.parseColor("#882200")
        canvas.drawRect(sx - 20f, sy - 55f + bob, sx + 20f, sy - 4f + bob, paint)
        // Lava cracks
        paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 5f, sy - 50f + bob, sx + 5f, sy - 20f + bob, paint)
        canvas.drawRect(sx - 15f, sy - 35f + bob, sx + 15f, sy - 30f + bob, paint)
        canvas.drawRect(sx - 12f, sy - 18f + bob, sx + 12f, sy - 15f + bob, paint)
        // Lava glow
        paint.color = Color.parseColor("#FFAA00")
        canvas.drawRect(sx - 3f, sy - 45f + bob, sx + 3f, sy - 25f + bob, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#771100")
        canvas.drawRect(sx - 28f, sy - 50f + bob, sx - 20f, sy - 15f + bob, paint)
        canvas.drawRect(sx + 20f, sy - 50f + bob, sx + 28f, sy - 15f + bob, paint)
        // Fists
        paint.color = flash ?: Color.parseColor("#993300")
        canvas.drawCircle(sx - 24f, sy - 13f + bob, 6f, paint)
        canvas.drawCircle(sx + 24f, sy - 13f + bob, 6f, paint)
        // Fist lava glow
        paint.color = Color.argb(80, 255, 100, 0)
        canvas.drawCircle(sx - 24f, sy - 13f + bob, 8f, paint)
        canvas.drawCircle(sx + 24f, sy - 13f + bob, 8f, paint)

        // Head
        paint.color = flash ?: Color.parseColor("#AA3300")
        canvas.drawCircle(sx, sy - 63f + bob, 16f, paint)
        // Fire eyes
        paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 6f, sy - 65f + bob, 5f, paint)
        canvas.drawCircle(sx + 6f, sy - 65f + bob, 5f, paint)
        // Eye inner
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawCircle(sx - 6f, sy - 65f + bob, 2f, paint)
        canvas.drawCircle(sx + 6f, sy - 65f + bob, 2f, paint)
        // Mouth
        paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 6f, sy - 56f + bob, sx + 6f, sy - 52f + bob, paint)

        // Flame crown (animated)
        paint.color = Color.parseColor("#FF8800")
        val crownWave = sin(globalTime * 5f)
        for (i in -2..2) {
            val wave = sin(globalTime * 6f + i * 1.2f) * 3f
            canvas.drawCircle(sx + i * 8f, sy - 78f - abs(i) * 3f + bob + wave, 6f, paint)
        }
        // Crown tips
        paint.color = Color.parseColor("#FFCC00")
        for (i in -2..2) {
            val wave = sin(globalTime * 6f + i * 1.2f) * 3f
            canvas.drawCircle(sx + i * 8f, sy - 79f - abs(i) * 3f + bob + wave, 3f, paint)
        }
    }

    private fun drawShieldBearer(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean, shieldDir: Int) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        paint.color = flash ?: Color.parseColor("#224477")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, paint)
        // Boots
        paint.color = flash ?: Color.parseColor("#112255")
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 8f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 8f - legSwing, paint)

        // Body
        paint.color = flash ?: Color.parseColor("#336699")
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, paint)
        // Armor plate
        paint.color = flash ?: Color.parseColor("#4488BB")
        canvas.drawRect(sx - 6f, sy - 30f + bob, sx + 6f, sy - 16f + bob, paint)
        // Belt
        paint.color = Color.parseColor("#554422")
        canvas.drawRect(sx - 8f, sy - 8f + bob, sx + 8f, sy - 4f + bob, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#336699")
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, paint)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, paint)

        // Head
        paint.color = flash ?: Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 38f + bob, 7f, paint)
        // Helmet
        paint.color = flash ?: Color.parseColor("#4488BB")
        canvas.drawArc(sx - 9f, sy - 48f + bob, sx + 9f, sy - 34f + bob, 180f, 180f, true, paint)
        // Helmet visor
        paint.color = flash ?: Color.parseColor("#336699")
        canvas.drawRect(sx - 7f, sy - 40f + bob, sx + 7f, sy - 38f + bob, paint)
        // Helmet crest
        paint.color = Color.parseColor("#2255AA")
        canvas.drawRect(sx - 1f, sy - 50f + bob, sx + 1f, sy - 44f + bob, paint)
        // Eyes
        paint.color = Color.parseColor("#222244")
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, paint)

        // Shield
        paint.color = Color.parseColor("#5599DD")
        canvas.drawOval(sx + 8f, sy - 30f + bob, sx + 24f, sy - 8f + bob, paint)
        // Shield inner
        paint.color = Color.parseColor("#77BBFF")
        canvas.drawOval(sx + 11f, sy - 27f + bob, sx + 21f, sy - 11f + bob, paint)
        // Shield boss (center)
        paint.color = Color.parseColor("#FFD700")
        canvas.drawCircle(sx + 16f, sy - 19f + bob, 3f, paint)
        // Shield rim
        paint.color = Color.parseColor("#3377BB")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawOval(sx + 8f, sy - 30f + bob, sx + 24f, sy - 8f + bob, paint)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.FILL
    }

    private fun drawSpearThrower(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        paint.color = flash ?: Color.parseColor("#336633")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f - legSwing, paint)
        // Sandals
        paint.color = flash ?: Color.parseColor("#886644")
        canvas.drawRect(sx - 7f, sy + 4f + legSwing, sx - 1f, sy + 7f + legSwing, paint)
        canvas.drawRect(sx + 1f, sy + 4f - legSwing, sx + 7f, sy + 7f - legSwing, paint)

        // Body
        paint.color = flash ?: Color.parseColor("#448844")
        canvas.drawRect(sx - 8f, sy - 32f + bob, sx + 8f, sy - 4f + bob, paint)
        // Tunic
        paint.color = flash ?: Color.parseColor("#55AA55")
        canvas.drawRect(sx - 6f, sy - 28f + bob, sx + 6f, sy - 14f + bob, paint)
        // Belt
        paint.color = Color.parseColor("#665533")
        canvas.drawRect(sx - 8f, sy - 8f + bob, sx + 8f, sy - 4f + bob, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#448844")
        canvas.drawRect(sx - 12f, sy - 28f + bob, sx - 8f, sy - 12f + bob, paint)
        canvas.drawRect(sx + 8f, sy - 28f + bob, sx + 12f, sy - 12f + bob, paint)

        // Head
        paint.color = flash ?: Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 38f + bob, 7f, paint)
        // Helm
        paint.color = flash ?: Color.parseColor("#66AA66")
        canvas.drawRect(sx - 8f, sy - 46f + bob, sx + 8f, sy - 40f + bob, paint)
        // Helm crest
        paint.color = Color.parseColor("#448844")
        canvas.drawRect(sx - 1f, sy - 50f + bob, sx + 1f, sy - 46f + bob, paint)
        // Eyes
        paint.color = Color.parseColor("#224422")
        canvas.drawCircle(sx + 3f, sy - 39f + bob, 1.5f, paint)

        // Spear
        paint.color = Color.parseColor("#886644")
        paint.strokeWidth = 2f
        canvas.drawLine(sx + 10f, sy - 42f + bob, sx + 32f, sy - 58f + bob, paint)
        // Spear tip
        paint.color = Color.parseColor("#AAAAAA")
        paint.strokeWidth = 2f
        canvas.drawLine(sx + 30f, sy - 56f + bob, sx + 38f, sy - 62f + bob, paint)
        // Spear tip point
        paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 36f, sy - 63f + bob, sx + 40f, sy - 61f + bob, paint)
        paint.strokeWidth = 1f
        // Spear binding
        paint.color = Color.parseColor("#665533")
        paint.style = Paint.Style.FILL
        canvas.drawRect(sx + 28f, sy - 54f + bob, sx + 32f, sy - 52f + bob, paint)
    }

    private fun drawChampion(canvas: Canvas, sx: Float, sy: Float, bob: Float, legSwing: Float, isMoving: Boolean, isIdle: Boolean, hurtFlash: Boolean) {
        paint.style = Paint.Style.FILL
        val flash: Int? = if (hurtFlash) Color.WHITE else null

        // Legs
        paint.color = flash ?: Color.parseColor("#AA8833")
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f + legSwing, paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f - legSwing, paint)
        // Greaves
        paint.color = flash ?: Color.parseColor("#CCAA44")
        canvas.drawRect(sx - 11f, sy - 2f, sx - 5f, sy + 4f + legSwing, paint)
        canvas.drawRect(sx + 5f, sy - 2f, sx + 11f, sy + 4f - legSwing, paint)

        // Large body
        paint.color = flash ?: Color.parseColor("#CCAA44")
        canvas.drawRect(sx - 16f, sy - 50f + bob, sx + 16f, sy - 4f + bob, paint)
        // Armor plate
        paint.color = flash ?: Color.parseColor("#DDCC66")
        canvas.drawRect(sx - 12f, sy - 46f + bob, sx + 12f, sy - 24f + bob, paint)
        // Armor detail
        paint.color = Color.parseColor("#FFE888")
        canvas.drawRect(sx - 4f, sy - 42f + bob, sx + 4f, sy - 28f + bob, paint)
        // Belt
        paint.color = Color.parseColor("#886633")
        canvas.drawRect(sx - 16f, sy - 8f + bob, sx + 16f, sy - 4f + bob, paint)
        // Belt buckle
        paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 3f, sy - 8f + bob, sx + 3f, sy - 4f + bob, paint)

        // Arms
        paint.color = flash ?: Color.parseColor("#BBAA44")
        canvas.drawRect(sx - 22f, sy - 46f + bob, sx - 16f, sy - 18f + bob, paint)
        canvas.drawRect(sx + 16f, sy - 46f + bob, sx + 22f, sy - 18f + bob, paint)
        // Gauntlets
        paint.color = flash ?: Color.parseColor("#DDCC66")
        canvas.drawRect(sx - 24f, sy - 22f + bob, sx - 16f, sy - 16f + bob, paint)
        canvas.drawRect(sx + 16f, sy - 22f + bob, sx + 24f, sy - 16f + bob, paint)

        // Head
        paint.color = flash ?: Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 58f + bob, 12f, paint)
        // Crown
        paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 12f, sy - 72f + bob, sx + 12f, sy - 66f + bob, paint)
        canvas.drawRect(sx - 8f, sy - 78f + bob, sx - 4f, sy - 72f + bob, paint)
        canvas.drawRect(sx - 2f, sy - 80f + bob, sx + 2f, sy - 72f + bob, paint)
        canvas.drawRect(sx + 4f, sy - 78f + bob, sx + 8f, sy - 72f + bob, paint)
        // Crown gems
        paint.color = Color.parseColor("#FF2222")
        canvas.drawCircle(sx, sy - 76f + bob, 2f, paint)
        // Eyes
        paint.color = Color.parseColor("#442200")
        canvas.drawCircle(sx - 4f, sy - 60f + bob, 2f, paint)
        canvas.drawCircle(sx + 4f, sy - 60f + bob, 2f, paint)
        // Determined brow
        paint.color = flash ?: Color.parseColor("#DDAA66")
        canvas.drawRect(sx - 6f, sy - 63f + bob, sx - 2f, sy - 61f + bob, paint)
        canvas.drawRect(sx + 2f, sy - 63f + bob, sx + 6f, sy - 61f + bob, paint)

        // Shield
        paint.color = Color.parseColor("#DDAA22")
        canvas.drawOval(sx + 14f, sy - 42f + bob, sx + 34f, sy - 12f + bob, paint)
        // Shield inner
        paint.color = Color.parseColor("#FFCC44")
        canvas.drawOval(sx + 18f, sy - 38f + bob, sx + 30f, sy - 16f + bob, paint)
        // Shield emblem
        paint.color = Color.parseColor("#FFD700")
        canvas.drawCircle(sx + 24f, sy - 27f + bob, 4f, paint)
        // Shield rim
        paint.color = Color.parseColor("#BB8811")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawOval(sx + 14f, sy - 42f + bob, sx + 34f, sy - 12f + bob, paint)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.FILL

        // Spear
        paint.color = Color.parseColor("#886644")
        paint.strokeWidth = 3f
        canvas.drawLine(sx - 18f, sy - 52f + bob, sx - 38f, sy - 72f + bob, paint)
        paint.strokeWidth = 1f
        // Spear tip
        paint.color = Color.parseColor("#CCCCCC")
        paint.strokeWidth = 2f
        canvas.drawLine(sx - 36f, sy - 70f + bob, sx - 44f, sy - 78f + bob, paint)
        paint.strokeWidth = 1f
    }

    fun renderProjectile(canvas: Canvas, proj: Projectile) {
        val (sx, sy) = worldToScreen(proj.position)
        paint.style = Paint.Style.FILL

        when (proj.type) {
            ProjectileType.KNIFE -> {
                val knifeAngle = if (proj.target != null && !proj.target.isDead) {
                    proj.velocity.angle
                } else {
                    proj.angle
                }
                canvas.save()
                canvas.rotate(knifeAngle, sx, sy)
                // Blade
                paint.color = Color.parseColor("#CCCCCC")
                canvas.drawRect(sx - 8f, sy - 2f, sx + 8f, sy + 2f, paint)
                // Edge highlight
                paint.color = Color.parseColor("#EEEEEE")
                canvas.drawRect(sx - 6f, sy - 1f, sx + 6f, sy, paint)
                // Handle
                paint.color = Color.parseColor("#886633")
                canvas.drawRect(sx + 6f, sy - 3f, sx + 10f, sy + 3f, paint)
                canvas.restore()
                // Trail
                paint.color = Color.argb(80, 200, 200, 255)
                canvas.drawOval(sx - 10f, sy - 4f, sx + 10f, sy + 4f, paint)
            }
            ProjectileType.MAGIC_BOLT -> {
                // Outer glow
                paint.color = Color.argb(40, 153, 68, 255)
                canvas.drawCircle(sx, sy, 8f, paint)
                // Core
                paint.color = Color.parseColor("#9944FF")
                canvas.drawCircle(sx, sy, 5f, paint)
                // Inner bright
                paint.color = Color.parseColor("#CC88FF")
                canvas.drawCircle(sx, sy, 3f, paint)
                // Center
                paint.color = Color.parseColor("#EECCFF")
                canvas.drawCircle(sx, sy, 1.5f, paint)
            }
            ProjectileType.FIREBALL -> {
                // Outer glow
                paint.color = Color.argb(40, 255, 68, 0)
                canvas.drawCircle(sx, sy, 10f, paint)
                // Core
                paint.color = Color.parseColor("#FF4400")
                canvas.drawCircle(sx, sy, 7f, paint)
                // Mid
                paint.color = Color.parseColor("#FFAA00")
                canvas.drawCircle(sx, sy, 4f, paint)
                // Center
                paint.color = Color.parseColor("#FFFF44")
                canvas.drawCircle(sx, sy, 2f, paint)
            }
            ProjectileType.SPEAR -> {
                canvas.save()
                canvas.rotate(proj.angle, sx, sy)
                // Shaft
                paint.color = Color.parseColor("#886644")
                canvas.drawRect(sx - 12f, sy - 1.5f, sx + 12f, sy + 1.5f, paint)
                // Tip
                paint.color = Color.parseColor("#AAAAAA")
                canvas.drawRect(sx + 10f, sy - 3f, sx + 16f, sy + 3f, paint)
                // Tip point
                paint.color = Color.parseColor("#CCCCCC")
                canvas.drawRect(sx + 14f, sy - 1.5f, sx + 18f, sy + 1.5f, paint)
                canvas.restore()
            }
            ProjectileType.ZEUS_BOLT -> {
                // Outer glow
                paint.color = Color.argb(50, 68, 170, 255)
                canvas.drawCircle(sx, sy, 14f, paint)
                // Core
                paint.color = Color.parseColor("#44AAFF")
                canvas.drawCircle(sx, sy, 10f, paint)
                // Inner
                paint.color = Color.parseColor("#88CCFF")
                canvas.drawCircle(sx, sy, 6f, paint)
                // Center
                paint.color = Color.parseColor("#FFFFFF")
                canvas.drawCircle(sx, sy, 3f, paint)
                // Lightning tendrils
                paint.color = Color.argb(120, 150, 200, 255)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                for (i in 0..3) {
                    val angle = globalTime * 8f + i * 1.57f
                    val tx = sx + cos(angle) * 12f
                    val ty = sy + sin(angle) * 12f
                    canvas.drawLine(sx, sy, tx, ty, paint)
                }
                paint.strokeWidth = 1f
                paint.style = Paint.Style.FILL
            }
        }
    }

    fun renderParticle(canvas: Canvas, particle: Particle) {
        val (sx, sy) = worldToScreen(particle.position)
        val alpha = ((particle.life / particle.maxLife) * 255).toInt().coerceIn(0, 255)
        paint.color = Color.argb(alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color))
        paint.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy - particle.heightOffset, particle.size, paint)
    }

    fun renderDoor(canvas: Canvas, door: Door, room: Room) {
        val (sx, sy) = worldToScreen(door.position)
        paint.style = Paint.Style.FILL

        if (door.isLocked) {
            paint.color = Color.argb(100, 100, 100, 100)
        } else {
            paint.color = Color.argb(180, 255, 215, 0)
        }

        // Door arch
        val hw = tileWidth / 2f
        val hh = tileHeight / 2f
        val path = Path().apply {
            moveTo(sx - hw / 2, sy)
            lineTo(sx - hw / 2, sy - 30f)
            quadTo(sx, sy - 42f, sx + hw / 2, sy - 30f)
            lineTo(sx + hw / 2, sy)
            close()
        }
        canvas.drawPath(path, paint)

        // Door frame
        paint.color = Color.argb(120, 139, 90, 43)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawPath(path, paint)
        paint.strokeWidth = 1f

        if (!door.isLocked) {
            // Glow effect
            val glowPulse = (sin(globalTime * 3f) * 0.3f + 0.7f)
            paint.color = Color.argb((50 * glowPulse).toInt(), 255, 215, 0)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy - 15f, 22f, paint)
            // Door rune
            paint.color = Color.argb((150 * glowPulse).toInt(), 255, 200, 50)
            canvas.drawCircle(sx, sy - 15f, 4f, paint)
        }
    }

    fun renderMerchant(canvas: Canvas, merchant: Merchant) {
        val (sx, sy) = worldToScreen(merchant.position)

        paint.style = Paint.Style.FILL
        val breathe = sin(globalTime * 2f) * 1f

        // Legs
        paint.color = Color.parseColor("#664411")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, paint)
        // Boots
        paint.color = Color.parseColor("#553311")
        canvas.drawRect(sx - 7f, sy + 4f, sx - 1f, sy + 8f, paint)
        canvas.drawRect(sx + 1f, sy + 4f, sx + 7f, sy + 8f, paint)

        // Body
        paint.color = Color.parseColor("#885522")
        canvas.drawRect(sx - 8f, sy - 30f + breathe, sx + 8f, sy - 4f + breathe, paint)
        // Vest
        paint.color = Color.parseColor("#996633")
        canvas.drawRect(sx - 6f, sy - 28f + breathe, sx + 6f, sy - 12f + breathe, paint)
        // Belt
        paint.color = Color.parseColor("#553311")
        canvas.drawRect(sx - 8f, sy - 8f + breathe, sx + 8f, sy - 4f + breathe, paint)
        // Belt pouch
        paint.color = Color.parseColor("#776633")
        canvas.drawCircle(sx + 6f, sy - 6f + breathe, 3f, paint)

        // Backpack
        paint.color = Color.parseColor("#557733")
        canvas.drawRect(sx - 14f, sy - 28f + breathe, sx - 8f, sy - 10f + breathe, paint)
        // Backpack strap
        paint.color = Color.parseColor("#446622")
        canvas.drawRect(sx - 12f, sy - 28f + breathe, sx - 10f, sy - 10f + breathe, paint)
        // Backpack items peeking
        paint.color = Color.parseColor("#FFD700")
        canvas.drawCircle(sx - 11f, sy - 24f + breathe, 2f, paint)
        paint.color = Color.parseColor("#FF4444")
        canvas.drawCircle(sx - 11f, sy - 18f + breathe, 2f, paint)

        // Arms
        paint.color = Color.parseColor("#885522")
        canvas.drawRect(sx + 8f, sy - 26f + breathe, sx + 12f, sy - 12f + breathe, paint)

        // Head
        paint.color = Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 36f + breathe, 7f, paint)
        // Hat
        paint.color = Color.parseColor("#663300")
        canvas.drawRect(sx - 10f, sy - 46f + breathe, sx + 10f, sy - 40f + breathe, paint)
        canvas.drawRect(sx - 6f, sy - 52f + breathe, sx + 6f, sy - 46f + breathe, paint)
        // Hat brim detail
        paint.color = Color.parseColor("#552200")
        canvas.drawRect(sx - 10f, sy - 42f + breathe, sx + 10f, sy - 40f + breathe, paint)
        // Eyes
        paint.color = Color.parseColor("#443322")
        canvas.drawCircle(sx - 3f, sy - 37f + breathe, 1.5f, paint)
        canvas.drawCircle(sx + 3f, sy - 37f + breathe, 1.5f, paint)
        // Smile
        paint.color = Color.parseColor("#CC8866")
        canvas.drawPoint(sx, sy - 33f + breathe, paint)

        // "Talk" indicator
        val bounce = sin(globalTime * 4f) * 3f
        paint.color = Color.parseColor("#FFD700")
        textPaint.color = Color.parseColor("#FFD700")
        textPaint.textSize = 20f
        canvas.drawText("!", sx - 3f, sy - 55f + bounce, textPaint)
    }

    fun renderMenu(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.parseColor("#0A0515")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Decorative pillars
        paint.color = Color.parseColor("#1A0F2D")
        canvas.drawRect(w * 0.05f, h * 0.1f, w * 0.08f, h * 0.9f, paint)
        canvas.drawRect(w * 0.92f, h * 0.1f, w * 0.95f, h * 0.9f, paint)
        // Pillar detail
        paint.color = Color.parseColor("#2A1F3D")
        for (i in 0..4) {
            val y = h * 0.15f + i * h * 0.15f
            canvas.drawRect(w * 0.05f, y, w * 0.08f, y + 10f, paint)
            canvas.drawRect(w * 0.92f, y, w * 0.95f, y + 10f, paint)
        }

        // Background flames
        for (i in 0..5) {
            val fx = w * (0.1f + i * 0.15f)
            val fy = h * 0.85f
            val flameH = 30f + sin(globalTime * 3f + i * 1.5f) * 15f
            paint.color = Color.argb(30, 255, 80, 0)
            canvas.drawOval(fx - 15f, fy - flameH, fx + 15f, fy + 5f, paint)
            paint.color = Color.argb(20, 255, 150, 0)
            canvas.drawOval(fx - 8f, fy - flameH * 0.7f, fx + 8f, fy, paint)
        }

        // Title
        canvas.drawText("哈迪斯", w / 2f, h * 0.35f, titlePaint)

        // Subtitle
        canvas.drawText("地狱逃脱", w / 2f, h * 0.45f, subtitlePaint)

        // Tap to start
        val alpha = ((sin(globalTime * 3f) + 1) * 127).toInt()
        subtitlePaint.color = Color.argb(alpha, 200, 180, 150)
        canvas.drawText("点击开始", w / 2f, h * 0.65f, subtitlePaint)
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.argb(200, 20, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        titlePaint.color = Color.parseColor("#FF2222")
        canvas.drawText("游戏结束", w / 2f, h * 0.4f, titlePaint)

        subtitlePaint.color = Color.parseColor("#AA8888")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, subtitlePaint)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.argb(200, 0, 20, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        titlePaint.color = Color.parseColor("#FFD700")
        canvas.drawText("通关!", w / 2f, h * 0.4f, titlePaint)

        subtitlePaint.color = Color.parseColor("#DDAA66")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, subtitlePaint)
    }

    fun drawFade(canvas: Canvas, alpha: Float) {
        paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
    }

    private fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    data class LayerTheme(
        val floor: Int,
        val floorAlt: Int,
        val wall: Int,
        val wallTop: Int,
        val accent: Int
    )
}
