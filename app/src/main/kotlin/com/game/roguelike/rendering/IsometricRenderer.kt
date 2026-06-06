package com.game.roguelike.rendering

import android.graphics.*
import com.game.roguelike.combat.Projectile
import com.game.roguelike.entity.*
import com.game.roguelike.level.Door
import com.game.roguelike.level.Room
import com.game.roguelike.util.Vector2

import android.content.Context

class IsometricRenderer(val context: Context) {

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
        textSize = 120f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    internal val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCAA88")
        textSize = 54f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    // Global animation time for ambient effects
    internal var globalTime = 0f

    // Shader/gradient toggle for low-end devices
    internal var enableShaders = true

    // Path object pool to reduce GC pressure
    private val pathPool = ArrayDeque<Path>(32)

    internal fun obtainPath(): Path {
        return if (pathPool.isNotEmpty()) {
            val p = pathPool.removeFirst()
            p.reset()
            p
        } else {
            Path()
        }
    }

    internal fun recyclePath(p: Path) {
        if (pathPool.size < 64) pathPool.addLast(p)
    }

    // Reset paint to safe defaults (clear shader, restore fill style)
    internal fun resetPaint() {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f
        paint.strokeCap = Paint.Cap.BUTT
    }

    // Light sources for ambient glow effects
    data class LightSource(val x: Float, val y: Float, val radius: Float, val color: Int, val intensity: Float)
    internal val lightSources = mutableListOf<LightSource>()

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
    private val environmentRenderer = EnvironmentRenderer(this)
    private val bossEntranceCinematicRenderer = BossEntranceCinematicRenderer(this)
    private val tileRenderer = TileRenderer(this)
    private val playerRenderer = PlayerRenderer(this, context)
    private val enemyRenderer = EnemyRenderer(this)
    private val entityRenderer = EntityRenderer(this)
    internal val screenRenderer = ScreenRenderer(this, context)

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

    // --- Delegated render calls ---

    fun renderRoom(canvas: Canvas, room: Room, playerPos: Vector2, dt: Float) {
        tileRenderer.renderRoom(canvas, room, playerPos, dt)
    }

    fun renderEnvironment(canvas: Canvas, room: Room) {
        environmentRenderer.render(canvas, room)
    }

    fun renderBossEntranceCinematic(canvas: Canvas, room: Room, layerIndex: Int, timer: Float) {
        bossEntranceCinematicRenderer.render(canvas, room, layerIndex, timer)
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

    fun renderBossWarning(canvas: Canvas, warning: BossWarning) {
        entityRenderer.renderBossWarning(canvas, warning)
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

    fun renderIntroStory(canvas: Canvas, w: Int, h: Int) {
        screenRenderer.renderIntroStory(canvas, w, h)
    }

    fun renderEndingStory(canvas: Canvas, w: Int, h: Int) {
        screenRenderer.renderEndingStory(canvas, w, h)
    }

    fun renderMultiplayerLobby(canvas: Canvas, w: Int, h: Int) {
        screenRenderer.renderMultiplayerLobby(canvas, w, h)
    }

    fun renderRoomList(canvas: Canvas, w: Int, h: Int, rooms: List<com.game.roguelike.network.RoomInfo>) {
        screenRenderer.renderRoomList(canvas, w, h, rooms)
    }

    fun renderRoomWaiting(canvas: Canvas, w: Int, h: Int, roomManager: com.game.roguelike.network.RoomManager) {
        screenRenderer.renderRoomWaiting(canvas, w, h, roomManager)
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
        val r = kotlin.math.min(255, (Color.red(color) + (255 - Color.red(color)) * (factor - 1f)).toInt())
        val g = kotlin.math.min(255, (Color.green(color) + (255 - Color.green(color)) * (factor - 1f)).toInt())
        val b = kotlin.math.min(255, (Color.blue(color) + (255 - Color.blue(color)) * (factor - 1f)).toInt())
        return Color.rgb(r, g, b)
    }

    // Create gradient helpers (with safety checks to prevent crashes)
    internal fun makeLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float, c0: Int, c1: Int): LinearGradient {
        // LinearGradient throws if both points are the same — nudge slightly
        if (x0 == x1 && y0 == y1) return LinearGradient(x0, y0, x0 + 0.01f, y0, c0, c1, Shader.TileMode.CLAMP)
        return LinearGradient(x0, y0, x1, y1, c0, c1, Shader.TileMode.CLAMP)
    }

    internal fun makeRadialGradient(cx: Float, cy: Float, radius: Float, centerColor: Int, edgeColor: Int): RadialGradient {
        // RadialGradient throws if radius <= 0 — clamp to minimum
        val safeRadius = if (radius > 0.01f) radius else 0.01f
        return RadialGradient(cx, cy, safeRadius, centerColor, edgeColor, Shader.TileMode.CLAMP)
    }

    data class LayerTheme(
        val floor: Int,
        val floorAlt: Int,
        val wall: Int,
        val wallTop: Int,
        val accent: Int
    )
}
