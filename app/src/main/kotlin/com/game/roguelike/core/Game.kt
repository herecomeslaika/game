package com.game.roguelike.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.SurfaceHolder
import com.game.roguelike.R
import com.game.roguelike.audio.AudioManager
import com.game.roguelike.blessing.Blessing
import com.game.roguelike.blessing.BlessingSelector
import com.game.roguelike.core.GameState
import com.game.roguelike.core.GodType
import com.game.roguelike.combat.Projectile
import com.game.roguelike.entity.*
import com.game.roguelike.event.GameEvent
import com.game.roguelike.event.EventPool
import com.game.roguelike.event.EventEffect
import com.game.roguelike.event.FateOutcome
import com.game.roguelike.level.*
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.save.GameSaveData
import com.game.roguelike.save.GameSaveManager
import com.game.roguelike.shop.Shop
import com.game.roguelike.ui.ShopTouchResult
import com.game.roguelike.ui.*
import com.game.roguelike.util.Vector2
import com.game.roguelike.network.RoomManager
import kotlin.math.min

class Game(private val context: Context) {

    private var holder: SurfaceHolder? = null
    @Volatile
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
    val bossWarnings = mutableListOf<BossWarning>()
    val bossRelics = mutableListOf<BossRelic>()
    val meteorMarks = mutableListOf<MeteorMark>()

    var currentLayer: Layer? = null
    var currentRoom: Room? = null
    var currentLayerIndex = 0

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

    val audioManager = AudioManager(context)
    val roomManager = RoomManager(context)
    private val saveManager = GameSaveManager(context)

    var shakeAmount = 0f
    var shakeDuration = 0f
    var transitionAlpha = 0f
    var transitionTarget: GameState? = null
    private val layerTransitionState = LayerTransitionState()
    var spikeDamageTimer = 0f
    var frostFieldTimer = 0f
    var currentEvent: GameEvent? = null
    var selectedEventOption: Int = -1
    var eventResultText: String? = null
    var eventResultTimer = 0f
    var storyTimer = 0f
    var roomTransitionCooldown = 0f
    var gameOverFadeAlpha = 0f
    private var pendingReturnState: GameState = GameState.MENU

    // Boss entrance animation
    var bossEntranceTimer = 0f
    var bossEntrancePhase = 0
    var bossEntranceName = ""
    var bossEntranceTitle = ""
    var pendingBossType: EnemyType? = null
    private var bossFightStarted = false
    var bossPhaseText = ""
    var bossPhaseTextTimer = 0f
    var timeScale = 1f
    private var hitstopTimer = 0f
    var pendingBossRelicStoryType: BossRelicType? = null
    private var crownTrailTimer = 0f

    fun triggerHitstop(frames: Int) {
        hitstopTimer = frames * TICK
        timeScale = 0f
    }

    fun showBossMessage(text: String, duration: Float = 1.4f) {
        bossPhaseText = text
        bossPhaseTextTimer = duration
    }

    private fun updateHitstop(dt: Float) {
        if (hitstopTimer > 0f) {
            hitstopTimer -= dt
            if (hitstopTimer <= 0f) {
                hitstopTimer = 0f
                timeScale = 1f
            }
        }
    }

    // Input manager reference - set by GameSurfaceView
    var inputManager: InputManager? = null
    var vibrator: Vibrator? = null
    internal val screenRenderer = renderer.screenRenderer

    private var soundsLoaded = false

    private val TICK = 1f / 60f
    private var accumulator = 0f
    private var lastTime = 0L

    @Synchronized
    fun start(holder: SurfaceHolder) {
        if (running) return
        this.holder = holder
        running = true
        lastTime = System.nanoTime()
        if (!soundsLoaded) {
            audioManager.loadSounds(context)
            soundsLoaded = true
        }
        if (gameState == GameState.MENU) {
            audioManager.playBgm(context, R.raw.bgm_main)
        }
        
        // 设置 RoomManager 回调
        roomManager.onGameStart = {
            startNewRun()
        }
        
        gameThread = Thread { gameLoop() }
        gameThread?.start()
    }

    @Synchronized
    fun stop() {
        autoSaveIfNeeded()
        running = false
        audioManager.pauseBgm()
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            // ignore
        }
        gameThread = null
    }

    fun resumeAudio() {
        audioManager.resumeBgm()
    }

    fun releaseAudio() {
        audioManager.release()
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
            GameState.INTRO_STORY, GameState.BLESSING_STORY, GameState.BOSS_RELIC_STORY, GameState.ENDING_STORY, GameState.FAILURE_STORY -> updateStory(dt)
            GameState.MULTIPLAYER_LOBBY -> updateMultiplayerLobby(dt)
            GameState.ROOM_LIST -> updateRoomList(dt)
            GameState.ROOM_WAITING -> updateRoomWaiting(dt)
            GameState.PLAYING -> updatePlaying(scaledDt)
            GameState.BOSS_ENTRANCE -> updateBossEntrance(dt)
            GameState.BLESSING_SELECT -> updateBlessingSelect(dt)
            GameState.SHOP -> updateShop(dt)
            GameState.EVENT -> updateEvent(dt)
            GameState.OPTIONS, GameState.EXIT_CONFIRM, GameState.LOAD_SAVE_CONFIRM, GameState.SAVE_GAME_CONFIRM -> {}
            GameState.LAYER_TRANSITION -> updateTransition(dt)
            GameState.GAME_OVER, GameState.VICTORY -> updateEndScreen(dt)
            GameState.PLAYER_DEATH -> updatePlayerDeath(dt)
            else -> {}
        }
        updateHitstop(dt)
        updateShake(dt)
        updateParticles(dt)
        shopUI.update(dt)
    }

    private fun updateMenu(dt: Float) {
        audioManager.playBgm(context, R.raw.bgm_main)
    }

    private fun updateStory(dt: Float) {
        storyTimer += dt
        audioManager.playBgm(context, R.raw.bgm_main)
    }

    private fun updateMultiplayerLobby(dt: Float) {
        // Waiting for tap input via handleTouch in multiplayer lobby
    }

    private fun updateRoomList(dt: Float) {
        // Waiting for tap input via handleTouch to join a room
    }

    private fun updateRoomWaiting(dt: Float) {
        // Waiting for game start or players to join
    }

    private fun updatePlaying(dt: Float) {
        if (roomTransitionCooldown > 0f) roomTransitionCooldown -= dt
        val room = currentRoom ?: return
        val input = inputManager ?: return

        // Update virtual joystick visual
        virtualJoystick.updateKnob(input.joystickDirection)

        // Process attack input (long press = charge, release = whirlwind or short tap = combo)
        if (input.attackReleased) {
            input.attackReleased = false
            if (tryPickupNearbyBossRelic()) {
                return
            }
            // Check merchant interaction first (short tap near merchant)
            val nearMerchant = merchant?.let { m -> !m.talked && m.isNearPlayer } == true
            if (nearMerchant) {
                merchant!!.talked = true
                shop.open(gold, currentLayerIndex)
                gameState = GameState.SHOP
            } else {
                when (player.stateMachine.currentState) {
                    PlayerState.CHARGING -> {
                        if (player.chargeTime >= 0.3f) {
                            player.startWhirlwind(this)
                        } else {
                            player.startAttack1(this)
                        }
                    }
                    PlayerState.IDLE, PlayerState.RUN -> {
                        player.startAttack1(this)
                    }
                    PlayerState.ATTACK1, PlayerState.ATTACK2 -> {
                        player.tryComboAttack(this)
                    }
                    else -> {}
                }
            }
        } else if (input.attackDown) {
            when (player.stateMachine.currentState) {
                PlayerState.IDLE, PlayerState.RUN -> {
                    player.startCharging()
                }
                else -> {}
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
        updateBossRelics(dt)
        updateMeteorMarks(dt)
        updateCrownTrail(dt)

        // Merchant update
        merchant?.update(dt, this)

        // Assign surround slots to melee enemies
        val meleeEnemies = enemies.filter { !it.isDead && !it.isBoss && !it.isRanged }
        meleeEnemies.forEachIndexed { i, enemy -> enemy.surroundSlot = i }

        // Enemy updates - copy list to avoid ConcurrentModificationException
        // (enemy update can add new enemies via summoning)
        val enemiesToUpdate = enemies.toList()
        for (enemy in enemiesToUpdate) {
            enemy.update(dt, this)
        }

        updateBossWarnings(dt)

        // Remove dead enemies after all updates
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            if (enemy.isDead && enemy.deathAnimationDone) {
                if (enemy.isBoss) {
                    spawnBossRelic(enemy)
                }
                val dropAmount = if (currentRoom?.type == RoomType.ELITE) enemy.goldDrop * 2 else enemy.goldDrop
                gold += dropAmount
                audioManager.play("enemy_death")
                // Gold pickup particles
                for (i in 0..3) {
                    particles.add(Particle(
                        position = Vector2(enemy.position.x, enemy.position.y),
                        velocity = Vector2((kotlin.random.Random.nextFloat() - 0.5f) * 60f, -40f),
                        color = android.graphics.Color.parseColor("#FFD700"),
                        life = 0.5f,
                        size = 3f
                    ))
                }
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
        val canClearAfterCombat = RoomClearRewardPolicy.canClearAfterCombat(room.type, bossFightStarted)
        if (canClearAfterCombat && !room.cleared) {
            if (enemies.isEmpty()) {
                room.cleared = true
                room.unlockDoors()
                if (RoomClearRewardPolicy.grantsBlessing(room.type, currentLayerIndex, bossFightStarted)) {
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

        // Check player death — enter death animation first
        if (player.isDead && gameState != GameState.PLAYER_DEATH) {
            gameState = GameState.PLAYER_DEATH
            gameOverFadeAlpha = 0f
        }
    }

    private fun updateBlessingSelect(dt: Float) {
        blessingSelectUI.update(dt)
    }

    private fun updateShop(dt: Float) {
        shopUI.update(dt)
    }

    private fun updateEvent(dt: Float) {
        if (eventResultTimer > 0f) {
            eventResultTimer -= dt
            if (eventResultTimer <= 0f) {
                currentEvent = null
                eventResultText = null
                gameState = GameState.PLAYING
            }
        }
    }

    private fun updateBossEntrance(dt: Float) {
        bossEntranceTimer += dt

        when (bossEntrancePhase) {
            BossEntranceTimeline.CINEMATIC_PHASE -> {
                // Phase 0: boss-specific cinematic before title reveal
                shake(if (currentLayerIndex == 2) 9f else 5f, BossEntranceTimeline.CINEMATIC_DURATION)
                if (bossEntranceTimer < 0.05f) {
                    vibrate(if (currentLayerIndex == 2) 180 else 100)
                }
                if (bossEntranceTimer >= BossEntranceTimeline.CINEMATIC_DURATION) {
                    bossEntrancePhase = BossEntranceTimeline.TITLE_PHASE
                    bossEntranceTimer = 0f
                    timeScale = 1f
                    shake(10f, 2f)
                    vibrate(300)
                }
            }
            BossEntranceTimeline.TITLE_PHASE -> {
                // Phase 1: boss name display + heavy shake
                if (bossEntranceTimer >= BossEntranceTimeline.TITLE_DURATION) {
                    bossEntrancePhase = BossEntranceTimeline.FADE_PHASE
                    bossEntranceTimer = 0f
                    vibrate(50)
                }
            }
            BossEntranceTimeline.FADE_PHASE -> {
                // Phase 2: fade out, restore, spawn boss
                if (BossEntranceTimeline.shouldSpawnBoss(bossEntrancePhase, bossEntranceTimer)) {
                    val room = currentRoom ?: return
                    room.spawnBoss(this)
                    bossFightStarted = true
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
                val targetLayerIndex = layerTransitionState.consumeTargetLayerIndex()
                if (targetLayerIndex == null) {
                    transitionAlpha = 0f
                    transitionTarget = null
                    gameState = GameState.PLAYING
                    return
                }
                currentLayerIndex = targetLayerIndex
                if (currentLayerIndex >= 3) {
                    storyTimer = 0f
                    audioManager.playBgm(context, R.raw.bgm_main)
                    gameState = GameState.ENDING_STORY
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

    private fun updatePlayerDeath(dt: Float) {
        // Still update player so death animation timer advances
        player.update(dt, this)

        // Once death animation finishes, fade to game over
        if (player.deathAnimationDone) {
            gameOverFadeAlpha += dt * 1.5f
            if (gameOverFadeAlpha >= 1f) {
                gameOverFadeAlpha = 1f
                storyTimer = 0f
                audioManager.playBgm(context, R.raw.bgm_main)
                gameState = GameState.FAILURE_STORY
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

    private fun updateBossRelics(dt: Float) {
        val iter = bossRelics.iterator()
        while (iter.hasNext()) {
            val relic = iter.next()
            relic.update(dt)
            if (relic.type == BossRelicType.TITAN_MOLTEN_HEART && kotlin.random.Random.nextFloat() < 0.28f) {
                particles.add(Particle(
                    position = Vector2(relic.position.x, relic.position.y),
                    velocity = Vector2((kotlin.random.Random.nextFloat() - 0.5f) * 35f, -45f - kotlin.random.Random.nextFloat() * 45f),
                    color = android.graphics.Color.parseColor("#FF9A24"),
                    life = 0.55f,
                    size = 3f + kotlin.random.Random.nextFloat() * 2f,
                    heightOffset = 8f
                ))
            }

        }
    }

    private fun tryPickupNearbyBossRelic(): Boolean {
        val relic = bossRelics.firstOrNull { player.position.distanceTo(it.position) < 64f } ?: return false
        applyBossRelic(relic.type)
        bossRelics.remove(relic)
        return true
    }

    private fun updateMeteorMarks(dt: Float) {
        val iter = meteorMarks.iterator()
        while (iter.hasNext()) {
            val mark = iter.next()
            if (mark.update(dt)) {
                explodeMeteorMark(mark)
                iter.remove()
            }
        }
    }

    private fun updateCrownTrail(dt: Float) {
        if (!player.eternalCrown) return
        val isMoving = player.stateMachine.currentState == PlayerState.RUN || player.isDashing
        if (!isMoving) return

        crownTrailTimer -= dt
        if (crownTrailTimer <= 0f) {
            crownTrailTimer = 0.08f
            particles.add(Particle(
                position = Vector2(player.position.x, player.position.y + 5f),
                velocity = Vector2((kotlin.random.Random.nextFloat() - 0.5f) * 18f, -12f - kotlin.random.Random.nextFloat() * 18f),
                color = android.graphics.Color.parseColor("#FFE68A"),
                life = 0.45f,
                size = 2f + kotlin.random.Random.nextFloat() * 1.8f
            ))
        }
    }

    private fun spawnBossRelic(enemy: Enemy) {
        val type = BossRelicType.forLayer(enemy.layerIndex)
        if (player.hasBossRelic(type)) return
        if (bossRelics.any { it.type == type }) return
        bossRelics.add(BossRelic(type, enemy.position))
    }

    private fun applyBossRelic(type: BossRelicType) {
        if (!player.hasBossRelic(type)) {
            player.grantBossRelic(type)
        }
        pendingBossRelicStoryType = type
        storyTimer = 0f
        gameState = GameState.BOSS_RELIC_STORY
        audioManager.play("pickup")
        shake(if (type.grantsCombatPower) 7f else 5f, 0.25f)

        val color = when (type) {
            BossRelicType.GIANT_BONE_CORE -> android.graphics.Color.parseColor("#72E6FF")
            BossRelicType.TITAN_MOLTEN_HEART -> android.graphics.Color.parseColor("#FF7A22")
            BossRelicType.CROWN_OF_ETERNITY -> android.graphics.Color.parseColor("#FFE68A")
        }
        for (i in 0 until 28) {
            val angle = kotlin.random.Random.nextFloat() * 6.28318f
            val speed = 50f + kotlin.random.Random.nextFloat() * 130f
            particles.add(Particle(
                position = Vector2(player.position.x, player.position.y),
                velocity = Vector2(kotlin.math.cos(angle) * speed, kotlin.math.sin(angle) * speed * 0.65f),
                color = color,
                life = 0.75f + kotlin.random.Random.nextFloat() * 0.45f,
                size = 3f + kotlin.random.Random.nextFloat() * 3f
            ))
        }
    }

    fun spawnMeteorMark(position: Vector2) {
        if (meteorMarks.size >= 10) return
        meteorMarks.add(MeteorMark(position))
    }

    private fun explodeMeteorMark(mark: MeteorMark) {
        shake(10f, 0.18f)
        audioManager.play("enemy_death")
        for (enemy in enemies) {
            if (enemy.isDead) continue
            val distance = enemy.position.distanceTo(mark.position)
            if (distance <= mark.radius) {
                enemy.takeDamage(mark.damage, this)
                val knockDir = (enemy.position - mark.position).normalized
                enemy.applyKnockback(knockDir.x * 240f, knockDir.y * 240f - 90f)
            }
        }

        for (i in 0 until 32) {
            val angle = kotlin.random.Random.nextFloat() * 6.28318f
            val speed = 80f + kotlin.random.Random.nextFloat() * 220f
            particles.add(Particle(
                position = Vector2(mark.position.x, mark.position.y),
                velocity = Vector2(kotlin.math.cos(angle) * speed, kotlin.math.sin(angle) * speed * 0.65f),
                color = if (i % 3 == 0) android.graphics.Color.parseColor("#FFFF66") else android.graphics.Color.parseColor("#FF5A1F"),
                life = 0.5f + kotlin.random.Random.nextFloat() * 0.35f,
                size = 4f + kotlin.random.Random.nextFloat() * 4f
            ))
        }
    }

    private fun updateBossWarnings(dt: Float) {
        if (bossPhaseTextTimer > 0f) {
            bossPhaseTextTimer -= dt
            if (bossPhaseTextTimer <= 0f) {
                bossPhaseTextTimer = 0f
                bossPhaseText = ""
            }
        }

        val iter = bossWarnings.iterator()
        while (iter.hasNext()) {
            val warning = iter.next()
            warning.update(dt, this)
            if (warning.isDead) iter.remove()
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
        bossWarnings.clear()
        bossRelics.clear()
        meteorMarks.clear()
        bossPhaseText = ""
        bossPhaseTextTimer = 0f
        merchant = null

        player.position.set(room.spawnPoint)
        player.velocity.set(Vector2.ZERO)

        // BGM switching
        when (room.type) {
            RoomType.BOSS -> audioManager.playBgm(context, R.raw.bgm_boss)
            RoomType.COMBAT, RoomType.ELITE -> audioManager.playBgm(context, R.raw.bgm_battle)
            else -> audioManager.stopBgm()
        }

        when (room.type) {
            RoomType.ENTRY -> {}
            RoomType.COMBAT -> room.spawnEnemies(this)
            RoomType.ELITE -> room.spawnElite(this)
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
                val bossConfig = com.game.roguelike.entity.EnemyConfig.bossForLayer(currentLayerIndex)
                val bossType = bossConfig?.type ?: EnemyType.MEGA_SKELETON
                bossFightStarted = false
                pendingBossType = bossType
                bossEntranceName = bossType.bossName
                bossEntranceTitle = bossType.bossTitle
                bossEntranceTimer = 0f
                bossEntrancePhase = BossEntranceTimeline.initialPhase
                timeScale = 0.2f
                gameState = GameState.BOSS_ENTRANCE
            }
            RoomType.EVENT -> {
                currentEvent = EventPool.rollEvent(currentLayerIndex)
                selectedEventOption = -1
                eventResultText = null
                eventResultTimer = 0f
                gameState = GameState.EVENT
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
        if (gameState == GameState.LAYER_TRANSITION) return
        val layer = currentLayer ?: return
        val connected = layer.getConnectedRooms()

        if (connected.isEmpty()) {
            // No more rooms — boss cleared, transition to next layer
            if (layer.isBossRoom() && currentRoom?.cleared == true) {
                if (!layerTransitionState.beginFrom(currentLayerIndex)) return
                gameState = GameState.LAYER_TRANSITION
                transitionAlpha = 0f
                transitionTarget = null
                audioManager.play("level_up")
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
        bossRelics.clear()
        meteorMarks.clear()
        player.reset()
        currentLayerIndex = 0
        currentLayer = Layer(currentLayerIndex)
        currentRoom = currentLayer!!.getCurrentRoom()
        loadRoom(currentRoom!!)
        timeScale = 1f
        bossEntranceTimer = 0f
        bossEntrancePhase = BossEntranceTimeline.initialPhase
        pendingBossType = null
        bossFightStarted = false
        layerTransitionState.reset()
        gameState = GameState.PLAYING
        audioManager.playBgm(context, R.raw.bgm_battle)
    }

    fun startIntroStory() {
        storyTimer = 0f
        gameState = GameState.INTRO_STORY
        audioManager.playBgm(context, R.raw.bgm_main)
    }

    fun continueIntroStory() {
        storyTimer = 0f
        gameState = GameState.BLESSING_STORY
        audioManager.playBgm(context, R.raw.bgm_main)
    }

    fun skipIntroStory() {
        startNewRun()
    }

    fun skipBlessingStory() {
        startNewRun()
    }

    fun skipEndingStory() {
        enterMainMenu()
    }

    fun skipFailureStory() {
        enterMainMenu()
    }

    private fun enterMainMenu() {
        gameState = GameState.MENU
        storyTimer = 0f
        inputManager?.reset()
        hud.closeBlessingPanel()
        audioManager.playBgm(context, R.raw.bgm_main)
    }

    fun selectBlessing(blessing: Blessing) {
        blessings.add(blessing)
        player.ownedGods.add(blessing.god)
        applyBlessingEffect(blessing, player)
        gameState = GameState.PLAYING
        audioManager.play("blessing")
    }

    private fun applyBlessingEffect(blessing: Blessing, player: Player) {
        blessing.onApply?.invoke(player)
    }

    fun saveCurrentRun(): Boolean {
        val saveData = createSaveData() ?: return false
        saveManager.save(saveData)
        return true
    }

    fun autoSaveIfNeeded() {
        if (isSaveableGameplayState()) {
            saveCurrentRun()
        }
    }

    private fun isSaveableGameplayState(): Boolean {
        if (player.isDead) return false
        return gameState == GameState.PLAYING ||
            gameState == GameState.BLESSING_SELECT ||
            gameState == GameState.SHOP ||
            gameState == GameState.EVENT ||
            gameState == GameState.BOSS_ENTRANCE ||
            gameState == GameState.BOSS_RELIC_STORY
    }

    private fun createSaveData(): GameSaveData? {
        val layer = currentLayer ?: return null
        val room = currentRoom ?: return null
        val savedState = if (gameState == GameState.BLESSING_SELECT) GameState.BLESSING_SELECT.name else GameState.PLAYING.name
        val offeringIds = if (gameState == GameState.BLESSING_SELECT) {
            blessingSelector.currentOffering.map { it.id }
        } else {
            emptyList()
        }
        return GameSaveData(
            version = 1,
            layerIndex = currentLayerIndex.coerceIn(0, 2),
            roomId = layer.currentRoomId,
            roomCleared = room.cleared,
            playerHealth = player.health.coerceAtLeast(1),
            playerMaxHealth = player.maxHealth.coerceAtLeast(1),
            gold = gold.coerceAtLeast(0),
            blessingIds = blessings.map { it.id },
            state = savedState,
            offeringIds = offeringIds,
            bossRelicIds = player.bossRelicIds(),
            droppedBossRelics = bossRelics.map { it.toSaveString() }
        )
    }

    private fun continueFromSave(): Boolean {
        val save = saveManager.load() ?: return false
        player.reset()
        blessings.clear()
        blessingSelector.clearOffering()

        currentLayerIndex = save.layerIndex.coerceIn(0, 2)
        currentLayer = Layer(currentLayerIndex)
        val room = currentLayer!!.goToRoom(save.roomId)
        loadRoom(room)

        restoreBlessings(save.blessingIds)
        player.restoreBossRelics(save.bossRelicIds)
        player.maxHealth = save.playerMaxHealth.coerceAtLeast(1)
        player.health = save.playerHealth.coerceIn(1, player.maxHealth)
        gold = save.gold.coerceAtLeast(0)

        currentRoom?.let { restoredRoom ->
            restoredRoom.cleared = save.roomCleared
            if (restoredRoom.cleared) {
                restoredRoom.unlockDoors()
                enemies.clear()
                bossFightStarted = false
                pendingBossType = null
                timeScale = 1f
                gameState = GameState.PLAYING
            }
        }

        bossRelics.clear()
        save.droppedBossRelics.mapNotNull { BossRelic.fromSaveString(it) }
            .filter { !player.hasBossRelic(it.type) }
            .forEach { bossRelics.add(it) }

        val offerings = save.offeringIds.mapNotNull { findBlessingById(it) }
        if (save.state == GameState.BLESSING_SELECT.name && offerings.isNotEmpty()) {
            blessingSelector.restoreOffering(offerings)
            gameState = GameState.BLESSING_SELECT
        } else if (gameState != GameState.BOSS_ENTRANCE) {
            gameState = GameState.PLAYING
        }
        return true
    }

    private fun restoreBlessings(ids: List<String>) {
        ids.mapNotNull { findBlessingById(it) }.forEach { blessing ->
            blessings.add(blessing)
            player.ownedGods.add(blessing.god)
            applyBlessingEffect(blessing, player)
        }
    }

    private fun findBlessingById(id: String): Blessing? {
        return (Blessing.ALL_BLESSINGS + Blessing.ALL_DUO_BLESSINGS).firstOrNull { it.id == id }
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
        try {
            canvas.drawColor(android.graphics.Color.BLACK)

            when (gameState) {
                GameState.MENU -> renderMenu(canvas)
                GameState.INTRO_STORY -> renderIntroStory(canvas)
                GameState.BLESSING_STORY -> renderBlessingStory(canvas)
                GameState.BOSS_RELIC_STORY -> renderBossRelicStory(canvas)
                GameState.MULTIPLAYER_LOBBY -> renderMultiplayerLobby(canvas)
                GameState.ROOM_LIST -> renderRoomList(canvas)
                GameState.ROOM_WAITING -> renderRoomWaiting(canvas)
                GameState.PLAYING -> renderPlaying(canvas)
                GameState.BOSS_ENTRANCE -> {
                    renderPlaying(canvas)
                    if (bossEntrancePhase == BossEntranceTimeline.CINEMATIC_PHASE) {
                        renderer.renderBossEntrance(canvas, bossEntranceName, bossEntranceTitle, bossEntranceTimer, bossEntrancePhase, screenWidth, screenHeight, currentLayerIndex)
                        currentRoom?.let {
                            renderer.renderBossEntranceCinematic(canvas, it, currentLayerIndex, bossEntranceTimer)
                        }
                    } else {
                        renderer.renderBossEntrance(canvas, bossEntranceName, bossEntranceTitle, bossEntranceTimer, bossEntrancePhase, screenWidth, screenHeight, currentLayerIndex)
                    }
                }
                GameState.BLESSING_SELECT -> {
                    renderPlaying(canvas)
                    blessingSelectUI.render(canvas, blessingSelector.currentOffering)
                }
                GameState.SHOP -> {
                    renderPlaying(canvas)
                    shopUI.render(canvas, shop, gold)
                }
                GameState.EVENT -> {
                    renderPlaying(canvas)
                    renderEvent(canvas)
                }
                GameState.OPTIONS -> {
                    renderMenu(canvas)
                    screenRenderer.renderOptions(
                        canvas = canvas,
                        w = screenWidth,
                        h = screenHeight,
                        bgmVolume = audioManager.bgmVolume,
                        sfxVolume = audioManager.sfxVolume,
                        muted = audioManager.muted
                    )
                }
                GameState.LOAD_SAVE_CONFIRM -> {
                    renderMenu(canvas)
                    screenRenderer.renderLoadSaveConfirm(canvas, screenWidth, screenHeight)
                }
                GameState.SAVE_GAME_CONFIRM -> {
                    renderPlaying(canvas)
                    screenRenderer.renderSaveGameConfirm(canvas, screenWidth, screenHeight)
                }
                GameState.EXIT_CONFIRM -> {
                    if (pendingReturnState == GameState.PLAYING) {
                        renderPlaying(canvas)
                    } else {
                        renderMenu(canvas)
                    }
                    screenRenderer.renderExitConfirm(canvas, screenWidth, screenHeight, pendingReturnState == GameState.PLAYING)
                }
                GameState.LAYER_TRANSITION -> {
                    renderPlaying(canvas)
                    renderer.drawFade(canvas, transitionAlpha)
                }
                GameState.GAME_OVER -> renderGameOver(canvas)
                GameState.VICTORY -> renderVictory(canvas)
                GameState.ENDING_STORY -> renderEndingStory(canvas)
                GameState.FAILURE_STORY -> renderFailureStory(canvas)
                GameState.PLAYER_DEATH -> {
                    renderPlaying(canvas)
                    renderer.drawFade(canvas, gameOverFadeAlpha)
                }
                else -> {}
            }
        } catch (e: Exception) {
            android.util.Log.e("Game", "Render error", e)
        }
    }

    private fun renderMenu(canvas: android.graphics.Canvas) {
        renderer.renderMenu(canvas, screenWidth, screenHeight)
    }

    private fun renderIntroStory(canvas: android.graphics.Canvas) {
        renderer.renderIntroStory(canvas, screenWidth, screenHeight)
    }

    private fun renderBlessingStory(canvas: android.graphics.Canvas) {
        renderer.renderBlessingStory(canvas, screenWidth, screenHeight)
    }

    private fun renderBossRelicStory(canvas: android.graphics.Canvas) {
        renderer.renderBossRelicStory(canvas, screenWidth, screenHeight, pendingBossRelicStoryType)
    }

    private fun renderEndingStory(canvas: android.graphics.Canvas) {
        renderer.renderEndingStory(canvas, screenWidth, screenHeight)
    }

    private fun renderFailureStory(canvas: android.graphics.Canvas) {
        renderer.renderFailureStory(canvas, screenWidth, screenHeight)
    }

    private fun renderMultiplayerLobby(canvas: android.graphics.Canvas) {
        renderer.renderMultiplayerLobby(canvas, screenWidth, screenHeight)
    }

    private fun renderRoomList(canvas: android.graphics.Canvas) {
        renderer.renderRoomList(canvas, screenWidth, screenHeight, roomManager.discoveredRooms)
    }

    private fun renderRoomWaiting(canvas: android.graphics.Canvas) {
        renderer.renderRoomWaiting(canvas, screenWidth, screenHeight, roomManager)
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
        val cameraFocus = if (gameState == GameState.BOSS_ENTRANCE) {
            Vector2(room.width * 32f, room.height * 16f)
        } else {
            player.position
        }
        renderer.renderRoom(canvas, room, cameraFocus, TICK)

        // Boss skill warnings
        for (warning in bossWarnings) {
            renderer.renderBossWarning(canvas, warning)
        }

        for (mark in meteorMarks) {
            renderer.renderMeteorMark(canvas, mark)
        }

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

        for (relic in bossRelics) {
            renderer.renderBossRelic(canvas, relic, player.position.distanceTo(relic.position) < 96f)
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
        hud.render(
            canvas = canvas,
            player = player,
            gold = gold,
            blessings = blessings,
            layerIndex = currentLayerIndex,
            boss = enemies.firstOrNull { it.isBoss && !it.deathAnimationDone },
            bossPhaseText = bossPhaseText,
            bossPhaseTextTimer = bossPhaseTextTimer
        )
    }

    private fun renderGameOver(canvas: android.graphics.Canvas) {
        renderer.renderGameOver(canvas, screenWidth, screenHeight)
    }

    private fun renderVictory(canvas: android.graphics.Canvas) {
        renderer.renderVictory(canvas, screenWidth, screenHeight)
    }

    private fun renderEvent(canvas: android.graphics.Canvas) {
        val event = currentEvent ?: return
        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Dark overlay
        p.color = android.graphics.Color.argb(180, 10, 15, 30)
        p.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, p)

        // Panel
        val panelW = 700f
        val panelX = w / 2f - panelW / 2f
        val panelY = h * 0.15f
        val panelBottom = h * 0.85f

        p.color = android.graphics.Color.argb(230, 20, 25, 45)
        canvas.drawRoundRect(panelX, panelY, panelX + panelW, panelBottom, 12f, 12f, p)

        // Panel border
        p.color = event.npcColor
        p.style = android.graphics.Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawRoundRect(panelX, panelY, panelX + panelW, panelBottom, 12f, 12f, p)

        // NPC name + description
        val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = event.npcColor
            textSize = 40f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("${event.npcName} — ${event.title}", w / 2f, panelY + 55f, titlePaint)

        val descPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CCCCCC")
            textSize = 20f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(event.description, w / 2f, panelY + 85f, descPaint)

        // Options or result
        if (eventResultText != null) {
            val resultPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FFD700")
                textSize = 32f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText(eventResultText!!, w / 2f, h / 2f, resultPaint)

            val continuePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#AAAAAA")
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("点击继续...", w / 2f, h / 2f + 50f, continuePaint)
        } else {
            val optionStartY = panelY + 120f
            val optionHeight = 130f
            val optionSpacing = 15f

            for (i in event.options.indices) {
                val option = event.options[i]
                val oy = optionStartY + i * (optionHeight + optionSpacing)

                // Option card background
                val isSelected = selectedEventOption == i
                p.color = if (isSelected) android.graphics.Color.argb(200, 50, 55, 80) else android.graphics.Color.argb(150, 35, 40, 60)
                p.style = android.graphics.Paint.Style.FILL
                canvas.drawRoundRect(panelX + 30f, oy, panelX + panelW - 30f, oy + optionHeight, 8f, 8f, p)

                // Option card border
                p.color = if (isSelected) android.graphics.Color.parseColor("#FFD700") else android.graphics.Color.parseColor("#7788AA")
                p.style = android.graphics.Paint.Style.STROKE
                p.strokeWidth = if (isSelected) 3f else 1f
                canvas.drawRoundRect(panelX + 30f, oy, panelX + panelW - 30f, oy + optionHeight, 8f, 8f, p)

                // Option text
                val optNamePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = 26f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                canvas.drawText(option.text, panelX + 55f, oy + 35f, optNamePaint)

                // Cost/reward descriptions
                val optDescPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 18f
                }
                optDescPaint.color = android.graphics.Color.parseColor("#FF6666")
                canvas.drawText("代价: ${option.costDescription}", panelX + 55f, oy + 65f, optDescPaint)
                optDescPaint.color = android.graphics.Color.parseColor("#66CC66")
                canvas.drawText("收益: ${option.rewardDescription}", panelX + 55f, oy + 90f, optDescPaint)

                // Selection number
                val numPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#7788AA")
                    textSize = 32f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.drawText("${i + 1}", panelX + panelW - 70f, oy + 75f, numPaint)
            }
        }
    }

        fun handleTouch(x: Float, y: Float) {
        when (gameState) {
            GameState.PLAYING -> {
                if (hud.handleBlessingPanelClick(x, y)) return
                if (hud.handleBackButtonClick(x, y)) {
                    pendingReturnState = GameState.PLAYING
                    gameState = GameState.EXIT_CONFIRM
                    return
                }
            }
            GameState.MENU -> {
                when {
                    screenRenderer.startBtnRect.contains(x, y) -> {
                        if (saveManager.hasSave()) {
                            gameState = GameState.LOAD_SAVE_CONFIRM
                        } else {
                            startIntroStory()
                        }
                    }
                    screenRenderer.multiplayerBtnRect.contains(x, y) -> gameState = GameState.MULTIPLAYER_LOBBY
                    screenRenderer.optionsBtnRect.contains(x, y) -> {
                        pendingReturnState = GameState.MENU
                        gameState = GameState.OPTIONS
                    }
                    screenRenderer.exitBtnRect.contains(x, y) -> {
                        pendingReturnState = GameState.MENU
                        gameState = GameState.EXIT_CONFIRM
                    }
                }
            }
            GameState.OPTIONS -> {
                when {
                    screenRenderer.optionsBgmSliderRect.contains(x, y) -> {
                        val volume = sliderValue(x, screenRenderer.optionsBgmSliderRect)
                        audioManager.setBgmVolume(volume)
                    }
                    screenRenderer.optionsSfxSliderRect.contains(x, y) -> {
                        val volume = sliderValue(x, screenRenderer.optionsSfxSliderRect)
                        audioManager.setSfxVolume(volume)
                    }
                    screenRenderer.optionsMainBgmBtnRect.contains(x, y) -> {
                        audioManager.playBgm(context, R.raw.bgm_main)
                    }
                    screenRenderer.optionsBattleBgmBtnRect.contains(x, y) -> {
                        audioManager.playBgm(context, R.raw.bgm_battle)
                    }
                    screenRenderer.optionsBossBgmBtnRect.contains(x, y) -> {
                        audioManager.playBgm(context, R.raw.bgm_boss)
                    }
                    screenRenderer.optionsStopBgmBtnRect.contains(x, y) -> {
                        audioManager.stopBgm()
                    }
                    screenRenderer.optionsMuteBtnRect.contains(x, y) -> {
                        audioManager.toggleMuted()
                    }
                    screenRenderer.optionsBackBtnRect.contains(x, y) -> {
                        gameState = pendingReturnState
                    }
                }
            }
            GameState.EXIT_CONFIRM -> {
                when {
                    screenRenderer.confirmCancelBtnRect.contains(x, y) -> gameState = pendingReturnState
                    screenRenderer.confirmOkBtnRect.contains(x, y) -> {
                        if (pendingReturnState == GameState.PLAYING) {
                            gameState = GameState.SAVE_GAME_CONFIRM
                        } else {
                            val activity = context as android.app.Activity
                            activity.finishAndRemoveTask()
                        }
                    }
                }
            }
            GameState.LOAD_SAVE_CONFIRM -> {
                when {
                    screenRenderer.confirmCancelBtnRect.contains(x, y) -> {
                        saveManager.deleteSave()
                        startIntroStory()
                    }
                    screenRenderer.confirmOkBtnRect.contains(x, y) -> {
                        if (!continueFromSave()) {
                            saveManager.deleteSave()
                            startIntroStory()
                        }
                    }
                }
            }
            GameState.SAVE_GAME_CONFIRM -> {
                when {
                    screenRenderer.confirmCancelBtnRect.contains(x, y) -> enterMainMenu()
                    screenRenderer.confirmOkBtnRect.contains(x, y) -> {
                        saveCurrentRun()
                        enterMainMenu()
                    }
                }
            }
            GameState.MULTIPLAYER_LOBBY -> {
                if (screenRenderer.createRoomBtnRect.contains(x, y)) {
                    roomManager.createRoom("房间")
                    gameState = GameState.ROOM_WAITING
                } else if (screenRenderer.joinRoomBtnRect.contains(x, y)) {
                    roomManager.scanRooms()
                    gameState = GameState.ROOM_LIST
                } else if (screenRenderer.backToMenuBtnRect.contains(x, y)) {
                    enterMainMenu()
                }
            }
            GameState.ROOM_LIST -> {
                var clicked = false
                roomManager.discoveredRooms.forEachIndexed { index, room ->
                    if (index < screenRenderer.roomListRects.size && screenRenderer.roomListRects[index].contains(x, y)) {
                        roomManager.joinRoom(room)
                        gameState = GameState.ROOM_WAITING
                        clicked = true
                    }
                }
                if (!clicked && screenRenderer.backToMenuBtnRect.contains(x, y)) {
                    roomManager.stop()
                    gameState = GameState.MULTIPLAYER_LOBBY
                }
            }
            GameState.ROOM_WAITING -> {
                if (!roomManager.isHost && screenRenderer.readyBtnRect.contains(x, y)) {
                    roomManager.setReady()
                }
                if (roomManager.isHost && screenRenderer.startGameBtnRect.contains(x, y)) {
                    roomManager.startGame()
                }
                if (screenRenderer.leaveRoomBtnRect.contains(x, y)) {
                    roomManager.stop()
                    gameState = GameState.MULTIPLAYER_LOBBY
                }
            }
            GameState.INTRO_STORY -> {
                if (screenRenderer.storyNextBtnRect.contains(x, y)) {
                    continueIntroStory()
                } else if (screenRenderer.storySkipBtnRect.contains(x, y)) {
                    skipIntroStory()
                }
            }
            GameState.BLESSING_STORY -> skipBlessingStory()
            GameState.BOSS_RELIC_STORY -> {
                if (storyTimer < 0.25f) return
                pendingBossRelicStoryType = null
                gameState = GameState.PLAYING
            }
            GameState.ENDING_STORY -> skipEndingStory()
            GameState.FAILURE_STORY -> skipFailureStory()
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
                val (item, result) = shopUI.handleTouch(x, y, shop, gold)
                when (result) {
                    ShopTouchResult.PURCHASED -> {
                        if (item != null) {
                            gold -= item.cost
                            item.sold = true
                            item.applyEffect(player, this)
                            audioManager.play("pickup")
                        }
                    }
                    ShopTouchResult.CANT_AFFORD -> {
                        shopUI.purchaseFailedTimer = 0.8f
                    }
                    ShopTouchResult.OUTSIDE -> {
                        shop.close()
                        gameState = GameState.PLAYING
                    }
                }
            }
            GameState.GAME_OVER, GameState.VICTORY -> {
                enterMainMenu()
            }
            GameState.EVENT -> handleTouchEvent(x, y)
            else -> {}
        }
    }

    private fun sliderValue(x: Float, rect: android.graphics.RectF): Float {
        val width = (rect.right - rect.left).coerceAtLeast(1f)
        return ((x - rect.left) / width).coerceIn(0f, 1f)
    }

    private fun handleTouchEvent(x: Float, y: Float) {
        // If showing result, any tap dismisses
        if (eventResultText != null) {
            if (eventResultTimer <= 0f) {
                currentEvent = null
                eventResultText = null
                gameState = GameState.PLAYING
            }
            return
        }

        val event = currentEvent ?: return
        val w = screenWidth.toFloat()
        val h = screenHeight.toFloat()
        val panelW = 700f
        val panelX = w / 2f - panelW / 2f
        val panelY = h * 0.15f
        val optionStartY = panelY + 120f
        val optionHeight = 130f
        val optionSpacing = 15f

        for (i in event.options.indices) {
            val oy = optionStartY + i * (optionHeight + optionSpacing)
            if (x >= panelX + 30f && x <= panelX + panelW - 30f &&
                y >= oy && y <= oy + optionHeight
            ) {
                applyEventEffect(event.options[i].effect)
                return
            }
        }
    }

    private fun applyEventEffect(effect: EventEffect) {
        var resultMsg = ""
        when (effect) {
            is EventEffect.LoseHpGiveGold -> {
                val hpLoss = (player.maxHealth * effect.hpPercent).toInt()
                player.health = (player.health - hpLoss).coerceAtLeast(1)
                gold += effect.gold
                resultMsg = "失去${hpLoss}生命，获得${effect.gold}金币"
                spawnGoldParticles()
            }
            is EventEffect.LoseGoldGiveBlessing -> {
                if (gold >= effect.gold) {
                    gold -= effect.gold
                    gameState = GameState.BLESSING_SELECT
                    blessingSelector.generateOffering(currentLayerIndex, blessings)
                    currentEvent = null
                    eventResultText = null
                    audioManager.play("pickup")
                    return
                } else {
                    resultMsg = "金币不足! 需要${effect.gold}金币"
                }
            }
            is EventEffect.LoseMaxHpGiveDamage -> {
                val maxHpLoss = (player.maxHealth * effect.maxHpPercent).toInt()
                player.maxHealth -= maxHpLoss
                player.health = player.health.coerceAtMost(player.maxHealth)
                player.attackDamage1 += effect.damageBonus
                player.attackDamage2 += effect.damageBonus
                player.attackDamage3 += effect.damageBonus
                resultMsg = "最大生命-${maxHpLoss}，攻击+${effect.damageBonus.toInt()}"
            }
            is EventEffect.GambleGold -> {
                val stake = (gold * effect.stakePercent).toInt()
                if (kotlin.random.Random.nextFloat() < 0.5f) {
                    gold += stake
                    resultMsg = "赢了! 获得${stake}金币"
                    spawnGoldParticles()
                } else {
                    gold -= stake
                    resultMsg = "输了... 失去${stake}金币"
                }
            }
            is EventEffect.RestHeal -> {
                if (effect.healPercent > 0f) {
                    val heal = (player.maxHealth * effect.healPercent).toInt()
                    player.health = (player.health + heal).coerceAtMost(player.maxHealth)
                    resultMsg = "恢复${heal}生命"
                } else {
                    // "do nothing" option — give small consolation gold
                    gold += 15
                    resultMsg = "获得15金币"
                    spawnGoldParticles()
                }
            }
            is EventEffect.RandomBlessingOrCurse -> {
                gameState = GameState.BLESSING_SELECT
                blessingSelector.generateOffering(currentLayerIndex, blessings)
                currentEvent = null
                eventResultText = null
                audioManager.play("pickup")
                return
            }
            is EventEffect.UpgradeWeapon -> {
                player.attackDamage1 += effect.damageBonus
                player.attackDamage2 += effect.damageBonus
                player.attackDamage3 += effect.damageBonus
                resultMsg = "攻击+${effect.damageBonus.toInt()}"
                audioManager.play("pickup")
            }
            is EventEffect.LoseHpGiveBlessing -> {
                val hpLoss = (player.maxHealth * effect.hpPercent).toInt()
                player.health = (player.health - hpLoss).coerceAtLeast(1)
                gameState = GameState.BLESSING_SELECT
                blessingSelector.generateOffering(currentLayerIndex, blessings)
                currentEvent = null
                eventResultText = null
                return
            }
            is EventEffect.SpendGoldGiveDamage -> {
                if (effect.gold > 0) {
                    if (gold >= effect.gold) {
                        gold -= effect.gold
                        if (effect.damageBonus > 0f) {
                            player.attackDamage1 += effect.damageBonus
                            player.attackDamage2 += effect.damageBonus
                            player.attackDamage3 += effect.damageBonus
                            resultMsg = "花费${effect.gold}金币，攻击+${effect.damageBonus.toInt()}"
                        } else {
                            val heal = (player.maxHealth * 0.5f).toInt()
                            player.health = (player.health + heal).coerceAtMost(player.maxHealth)
                            resultMsg = "花费${effect.gold}金币，恢复${heal}生命"
                        }
                    } else {
                        resultMsg = "金币不足!"
                    }
                } else {
                    // Free option with no effect (consolation)
                    gold += 15
                    player.speed *= 0.95f
                    resultMsg = "获得15金币，速度微降"
                    spawnGoldParticles()
                }
            }
            is EventEffect.FateWheel -> {
                val outcome = effect.outcomes[kotlin.random.Random.nextInt(effect.outcomes.size)]
                when (outcome.description) {
                    "获得30金币" -> { gold += 30; spawnGoldParticles() }
                    "攻击+3" -> { player.attackDamage1 += 3f; player.attackDamage2 += 3f; player.attackDamage3 += 3f }
                    "恢复40%生命" -> { val heal = (player.maxHealth * 0.4f).toInt(); player.health = (player.health + heal).coerceAtMost(player.maxHealth) }
                    "什么都没有" -> {}
                }
                resultMsg = "命运之轮: ${outcome.description}"
            }
            is EventEffect.LoseAllGoldHealAndDamage -> {
                val lostGold = gold
                gold = 0
                val heal = (player.maxHealth * effect.healPercent).toInt()
                player.health = (player.health + heal).coerceAtMost(player.maxHealth)
                player.attackDamage1 += effect.damageBonus
                player.attackDamage2 += effect.damageBonus
                player.attackDamage3 += effect.damageBonus
                resultMsg = "失去${lostGold}金币，恢复${heal}生命，攻击+${effect.damageBonus.toInt()}"
            }
            is EventEffect.CoinFlipBlessing -> {
                if (gold >= effect.gold) {
                    gold -= effect.gold
                    if (kotlin.random.Random.nextFloat() < effect.chance) {
                        player.attackDamage1 += 2f
                        player.attackDamage2 += 2f
                        player.attackDamage3 += 2f
                        resultMsg = "好运! 攻击+2"
                    } else {
                        resultMsg = "运气不佳... 失去${effect.gold}金币"
                    }
                } else {
                    resultMsg = "金币不足!"
                }
            }
            is EventEffect.AlchemistExperiment -> {
                val hpLoss = (player.maxHealth * effect.hpCost).toInt()
                player.health = (player.health - hpLoss).coerceAtLeast(1)
                if (kotlin.random.Random.nextFloat() < 0.75f) {
                    val heal = (player.maxHealth * effect.successHeal).toInt()
                    player.health = (player.health + heal).coerceAtMost(player.maxHealth)
                    resultMsg = "药水生效! 恢复${heal}生命"
                } else {
                    resultMsg = "药水失败了..."
                }
            }
        }

        eventResultText = resultMsg
        eventResultTimer = 1.5f
        audioManager.play("pickup")
    }

    private fun spawnGoldParticles() {
        for (i in 0..5) {
            particles.add(Particle(
                position = Vector2(player.position.x, player.position.y),
                velocity = Vector2((kotlin.random.Random.nextFloat() - 0.5f) * 80f, -50f),
                color = android.graphics.Color.parseColor("#FFD700"),
                life = 0.6f,
                size = 4f
            ))
        }
    }
}

