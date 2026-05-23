package com.game.roguelike.core

import android.content.Context
import android.view.SurfaceHolder
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.blessing.BlessingSelector
import com.game.roguelike.combat.Projectile
import com.game.roguelike.entity.*
import com.game.roguelike.level.*
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

    val renderer = IsometricRenderer()
    val player = Player()
    val enemies = mutableListOf<Enemy>()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<Particle>()

    var currentLayer: Layer? = null
    var currentRoom: Room? = null
    var currentLayerIndex = 0
    var currentRoomIndex = 0

    var gold = 0
    val blessings = mutableListOf<Blessing>()
    val blessingSelector = BlessingSelector()

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

    // Input manager reference - set by GameSurfaceView
    var inputManager: InputManager? = null

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
        when (gameState) {
            GameState.MENU -> updateMenu(dt)
            GameState.PLAYING -> updatePlaying(dt)
            GameState.BLESSING_SELECT -> updateBlessingSelect(dt)
            GameState.SHOP -> updateShop(dt)
            GameState.LAYER_TRANSITION -> updateTransition(dt)
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
            val nearMerchant = merchant?.let { m -> !m.talked && m.isNearPlayer(player) } == true
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
        if ((room.type == RoomType.COMBAT || room.type == RoomType.BOSS) && !room.cleared) {
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
                currentLayer = Layer.create(currentLayerIndex)
                currentRoomIndex = 0
                currentRoom = currentLayer!!.rooms[0]
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

    fun loadRoom(room: Room) {
        currentRoom = room
        enemies.clear()
        projectiles.clear()
        merchant = null

        player.position.set(room.spawnPoint)
        player.velocity.set(Vector2.ZERO)

        when (room.type) {
            RoomType.ENTRY -> {
                // No enemies
            }
            RoomType.COMBAT -> {
                room.spawnEnemies(this)
            }
            RoomType.REWARD -> {
                gameState = GameState.BLESSING_SELECT
                blessingSelector.generateOffering(currentLayerIndex, blessings)
            }
            RoomType.SHOP -> {
                merchant = Merchant(room.merchantPosition)
            }
            RoomType.BOSS -> {
                room.spawnBoss(this)
            }
        }
    }

    fun goToNextRoom() {
        val layer = currentLayer ?: return
        currentRoomIndex++
        if (currentRoomIndex >= layer.rooms.size) {
            gameState = GameState.LAYER_TRANSITION
            transitionAlpha = 0f
            transitionTarget = null
            return
        }
        loadRoom(layer.rooms[currentRoomIndex])
    }

    fun startNewRun() {
        gold = 0
        blessings.clear()
        player.reset()
        currentLayerIndex = 0
        currentLayer = Layer.create(0)
        currentRoomIndex = 0
        currentRoom = currentLayer!!.rooms[0]
        loadRoom(currentRoom!!)
        gameState = GameState.PLAYING
    }

    fun selectBlessing(blessing: Blessing) {
        blessings.add(blessing)
        blessing.applyTo(player, this)
        gameState = GameState.PLAYING
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
        entities.addAll(enemies.filter { !it.isDead })
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
        hud.render(canvas, player, gold, blessings, currentLayerIndex, currentRoomIndex)
    }

    private fun renderGameOver(canvas: android.graphics.Canvas) {
        renderer.renderGameOver(canvas, screenWidth, screenHeight)
    }

    private fun renderVictory(canvas: android.graphics.Canvas) {
        renderer.renderVictory(canvas, screenWidth, screenHeight)
    }

    fun handleTouch(x: Float, y: Float) {
        when (gameState) {
            GameState.MENU -> {
                startNewRun()
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
            GameState.GAME_OVER, GameState.VICTORY -> {
                gameState = GameState.MENU
            }
            else -> {}
        }
    }
}
