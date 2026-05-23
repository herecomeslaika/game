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
import kotlin.math.abs
import kotlin.math.sin

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
        // Camera target is the isometric screen position of the player
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
        // Convert world pixel coords to grid coords then project isometrically
        val gridX = worldX / tileWidth
        val gridY = worldY / tileHeight
        val sx = (gridX - gridY) * tileWidth / 2f - cameraX + screenWidth / 2f
        val sy = (gridX + gridY) * tileHeight / 2f - cameraY + screenHeight / 2f
        return Pair(sx, sy)
    }

    fun worldToScreen(v: Vector2): Pair<Float, Float> = worldToScreen(v.x, v.y)

    fun renderRoom(canvas: Canvas, room: Room, playerPos: Vector2) {
        updateCamera(playerPos, room)

        val theme = layerColors[room.layerIndex.coerceIn(0, 2)]

        for (row in 0 until room.height) {
            for (col in 0 until room.width) {
                val tile = room.getTile(col, row)
                // Use grid coordinates directly for isometric projection
                val sx = (col - row) * tileWidth / 2f - cameraX + screenWidth / 2f
                val sy = (col + row) * tileHeight / 2f - cameraY + screenHeight / 2f

                // Skip off-screen tiles
                if (sx < -tileWidth * 2 || sx > screenWidth + tileWidth * 2 ||
                    sy < -tileHeight * 4 || sy > screenHeight + tileHeight * 4
                ) continue

                when (tile) {
                    Room.TILE_FLOOR -> {
                        drawIsometricTile(canvas, sx, sy, theme.floor, theme.floorAlt, col, row)
                    }
                    Room.TILE_WALL -> {
                        drawIsometricWall(canvas, sx, sy, theme.wall, theme.wallTop)
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
            moveTo(sx, sy - hh)      // top
            lineTo(sx + hw, sy)      // right
            lineTo(sx, sy + hh)      // bottom
            lineTo(sx - hw, sy)      // left
            close()
        }
        paint.color = if ((col + row) % 2 == 0) c1 else c2
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // Tile border
        paint.color = Color.argb(30, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawPath(path, paint)
    }

    private fun drawIsometricWall(canvas: Canvas, sx: Float, sy: Float, sideColor: Int, topColor: Int) {
        val hw = tileWidth / 2f
        val hh = tileHeight / 2f
        val wallHeight = 40f
        val leftFaceHeight = wallHeight * 0.4f // Reduced left face to prevent entity overlap

        // Left face (shorter to avoid overlapping entities near left walls)
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

        // Top face
        paint.color = topColor
        val topPath = Path().apply {
            moveTo(sx, sy - hh - wallHeight)
            lineTo(sx + hw, sy - wallHeight)
            lineTo(sx, sy + hh - wallHeight)
            lineTo(sx - hw, sy - wallHeight)
            close()
        }
        canvas.drawPath(topPath, paint)
    }

    private fun drawIsometricObstacle(canvas: Canvas, sx: Float, sy: Float, color: Int) {
        val hw = tileWidth / 3f
        val hh = tileHeight / 3f
        val height = 20f

        // Pillar
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
        val isHurt = player.stateMachine.currentState == PlayerState.HURT

        canvas.save()
        if (!facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        // Walk animation: bob body up/down, alternate legs
        val bob = if (isRunning) (Math.sin(player.moveAnimPhase * Math.PI * 2) * 2f).toFloat() else 0f
        val legSwing = if (isRunning) (Math.sin(player.moveAnimPhase * Math.PI * 2) * 4f).toFloat() else 0f

        // Flash when hurt
        val bodyColor = if (isHurt && player.hurtTimer % 0.1f < 0.05f) Color.WHITE else Color.parseColor("#CC3333")

        // Body (bobbing)
        paint.color = bodyColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(sx - 10f, sy - 36f + bob, sx + 10f, sy - 4f + bob, paint)

        // Legs (alternating when running)
        paint.color = Color.parseColor("#992222")
        canvas.drawRect(sx - 8f, sy - 4f, sx - 2f, sy + 8f + legSwing, paint)
        canvas.drawRect(sx + 2f, sy - 4f, sx + 8f, sy + 8f - legSwing, paint)

        // Cape (flowing when running)
        paint.color = Color.parseColor("#881111")
        if (isRunning) {
            val capeSwing = (Math.sin(player.moveAnimPhase * Math.PI * 2 + 1f) * 3f).toFloat()
            canvas.drawRect(sx - 12f + capeSwing, sy - 34f + bob, sx - 6f + capeSwing, sy - 6f + bob, paint)
            canvas.drawRect(sx + 6f - capeSwing, sy - 34f + bob, sx + 12f - capeSwing, sy - 6f + bob, paint)
        }

        // Head
        paint.color = Color.parseColor("#FFCC99")
        canvas.drawOval(sx - 7f, sy - 48f + bob, sx + 7f, sy - 36f + bob, paint)

        // Hair
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(sx - 8f, sy - 50f + bob, sx + 8f, sy - 42f + bob, paint)
        // Hair strands
        canvas.drawRect(sx - 9f, sy - 48f + bob, sx - 6f, sy - 40f + bob, paint)

        // Eyes
        paint.color = Color.parseColor("#222266")
        canvas.drawCircle(sx + 3f, sy - 43f + bob, 1.5f, paint)

        // Sword
        drawPlayerSword(canvas, sx, sy + bob, player)

        // Dash trail effect
        if (player.isDashing) {
            paint.color = Color.argb(100, 100, 150, 255)
            paint.style = Paint.Style.FILL
            canvas.drawOval(sx - 15f, sy - 30f, sx + 15f, sy + 5f, paint)
        }

        // Running dust particles (visual only, rendered here)
        if (isRunning) {
            val dustAlpha = (Math.abs(legSwing) / 4f * 60f).toInt().coerceIn(0, 60)
            paint.color = Color.argb(dustAlpha, 180, 160, 140)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx - 5f, sy + 10f, 2f, paint)
            canvas.drawCircle(sx + 5f, sy + 10f, 2f, paint)
        }

        canvas.restore()

        // Health bar above player (not flipped)
        if (player.health < player.maxHealth) {
            val barW = 30f
            val barH = 4f
            val hpRatio = player.health.toFloat() / player.maxHealth
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            canvas.drawRect(sx - barW / 2, sy - 56f + bob, sx + barW / 2, sy - 56f + barH + bob, paint)
            paint.color = Color.GREEN
            canvas.drawRect(sx - barW / 2, sy - 56f + bob, sx - barW / 2 + barW * hpRatio, sy - 56f + barH + bob, paint)
        }
    }

    private fun drawPlayerSword(canvas: Canvas, sx: Float, sy: Float, player: Player) {
        paint.color = Color.parseColor("#CCCCCC")
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 3f

        when {
            player.isAttacking1 -> {
                // Slash forward
                paint.color = Color.parseColor("#DDDDDD")
                canvas.drawLine(sx + 10f, sy - 30f, sx + 35f, sy - 20f, paint)
                // Slash effect arc
                paint.color = Color.argb(150, 255, 200, 100)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawArc(sx - 5f, sy - 45f, sx + 40f, sy + 5f, -45f, 90f, false, paint)
            }
            player.isAttacking2 -> {
                // Upward slash
                paint.color = Color.parseColor("#DDDDDD")
                canvas.drawLine(sx + 8f, sy - 15f, sx + 5f, sy - 45f, paint)
                paint.color = Color.argb(150, 255, 180, 80)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawArc(sx - 15f, sy - 50f, sx + 25f, sy, -60f, 120f, false, paint)
            }
            player.isAttacking3 -> {
                // Spin slash
                paint.color = Color.parseColor("#DDDDDD")
                canvas.drawLine(sx - 15f, sy - 25f, sx + 20f, sy - 25f, paint)
                paint.color = Color.argb(180, 255, 150, 50)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                canvas.drawCircle(sx, sy - 25f, 25f, paint)
            }
            else -> {
                // Idle sword
                canvas.drawLine(sx + 10f, sy - 20f, sx + 14f, sy - 2f, paint)
                // Handle
                paint.color = Color.parseColor("#886633")
                canvas.drawRect(sx + 8f, sy - 20f, sx + 16f, sy - 16f, paint)
            }
        }
        paint.strokeWidth = 1f
    }

    fun renderEnemy(canvas: Canvas, enemy: Enemy) {
        val (sx, sy) = worldToScreen(enemy.position)

        canvas.save()
        if (!enemy.facingRight) {
            canvas.scale(-1f, 1f, sx, sy)
        }

        // Enemy body - drawn by type
        when (enemy.type) {
            EnemyType.SKELETON -> drawSkeleton(canvas, sx, sy)
            EnemyType.WRAITH -> drawWraith(canvas, sx, sy)
            EnemyType.MEGA_SKELETON -> drawMegaSkeleton(canvas, sx, sy)
            EnemyType.FLAME_DANCER -> drawFlameDancer(canvas, sx, sy)
            EnemyType.LAVA_CASTER -> drawLavaCaster(canvas, sx, sy)
            EnemyType.INFERNO_TITAN -> drawInfernoTitan(canvas, sx, sy)
            EnemyType.SHIELD_BEARER -> drawShieldBearer(canvas, sx, sy)
            EnemyType.SPEAR_THROWER -> drawSpearThrower(canvas, sx, sy)
            EnemyType.CHAMPION -> drawChampion(canvas, sx, sy)
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
        }

        // Boss name
        if (enemy.isBoss) {
            textPaint.color = Color.parseColor("#FF6644")
            textPaint.textSize = 28f
            canvas.drawText(enemy.name, sx - textPaint.measureText(enemy.name) / 2, sy - enemy.height - 16f, textPaint)
        }
    }

    private fun drawSkeleton(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Body
        paint.color = Color.parseColor("#D4C8A0")
        canvas.drawRect(sx - 8f, sy - 30f, sx + 8f, sy - 4f, paint)
        // Head (skull)
        paint.color = Color.parseColor("#E8DCC0")
        canvas.drawCircle(sx, sy - 36f, 8f, paint)
        // Eyes
        paint.color = Color.RED
        canvas.drawCircle(sx - 3f, sy - 37f, 2f, paint)
        canvas.drawCircle(sx + 3f, sy - 37f, 2f, paint)
        // Legs
        paint.color = Color.parseColor("#B0A480")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, paint)
        // Sword
        paint.color = Color.parseColor("#888888")
        canvas.drawLine(sx + 8f, sy - 18f, sx + 18f, sy - 8f, paint)
    }

    private fun drawWraith(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Ghostly body
        paint.color = Color.argb(160, 100, 50, 150)
        val path = Path().apply {
            moveTo(sx - 10f, sy - 4f)
            lineTo(sx - 10f, sy - 30f)
            quadTo(sx, sy - 48f, sx + 10f, sy - 30f)
            lineTo(sx + 10f, sy - 4f)
            // Wavy bottom
            lineTo(sx + 7f, sy + 4f)
            lineTo(sx + 3f, sy - 2f)
            lineTo(sx, sy + 4f)
            lineTo(sx - 3f, sy - 2f)
            lineTo(sx - 7f, sy + 4f)
            close()
        }
        canvas.drawPath(path, paint)
        // Eyes
        paint.color = Color.parseColor("#FF44FF")
        canvas.drawCircle(sx - 4f, sy - 34f, 3f, paint)
        canvas.drawCircle(sx + 4f, sy - 34f, 3f, paint)
    }

    private fun drawMegaSkeleton(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Large body
        paint.color = Color.parseColor("#B8A878")
        canvas.drawRect(sx - 16f, sy - 50f, sx + 16f, sy - 4f, paint)
        // Skull
        paint.color = Color.parseColor("#D8C898")
        canvas.drawCircle(sx, sy - 58f, 14f, paint)
        // Eyes (glowing)
        paint.color = Color.parseColor("#FF4400")
        canvas.drawCircle(sx - 5f, sy - 60f, 4f, paint)
        canvas.drawCircle(sx + 5f, sy - 60f, 4f, paint)
        // Jaw
        paint.color = Color.parseColor("#A89868")
        canvas.drawRect(sx - 10f, sy - 52f, sx + 10f, sy - 46f, paint)
        // Legs
        paint.color = Color.parseColor("#988858")
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f, paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f, paint)
        // Giant sword
        paint.color = Color.parseColor("#666666")
        paint.strokeWidth = 5f
        canvas.drawLine(sx + 16f, sy - 45f, sx + 35f, sy - 10f, paint)
        paint.strokeWidth = 1f
    }

    private fun drawFlameDancer(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Body
        paint.color = Color.parseColor("#FF6622")
        canvas.drawRect(sx - 8f, sy - 32f, sx + 8f, sy - 4f, paint)
        // Head
        paint.color = Color.parseColor("#FF8844")
        canvas.drawCircle(sx, sy - 38f, 7f, paint)
        // Flame crown
        paint.color = Color.parseColor("#FFAA00")
        canvas.drawCircle(sx - 4f, sy - 44f, 4f, paint)
        canvas.drawCircle(sx + 4f, sy - 44f, 4f, paint)
        canvas.drawCircle(sx, sy - 47f, 5f, paint)
        // Legs
        paint.color = Color.parseColor("#CC4400")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, paint)
        // Fire trail
        paint.color = Color.argb(120, 255, 100, 0)
        canvas.drawOval(sx - 12f, sy + 2f, sx + 12f, sy + 10f, paint)
    }

    private fun drawLavaCaster(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Robe body
        paint.color = Color.parseColor("#882200")
        val robe = Path().apply {
            moveTo(sx - 10f, sy + 6f)
            lineTo(sx - 8f, sy - 28f)
            lineTo(sx + 8f, sy - 28f)
            lineTo(sx + 10f, sy + 6f)
            close()
        }
        canvas.drawPath(robe, paint)
        // Head
        paint.color = Color.parseColor("#FF4400")
        canvas.drawCircle(sx, sy - 34f, 7f, paint)
        // Glowing eyes
        paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 3f, sy - 35f, 2f, paint)
        canvas.drawCircle(sx + 3f, sy - 35f, 2f, paint)
        // Staff
        paint.color = Color.parseColor("#553300")
        paint.strokeWidth = 3f
        canvas.drawLine(sx + 10f, sy - 35f, sx + 10f, sy + 5f, paint)
        paint.strokeWidth = 1f
        // Staff orb
        paint.color = Color.parseColor("#FF6600")
        canvas.drawCircle(sx + 10f, sy - 38f, 5f, paint)
    }

    private fun drawInfernoTitan(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Massive body
        paint.color = Color.parseColor("#882200")
        canvas.drawRect(sx - 20f, sy - 55f, sx + 20f, sy - 4f, paint)
        // Lava cracks
        paint.color = Color.parseColor("#FF4400")
        canvas.drawRect(sx - 5f, sy - 50f, sx + 5f, sy - 20f, paint)
        canvas.drawRect(sx - 15f, sy - 35f, sx + 15f, sy - 30f, paint)
        // Head
        paint.color = Color.parseColor("#AA3300")
        canvas.drawCircle(sx, sy - 63f, 16f, paint)
        // Fire eyes
        paint.color = Color.parseColor("#FFFF00")
        canvas.drawCircle(sx - 6f, sy - 65f, 5f, paint)
        canvas.drawCircle(sx + 6f, sy - 65f, 5f, paint)
        // Flame crown
        paint.color = Color.parseColor("#FF8800")
        for (i in -2..2) {
            canvas.drawCircle(sx + i * 8f, sy - 78f - abs(i) * 3f, 6f, paint)
        }
        // Legs
        paint.color = Color.parseColor("#661100")
        canvas.drawRect(sx - 14f, sy - 4f, sx - 4f, sy + 12f, paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 14f, sy + 12f, paint)
        // Arms
        canvas.drawRect(sx - 28f, sy - 50f, sx - 20f, sy - 15f, paint)
        canvas.drawRect(sx + 20f, sy - 50f, sx + 28f, sy - 15f, paint)
    }

    private fun drawShieldBearer(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Body
        paint.color = Color.parseColor("#336699")
        canvas.drawRect(sx - 8f, sy - 32f, sx + 8f, sy - 4f, paint)
        // Head
        paint.color = Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 38f, 7f, paint)
        // Helmet
        paint.color = Color.parseColor("#4488BB")
        canvas.drawArc(sx - 9f, sy - 48f, sx + 9f, sy - 34f, 180f, 180f, true, paint)
        // Shield
        paint.color = Color.parseColor("#5599DD")
        canvas.drawRect(sx + 8f, sy - 30f, sx + 22f, sy - 8f, paint)
        paint.color = Color.parseColor("#77BBFF")
        canvas.drawRect(sx + 10f, sy - 28f, sx + 20f, sy - 10f, paint)
        // Legs
        paint.color = Color.parseColor("#224477")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, paint)
    }

    private fun drawSpearThrower(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Body
        paint.color = Color.parseColor("#448844")
        canvas.drawRect(sx - 8f, sy - 32f, sx + 8f, sy - 4f, paint)
        // Head
        paint.color = Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 38f, 7f, paint)
        // Helm
        paint.color = Color.parseColor("#66AA66")
        canvas.drawRect(sx - 8f, sy - 46f, sx + 8f, sy - 40f, paint)
        // Spear
        paint.color = Color.parseColor("#886644")
        paint.strokeWidth = 2f
        canvas.drawLine(sx + 8f, sy - 40f, sx + 30f, sy - 55f, paint)
        // Spear tip
        paint.color = Color.parseColor("#AAAAAA")
        canvas.drawLine(sx + 28f, sy - 54f, sx + 35f, sy - 58f, paint)
        paint.strokeWidth = 1f
        // Legs
        paint.color = Color.parseColor("#336633")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, paint)
    }

    private fun drawChampion(canvas: Canvas, sx: Float, sy: Float) {
        paint.style = Paint.Style.FILL
        // Large body
        paint.color = Color.parseColor("#CCAA44")
        canvas.drawRect(sx - 16f, sy - 50f, sx + 16f, sy - 4f, paint)
        // Head
        paint.color = Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 58f, 12f, paint)
        // Crown
        paint.color = Color.parseColor("#FFD700")
        canvas.drawRect(sx - 12f, sy - 72f, sx + 12f, sy - 66f, paint)
        canvas.drawRect(sx - 8f, sy - 78f, sx - 4f, sy - 72f, paint)
        canvas.drawRect(sx - 2f, sy - 80f, sx + 2f, sy - 72f, paint)
        canvas.drawRect(sx + 4f, sy - 78f, sx + 8f, sy - 72f, paint)
        // Shield
        paint.color = Color.parseColor("#DDAA22")
        canvas.drawOval(sx + 14f, sy - 42f, sx + 32f, sy - 12f, paint)
        paint.color = Color.parseColor("#FFCC44")
        canvas.drawOval(sx + 18f, sy - 38f, sx + 28f, sy - 16f, paint)
        // Legs
        paint.color = Color.parseColor("#AA8833")
        canvas.drawRect(sx - 12f, sy - 4f, sx - 4f, sy + 10f, paint)
        canvas.drawRect(sx + 4f, sy - 4f, sx + 12f, sy + 10f, paint)
        // Spear
        paint.color = Color.parseColor("#886644")
        paint.strokeWidth = 3f
        canvas.drawLine(sx - 16f, sy - 50f, sx - 35f, sy - 70f, paint)
        paint.strokeWidth = 1f
    }

    fun renderProjectile(canvas: Canvas, proj: Projectile) {
        val (sx, sy) = worldToScreen(proj.position)
        paint.style = Paint.Style.FILL

        when (proj.type) {
            ProjectileType.KNIFE -> {
                // Throwing knife
                paint.color = Color.parseColor("#CCCCCC")
                canvas.save()
                canvas.rotate(proj.angle, sx, sy)
                canvas.drawRect(sx - 8f, sy - 2f, sx + 8f, sy + 2f, paint)
                paint.color = Color.parseColor("#886633")
                canvas.drawRect(sx + 6f, sy - 3f, sx + 10f, sy + 3f, paint)
                canvas.restore()
                // Trail
                paint.color = Color.argb(80, 200, 200, 255)
                canvas.drawOval(sx - 10f, sy - 4f, sx + 10f, sy + 4f, paint)
            }
            ProjectileType.MAGIC_BOLT -> {
                paint.color = Color.parseColor("#9944FF")
                canvas.drawCircle(sx, sy, 5f, paint)
                paint.color = Color.parseColor("#CC88FF")
                canvas.drawCircle(sx, sy, 3f, paint)
            }
            ProjectileType.FIREBALL -> {
                paint.color = Color.parseColor("#FF4400")
                canvas.drawCircle(sx, sy, 7f, paint)
                paint.color = Color.parseColor("#FFAA00")
                canvas.drawCircle(sx, sy, 4f, paint)
                paint.color = Color.parseColor("#FFFF44")
                canvas.drawCircle(sx, sy, 2f, paint)
            }
            ProjectileType.SPEAR -> {
                paint.color = Color.parseColor("#886644")
                canvas.save()
                canvas.rotate(proj.angle, sx, sy)
                canvas.drawRect(sx - 12f, sy - 1.5f, sx + 12f, sy + 1.5f, paint)
                paint.color = Color.parseColor("#AAAAAA")
                canvas.drawRect(sx + 10f, sy - 3f, sx + 16f, sy + 3f, paint)
                canvas.restore()
            }
            ProjectileType.ZEUS_BOLT -> {
                paint.color = Color.parseColor("#44AAFF")
                canvas.drawCircle(sx, sy, 10f, paint)
                paint.color = Color.parseColor("#FFFFFF")
                canvas.drawCircle(sx, sy, 5f, paint)
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
            quadTo(sx, sy - 40f, sx + hw / 2, sy - 30f)
            lineTo(sx + hw / 2, sy)
            close()
        }
        canvas.drawPath(path, paint)

        if (!door.isLocked) {
            // Glow effect
            paint.color = Color.argb(60, 255, 215, 0)
            canvas.drawCircle(sx, sy - 15f, 20f, paint)
        }
    }

    fun renderMerchant(canvas: Canvas, merchant: Merchant) {
        val (sx, sy) = worldToScreen(merchant.position)

        paint.style = Paint.Style.FILL
        // Body
        paint.color = Color.parseColor("#885522")
        canvas.drawRect(sx - 8f, sy - 30f, sx + 8f, sy - 4f, paint)
        // Head
        paint.color = Color.parseColor("#FFCC99")
        canvas.drawCircle(sx, sy - 36f, 7f, paint)
        // Hat
        paint.color = Color.parseColor("#663300")
        canvas.drawRect(sx - 10f, sy - 46f, sx + 10f, sy - 40f, paint)
        canvas.drawRect(sx - 6f, sy - 52f, sx + 6f, sy - 46f, paint)
        // Backpack
        paint.color = Color.parseColor("#557733")
        canvas.drawRect(sx - 14f, sy - 28f, sx - 8f, sy - 10f, paint)
        // Legs
        paint.color = Color.parseColor("#664411")
        canvas.drawRect(sx - 6f, sy - 4f, sx - 1f, sy + 6f, paint)
        canvas.drawRect(sx + 1f, sy - 4f, sx + 6f, sy + 6f, paint)

        // "Talk" indicator
        paint.color = Color.parseColor("#FFD700")
        textPaint.color = Color.parseColor("#FFD700")
        textPaint.textSize = 20f
        val bounce = sin(System.nanoTime() / 200_000_000f) * 3f
        canvas.drawText("!", sx - 3f, sy - 55f + bounce, textPaint)
    }

    fun renderMenu(canvas: Canvas, w: Int, h: Int) {
        // Background
        paint.color = Color.parseColor("#0A0515")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Decorative pillars
        paint.color = Color.parseColor("#1A0F2D")
        canvas.drawRect(w * 0.05f, h * 0.1f, w * 0.08f, h * 0.9f, paint)
        canvas.drawRect(w * 0.92f, h * 0.1f, w * 0.95f, h * 0.9f, paint)

        // Title
        canvas.drawText("哈迪斯", w / 2f, h * 0.35f, titlePaint)

        // Subtitle
        canvas.drawText("地狱逃脱", w / 2f, h * 0.45f, subtitlePaint)

        // Tap to start
        val alpha = ((sin(System.nanoTime() / 500_000_000f) + 1) * 127).toInt()
        subtitlePaint.color = Color.argb(alpha, 200, 180, 150)
        canvas.drawText("点击开始", w / 2f, h * 0.65f, subtitlePaint)
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.argb(200, 20, 0, 0)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        titlePaint.color = Color.parseColor("#FF2222")
        canvas.drawText("游戏结束", w / 2f, h * 0.4f, titlePaint)

        subtitlePaint.color = Color.parseColor("#AA8888")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, subtitlePaint)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        paint.color = Color.argb(200, 0, 20, 0)
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
