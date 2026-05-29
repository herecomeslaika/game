package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.combat.Projectile
import com.game.roguelike.entity.*
import com.game.roguelike.level.Door
import com.game.roguelike.level.Room
import com.game.roguelike.util.Vector2

class IsometricRenderer {

    var screenWidth = 1920
    var screenHeight = 1080
    var tileWidth = 64
    var tileHeight = 32

    var cameraX = 0f
    var cameraY = 0f

    internal val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 0, 0, 0)
    }
    internal val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    internal val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 80f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    internal val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCAA88")
        textSize = 36f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    // Global animation time for ambient effects
    internal var globalTime = 0f

    // Layer color themes
    internal val layerColors = arrayOf(
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

    // Sub-renderers
    private val tileRenderer = TileRenderer(this)
    private val playerRenderer = PlayerRenderer(this)
    private val enemyRenderer = EnemyRenderer(this)
    private val entityRenderer = EntityRenderer(this)
    private val screenRenderer = ScreenRenderer(this)

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

    fun screenToGrid(screenX: Float, screenY: Float): Pair<Float, Float> {
        val adjX = screenX + cameraX - screenWidth / 2f
        val adjY = screenY + cameraY - screenHeight / 2f
        val gridX = adjX / (tileWidth / 2f) + adjY / (tileHeight / 2f)
        val gridY = -adjX / (tileWidth / 2f) + adjY / (tileHeight / 2f)
        return Pair(gridX, gridY)
    }

    // --- Delegated render calls ---

    fun renderRoom(canvas: Canvas, room: Room, playerPos: Vector2, dt: Float) {
        tileRenderer.renderRoom(canvas, room, playerPos, dt)
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
        playerRenderer.renderPlayer(canvas, player)
    }

    fun renderEnemy(canvas: Canvas, enemy: Enemy) {
        enemyRenderer.renderEnemy(canvas, enemy)
    }

    fun renderProjectile(canvas: Canvas, proj: Projectile) {
        entityRenderer.renderProjectile(canvas, proj)
    }

    fun renderParticle(canvas: Canvas, particle: Particle) {
        entityRenderer.renderParticle(canvas, particle)
    }

    fun renderDoor(canvas: Canvas, door: Door, room: Room) {
        entityRenderer.renderDoor(canvas, door, room)
    }

    fun renderMerchant(canvas: Canvas, merchant: Merchant, isNearPlayer: Boolean = false) {
        entityRenderer.renderMerchant(canvas, merchant, isNearPlayer)
    }

    fun renderMenu(canvas: Canvas, w: Int, h: Int) {
        screenRenderer.renderMenu(canvas, w, h)
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        screenRenderer.renderGameOver(canvas, w, h)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        screenRenderer.renderVictory(canvas, w, h)
    }

    fun drawFade(canvas: Canvas, alpha: Float) {
        screenRenderer.drawFade(canvas, alpha)
    }

    fun renderBossEntrance(canvas: Canvas, bossName: String, bossTitle: String, timer: Float, phase: Int, w: Int, h: Int) {
        screenRenderer.renderBossEntrance(canvas, bossName, bossTitle, timer, phase, w, h)
    }

    // --- Color utility methods (used by sub-renderers) ---

    internal fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    internal fun lighten(color: Int, factor: Float): Int {
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
