package com.game.roguelike.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SurfaceHolder
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.blessing.BlessingSelector
import com.game.roguelike.core.GameState
import com.game.roguelike.core.GodType
import com.game.roguelike.combat.Projectile
import com.game.roguelike.entity.*
import com.game.roguelike.level.*
import com.game.roguelike.network.NetworkFrame
import com.game.roguelike.network.RoomManager
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.shop.Shop
import com.game.roguelike.ui.*
import com.game.roguelike.util.Vector2
import kotlin.math.min

class Game(private val context: Context) {

    private var holder: SurfaceHolder? = null
    private var running = false
    private var gameThread: Thread? = null

    var screenWidth = 1920
    var screenHeight = 1080
    var gameState = GameState.MENU
        private set

    val renderer = IsometricRenderer(context)
    val player = Player()
    val enemies = mutableListOf<Enemy>()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<Particle>()
    val ghosts = mutableListOf<GhostSummon>()

    var currentLayer: Layer? = null
    var currentRoom: Room? = null
    var currentLayerIndex = 0

    var gold = 0
    val blessings = mutableListOf<Blessing>()
    val blessingSelector = BlessingSelector()

    // 联机相关
    var isMultiplayer = false
    var isHost = false
    var localPlayerId = 1
    val otherPlayer = Player() // 双人模式第二个玩家对象
    var roomManager: RoomManager? = null

    val shop = Shop()
    var merchant: Merchant? = null

    val hud = HUD()
    val virtualJoystick = VirtualJoystick()
    val actionButtons = ActionButtons()
    val blessingSelectUI = BlessingSelectUI()
    val shopUI = ShopUI()

    var shakeAmount = 0f
    var shakeDuration = 0f
    var transitionAlpha = 0f
    var transitionTarget: GameState? = null
    var spikeDamageTimer = 0f
    var frostFieldTimer = 0f

    // Boss entrance animation
    var bossEntranceTimer = 0f
    var bossEntrancePhase = 0
    var bossEntranceName = ""
    var bossEntranceTitle = ""
    var pendingBossType: EnemyType? = null
    var timeScale = 1f

    // Input manager reference - set by GameSurfaceView
    var inputManager: InputManager? = null
    var vibrator: Vibrator? = null
    internal val screenRenderer = renderer.screenRenderer

    private val TICK = 1f / 60f
    private var accumulator = 0f
    private var lastTime = 0L

    fun start(holder: SurfaceHolder) {
        this.holder = holder
        running = true
        lastTime = System.nanoTime()
        gameThread = Thread { gameLoop() }
        gameThread?.start()
    }

    fun stop() {
        running = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            // ignore
        }
    }

    fun onScreenResize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        renderer.updateScreenSize(width, height)
        virtualJoystick.updateLayout(width, height)
        inputManager?.let { actionButtons.updateLayout(width, height, it) }
        hud.updateLayout(width, height)
        blessingSelectUI.updateLayout(width, height)
        shopUI.updateLayout(width, height)
    }

    private fun gameLoop() {
        while (running) {
            val now = System.nanoTime()
            val dt = min((now - lastTime) / 1_000_000_000f, 0.1f)
            lastTime = now
            accumulator += dt

            while (accumulator >= TICK) {
                update(TICK)
                accumulator -= TICK
            }

            render()
        }
    }

    fun update(dt: Float) {
        val scaledDt = dt * timeScale
        when (gameState) {
            GameState.MENU -> updateMenu(dt)
            GameState.PLAYING -> updatePlaying(scaledDt)
            GameState.BOSS_ENTRANCE -> updateBossEntrance(dt)
            GameState.BLESSING_SELECT -> updateBlessingSelect(dt)
            GameState.SHOP -> updateShop(dt)
            GameState.LAYER_TRANSITION -> updateTransition(dt)
            GameState.MULTIPLAYER_LOBBY, GameState.ROOM_LIST, GameState.ROOM_WAITING -> {}
            GameState.GAME_OVER, GameState.VICTORY -> updateEndScreen(dt)
            else -> {}
        }
        updateShake(dt)
        updateParticles(dt)
    }

    private fun updateMenu(dt: Float) {
        // Waiting for tap input via handleTouch
    }

    private fun updatePlaying(dt: Float) {
        val room = currentRoom ?: return
        val input = inputManager ?: return

        // Update virtual joystick visual
        virtualJoystick.updateKnob(input.joystickDirection)

        // Process attack input
        if (input.consumeAttack()) {
            // Check merchant interaction first
            val nearMerchant = merchant?.let { m -> !m.talked && m.isNearPlayer } == true
            if (nearMerchant) {
                merchant!!.talked = true
                shop.open(gold)
                gameState = GameState.SHOP
            } else {
                when (player.stateMachine.currentState) {
                    PlayerState.IDLE, PlayerState.RUN -> {
                        player.startAttack1(this)
                    }
                    PlayerState.ATTACK1, PlayerState.ATTACK2 -> {
                        player.tryComboAttack(this)
                    }
                    else -> {}
                }
            }
        }

        // Special attack
        if (input.consumeSpecial()) {
            when (player.stateMachine.currentState) {
                PlayerState.IDLE, PlayerState.RUN -> player.startSpecial(this)
                else -> {}
            }
        }

        // Dash
        if (input.consumeDash()) {
            when (player.stateMachine.currentState) {
                PlayerState.IDLE, PlayerState.RUN -> player.startDash(this)
                else -> {}
            }
        }

        // Player update
        player.update(dt, this)

        // Merchant update
        merchant?.update(dt, this)

        // Enemy updates - copy list to avoid ConcurrentModificationException
        // (enemy update can add new enemies via summoning)
        val enemiesToUpdate = enemies.toList()
        for (enemy in enemiesToUpdate) {
            enemy.update(dt, this)
        }
        // Remove dead enemies after all updates
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            if (enemy.isDead && enemy.deathAnimationDone) {
                gold += enemy.goldDrop
                // Hades summon: spawn ghost on kill
                if (player.hasSummon && ghosts.size < 3) {
                    ghosts.add(GhostSummon(enemy.position))
                    for (i in 0..5) {
                        particles.add(Particle(
                            position = Vector2(enemy.position.x, enemy.position.y),
                            velocity = Vector2((kotlin.random.Random.nextFloat() - 0.5f) * 80f, -50f),
                            color = android.graphics.Color.parseColor("#AA44FF"),
                            life = 0.5f,
                            size = 4f
                        ))
                    }
                }
                enemyIter.remove()
            }
        }

        // Projectile updates
        val projIter = projectiles.iterator()
        while (projIter.hasNext()) {
            val proj = projIter.next()
            proj.update(dt, this)
            if (proj.shouldRemove) {
                projIter.remove()
            }
        }

        // Check door collision (to go to next room)
        for (door in room.doors) {
            if (!door.isLocked && player.position.distanceTo(door.position) < 40f) {
                goToNextRoom()
                break
            }
        }

        // Check room clear
        if ((room.type == RoomType.COMBAT || room.type == RoomType.ELITE || room.type == RoomType.BOSS) && !room.cleared) {
            if (enemies.isEmpty()) {
                room.cleared = true
                room.unlockDoors()
                // Boss room grants blessing selection
                if (room.type == RoomType.BOSS) {
                    gameState = GameState.BLESSING_SELECT
                    blessingSelector.generateOffering(currentLayerIndex, blessings)
                }
            }
        }

        // Spike damage — check if player is standing on a spike tile
        checkSpikeDamage(room, dt)

        // Frost field (Demeter RARE: freeze nearest enemy periodically)
        if (player.supportFreeze) {
            frostFieldTimer += dt
            if (frostFieldTimer >= player.supportCooldown) {
                frostFieldTimer = 0f
                val nearest = enemies.filter { !it.isDead }.minByOrNull { it.position.distanceTo(player.position) }
                nearest?.freezeTimer = player.freezeDuration
            }
        }

        // Hades summon — update ghosts
        val ghostIter = ghosts.iterator()
        while (ghostIter.hasNext()) {
            val ghost = ghostIter.next()
            ghost.update(dt, this)
            if (ghost.isDead) {
                ghostIter.remove()
            }
        }

        // Fire trail particles damage player
        // Use index-based iteration to avoid ConcurrentModificationException
        // (takeDamage adds particles to the same list)
        for (i in particles.indices) {
            val particle = particles[i]
            if (particle.damage > 0 && particle.isFireTrail && !player.isDashInvincible) {
                if (player.position.distanceTo(particle.position) < 25f) {
                    player.takeDamage(particle.damage.toInt(), this)
                }
            }
        }

        // Check player death
        if (player.isDead) {
            gameState = GameState.GAME_OVER
        }
    }

    private fun updateBlessingSelect(dt: Float) {
        blessingSelectUI.update(dt)
    }

    private fun updateShop(dt: Float) {
        shopUI.update(dt)
    }

    private fun updateBossEntrance(dt: Float) {
        bossEntranceTimer += dt

        when (bossEntrancePhase) {
            0 -> {
                // Phase 0 (0-1.5s): slow-motion + screen shake + short vibration
                shake(6f, 1.5f)
                if (bossEntranceTimer < 0.05f) {
                    vibrate(100)
                }
                if (bossEntranceTimer >= 1.5f) {
                    bossEntrancePhase = 1
                    bossEntranceTimer = 0f
                    timeScale = 1f
                    shake(10f, 2f)
                    vibrate(300)
                }
            }
            1 -> {
                // Phase 1 (1.5-3.5s): boss name display + heavy shake
                if (bossEntranceTimer >= 2f) {
                    bossEntrancePhase = 2
                    bossEntranceTimer = 0f
                    vibrate(50)
                }
            }
            2 -> {
                // Phase 2 (3.5-4.0s): fade out, restore, spawn boss
                if (bossEntranceTimer >= 0.5f) {
                    val room = currentRoom ?: return
                    room.spawnBoss(this)
                    pendingBossType = null
                    timeScale = 1f
                    gameState = GameState.PLAYING
                }
            }
        }
    }

    private fun updateTransition(dt: Float) {
        if (transitionTarget == null) {
            transitionAlpha += dt * 2f
            if (transitionAlpha >= 1f) {
                transitionAlpha = 1f
                currentLayerIndex++
                if (currentLayerIndex >= 3) {
                    gameState = GameState.VICTORY
                    return
                }
                currentLayer = Layer(currentLayerIndex)
                currentRoom = currentLayer!!.getCurrentRoom()
                loadRoom(currentRoom!!)
                transitionTarget = GameState.PLAYING
            }
        } else if (transitionTarget == GameState.PLAYING) {
            transitionAlpha -= dt * 2f
            if (transitionAlpha <= 0f) {
                transitionAlpha = 0f
                gameState = GameState.PLAYING
            }
        }
    }

    private fun updateEndScreen(dt: Float) {
        // Waiting for tap
    }

    private fun updateShake(dt: Float) {
        if (shakeDuration > 0) {
            shakeDuration -= dt
            if (shakeDuration <= 0) {
                shakeAmount = 0f
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.update(dt)
            if (p.isDead) iter.remove()
        }
    }

    fun shake(amount: Float, duration: Float) {
        shakeAmount = amount
        shakeDuration = duration
    }

    private fun vibrate(durationMs: Long) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: SecurityException) {
            // Missing VIBRATE permission — skip silently
        }
    }

    private fun checkSpikeDamage(room: Room, dt: Float) {
        val tw = 64f
        val th = 32f
        val gridX = (player.position.x / tw).toInt().coerceIn(0, room.width - 1)
        val gridY = (player.position.y / th).toInt().coerceIn(0, room.height - 1)
        val tile = room.getTile(gridX, gridY)

        if (tile == Room.TILE_SPIKE && !player.isDashInvincible) {
            spikeDamageTimer += dt
            if (spikeDamageTimer >= 1f) {
                spikeDamageTimer = 0f
                player.takeDamage(5, this)
            }
        } else {
            spikeDamageTimer = 0f
        }
    }

    fun loadRoom(room: Room) {
        currentRoom = room
        enemies.clear()
        projectiles.clear()
        ghosts.clear()
        merchant = null

        player.position.set(room.spawnPoint)
        player.velocity.set(Vector2.ZERO)

        when (room.type) {
            RoomType.ENTRY -> {}
            RoomType.COMBAT -> room.spawnEnemies(this)
            RoomType.ELITE -> room.spawnEnemies(this) // elite uses combat spawn but harder
            RoomType.REWARD -> {
                gameState = GameState.BLESSING_SELECT
                blessingSelector.generateOffering(currentLayerIndex, blessings)
            }
            RoomType.TREASURE -> {
                gameState = GameState.BLESSING_SELECT
                blessingSelector.generateOffering(currentLayerIndex, blessings)
            }
            RoomType.SHOP -> merchant = Merchant(room.merchantPosition)
            RoomType.BOSS -> {
                val bossType = when (currentLayerIndex) {
                    0 -> EnemyType.MEGA_SKELETON
                    1 -> EnemyType.INFERNO_TITAN
                    2 -> EnemyType.CHAMPION
                    else -> EnemyType.MEGA_SKELETON
                }
                pendingBossType = bossType
                bossEntranceName = bossType.bossName
                bossEntranceTitle = bossType.bossTitle
                bossEntranceTimer = 0f
                bossEntrancePhase = 0
                timeScale = 0.2f
                gameState = GameState.BOSS_ENTRANCE
            }
            RoomType.EVENT -> {
                // Random event: heal or blessing
                if (kotlin.random.Random.nextFloat() < 0.5f) {
                    player.health = (player.health + (player.maxHealth * 0.2f).toInt()).coerceAtMost(player.maxHealth)
                } else {
                    gameState = GameState.BLESSING_SELECT
                    blessingSelector.generateOffering(currentLayerIndex, blessings)
                }
            }
            RoomType.REST -> {
                player.health = (player.health + (player.maxHealth * 0.3f).toInt()).coerceAtMost(player.maxHealth)
            }
            RoomType.HIDDEN -> {
                gameState = GameState.BLESSING_SELECT
                blessingSelector.generateOffering(currentLayerIndex, blessings)
            }
        }
    }

    fun goToNextRoom() {
        val layer = currentLayer ?: return
        val connected = layer.getConnectedRooms()

        if (connected.isEmpty()) {
            // No more rooms — boss cleared, transition to next layer
            if (layer.isBossRoom() && currentRoom?.cleared == true) {
                gameState = GameState.LAYER_TRANSITION
                transitionAlpha = 0f
                transitionTarget = null
            }
            return
        }

        // If only one connection, go directly
        if (connected.size == 1) {
            val nextRoom = layer.goToRoom(connected[0].id)
            loadRoom(nextRoom)
            return
        }

        // Multiple paths — auto-pick the first available connection
        // (Future: add room selection UI with ROOM_SELECT state)
        val nextRoom = layer.goToRoom(connected[0].id)
        loadRoom(nextRoom)
    }

    fun goToRoomById(roomId: Int) {
        val layer = currentLayer ?: return
        val nextRoom = layer.goToRoom(roomId)
        loadRoom(nextRoom)
    }

    fun startNewRun() {
        gold = 0
        blessings.clear()
        ghosts.clear()
        player.reset()
        currentLayerIndex = 0
        currentLayer = Layer(currentLayerIndex)
        currentRoom = currentLayer!!.getCurrentRoom()
        loadRoom(currentRoom!!)
        timeScale = 1f
        bossEntranceTimer = 0f
        bossEntrancePhase = 0
        pendingBossType = null
        gameState = GameState.PLAYING
    }

    fun selectBlessing(blessing: Blessing) {
        blessings.add(blessing)
        player.ownedGods.add(blessing.god)
        applyBlessingEffect(blessing, player)
        gameState = GameState.PLAYING
    }

    private fun applyBlessingEffect(blessing: Blessing, player: Player) {
        blessing.onApply?.invoke(player)
    }

    private fun render() {
        val holder = this.holder ?: return
        var canvas: android.graphics.Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                synchronized(this) {
                    renderFrame(canvas)
                }
            }
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun renderFrame(canvas: android.graphics.Canvas) {
        canvas.drawColor(android.graphics.Color.BLACK)

        when (gameState) {
            GameState.MENU -> renderMenu(canvas)
            GameState.PLAYING -> renderPlaying(canvas)
            GameState.BOSS_ENTRANCE -> {
                renderPlaying(canvas)
                renderer.renderBossEntrance(canvas, bossEntranceName, bossEntranceTitle, bossEntranceTimer, bossEntrancePhase, screenWidth, screenHeight)
            }
            GameState.BLESSING_SELECT -> {
                renderPlaying(canvas)
                blessingSelectUI.render(canvas, blessingSelector.currentOffering)
            }
            GameState.SHOP -> {
                renderPlaying(canvas)
                shopUI.render(canvas, shop)
            }
            GameState.LAYER_TRANSITION -> {
                renderPlaying(canvas)
                renderer.drawFade(canvas, transitionAlpha)
            }
            GameState.MULTIPLAYER_LOBBY -> screenRenderer.renderMultiplayerLobby(canvas, screenWidth, screenHeight)
            GameState.ROOM_LIST -> screenRenderer.renderRoomList(canvas, screenWidth, screenHeight, roomManager?.discoveredRooms ?: emptyList())
            GameState.ROOM_WAITING -> {
                val rm = roomManager
                if (rm != null) {
                    screenRenderer.renderRoomWaiting(canvas, screenWidth, screenHeight, rm)
                }
            }
            GameState.GAME_OVER -> renderGameOver(canvas)
            GameState.VICTORY -> renderVictory(canvas)
            else -> {}
        }
    }

    private fun renderMenu(canvas: android.graphics.Canvas) {
        renderer.renderMenu(canvas, screenWidth, screenHeight)
    }

    private fun renderPlaying(canvas: android.graphics.Canvas) {
        val room = currentRoom ?: return

        canvas.save()
        // Screen shake
        if (shakeAmount > 0) {
            val sx = (Math.random() - 0.5) * 2 * shakeAmount
            val sy = (Math.random() - 0.5) * 2 * shakeAmount
            canvas.translate(sx.toFloat(), sy.toFloat())
        }

        // Render room
        renderer.renderRoom(canvas, room, player.position, TICK)

        // Depth-sort entities
        val entities = mutableListOf<Entity>()
        entities.add(player)
        entities.addAll(enemies.filter { !it.deathAnimationDone })
        merchant?.let { entities.add(it) }
        entities.sortBy { it.position.y }

        // Shadows
        for (entity in entities) {
            renderer.renderShadow(canvas, entity)
        }

        // Entities
        for (entity in entities) {
            entity.render(canvas, renderer)
        }

        // Projectiles
        for (proj in projectiles) {
            renderer.renderProjectile(canvas, proj)
        }

        // Ghost summons
        for (ghost in ghosts) {
            ghost.render(canvas, renderer)
        }

        // Particles
        for (particle in particles) {
            renderer.renderParticle(canvas, particle)
        }

        // Doors
        for (door in room.doors) {
            renderer.renderDoor(canvas, door, room)
        }

        canvas.restore()

        // UI (not affected by shake)
        virtualJoystick.render(canvas)
        actionButtons.render(canvas, this)
        hud.render(canvas, player, gold, blessings, currentLayerIndex)
    }

    private fun renderGameOver(canvas: android.graphics.Canvas) {
        renderer.renderGameOver(canvas, screenWidth, screenHeight)
    }

    private fun renderVictory(canvas: android.graphics.Canvas) {
        renderer.renderVictory(canvas, screenWidth, screenHeight)
    }

    fun handleTouch(x: Float, y: Float) {
        when (gameState) {
            GameState.PLAYING -> {
                // 检测右上角返回按钮点击
                if (hud.handleBackButtonClick(x, y)) {
                    // 弹出确认提示，暂时直接返回主菜单
                    gameState = GameState.MENU
                    inputManager?.reset()
                    return
                }
            }
            GameState.MENU -> {
                // 检查点击开始游戏按钮
                if (screenRenderer.startBtnRect.contains(x, y)) {
                    startNewRun()
                }
                // 检查点击联机模式按钮
                else if (screenRenderer.multiplayerBtnRect.contains(x, y)) {
                    // 进入联机大厅
                    gameState = GameState.MULTIPLAYER_LOBBY
                    roomManager = RoomManager(context)
                    roomManager?.onGameStart = {
                        startNewRun()
                    }
                    roomManager?.onKicked = {
                        roomManager?.stop()
                        roomManager = null
                        isMultiplayer = false
                        isHost = false
                        gameState = GameState.MULTIPLAYER_LOBBY
                    }
                    roomManager?.onLobbyUpdated = {
                        // 大厅玩家列表已更新，renderRoomWaiting 会自动读取最新数据
                    }
                }
                // 检查点击退出游戏按钮
                else if (screenRenderer.exitBtnRect.contains(x, y)) {
                    // 提示退出游戏，直接关闭Activity
                    val activity = context as android.app.Activity
                    activity.finishAndRemoveTask()
                }
            }
            GameState.BLESSING_SELECT -> {
                if (blessingSelector.currentOffering.isEmpty()) {
                    gameState = GameState.PLAYING
                } else {
                    val selected = blessingSelectUI.handleTouch(x, y, blessingSelector.currentOffering)
                    if (selected != null) {
                        selectBlessing(selected)
                    }
                }
            }
            GameState.SHOP -> {
                val purchased = shopUI.handleTouch(x, y, shop)
                if (purchased != null) {
                    val cost = purchased.cost
                    if (gold >= cost) {
                        gold -= cost
                        purchased.applyEffect(player, this)
                    }
                } else {
                    // Tap outside items to close
                    shop.close()
                    gameState = GameState.PLAYING
                }
            }
            GameState.MULTIPLAYER_LOBBY -> {
                if (screenRenderer.createRoomBtnRect.contains(x, y)) {
                    // 创建房间 → 进入等待状态
                    roomManager?.createRoom("房间")
                    isHost = true
                    isMultiplayer = true
                    gameState = GameState.ROOM_WAITING
                } else if (screenRenderer.joinRoomBtnRect.contains(x, y)) {
                    // 加入房间 — 扫描并进入房间列表
                    gameState = GameState.ROOM_LIST
                    roomManager?.scanRooms()
                } else if (screenRenderer.backToMenuBtnRect.contains(x, y)) {
                    // 返回主菜单
                    roomManager?.stop()
                    roomManager = null
                    gameState = GameState.MENU
                }
            }
            GameState.ROOM_LIST -> {
                // 检查点击房间列表中的某个房间
                val rooms = roomManager?.discoveredRooms ?: emptyList()
                for (i in screenRenderer.roomListRects.indices) {
                    if (i < rooms.size && screenRenderer.roomListRects[i].contains(x, y)) {
                        roomManager?.joinRoom(rooms[i])
                        isMultiplayer = true
                        gameState = GameState.ROOM_WAITING
                        break
                    }
                }
                // 返回按钮
                if (screenRenderer.backToMenuBtnRect.contains(x, y)) {
                    gameState = GameState.MULTIPLAYER_LOBBY
                }
            }
            GameState.ROOM_WAITING -> {
                val rm = roomManager ?: return
                if (rm.isHost) {
                    // 房主：开始游戏
                    if (screenRenderer.startGameBtnRect.contains(x, y)) {
                        rm.startGame()
                    }
                    // 房主：踢人
                    val players = rm.lobbyPlayers
                    for (i in screenRenderer.kickPlayerBtnRects.indices) {
                        if (screenRenderer.kickPlayerBtnRects[i].contains(x, y)) {
                            // 找到非房主玩家对应的索引
                            val nonHostPlayers = players.filter { !it.isHost }
                            if (i < nonHostPlayers.size) {
                                rm.kickPlayer(nonHostPlayers[i].playerId)
                            }
                            break
                        }
                    }
                } else {
                    // 客户端：准备
                    if (screenRenderer.readyBtnRect.contains(x, y)) {
                        rm.setReady()
                    }
                }
                // 离开房间
                if (screenRenderer.leaveRoomBtnRect.contains(x, y)) {
                    rm.stop()
                    roomManager = null
                    isMultiplayer = false
                    isHost = false
                    gameState = GameState.MULTIPLAYER_LOBBY
                }
            }
            GameState.GAME_OVER, GameState.VICTORY -> {
                gameState = GameState.MENU
                inputManager?.reset()
            }
            else -> {}
        }
    }
}
