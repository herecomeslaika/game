package com.game.roguelike.entity

import android.graphics.Canvas
import com.game.roguelike.combat.Projectile
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.core.EnemyState
import com.game.roguelike.core.Game
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.util.StateMachine
import com.game.roguelike.util.Vector2
import kotlin.random.Random
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class EnemyType {
    SKELETON, WRAITH, MEGA_SKELETON,
    FLAME_DANCER, LAVA_CASTER, INFERNO_TITAN,
    SHIELD_BEARER, SPEAR_THROWER, CHAMPION
}

val EnemyType.bossName: String
    get() = EnemyConfig.forType(this).bossName

val EnemyType.bossTitle: String
    get() = EnemyConfig.forType(this).bossTitle

class Enemy(
    val type: EnemyType,
    spawnPos: Vector2,
    val layerIndex: Int = 0,
    val isBoss: Boolean = false
) : Entity() {

    val config = EnemyConfig.forType(type)
    val behavior = EnemyBehaviors.forType(type)

    override var width = config.width
    override var height = config.height
    var maxHealth = config.maxHealth
    var health = config.maxHealth
    var speed = config.speed
    var attackDamage = config.attackDamage
    var attackRange = config.attackRange
    var attackCooldown = config.attackCooldown
    var attackCooldownTimer = 0f
    var aggroRange = config.aggroRange
    var goldDrop = config.goldDrop
    var name = config.name
    var facingRight = true
    var isDead = false
    var deathAnimationDone = false

    var deathAnimationTimer = 0f
    val deathAnimationDuration = 0.6f

    var hasShield = config.hasShield
    var shieldDirection = 1

    var isRanged = config.isRanged
    var projectileSpeed = config.projectileSpeed
    var projectileType = config.projectileType

    var phase = 0
    var phaseThreshold = 0.5f
    var phaseThresholds = config.bossPhaseThresholds
    var isPhaseTransitioning = false
    var phaseTransitionTimer = 0f

    var canSummon = config.canSummon
    var summonCooldown = config.summonCooldown
    var summonTimer = if (config.canSummon) 0f else 0f
    var summonCount = config.summonCount

    var leavesFireTrail = config.leavesFireTrail
    var fireTrailTimer = 0f

    var canCharge = config.canCharge
    var isCharging = false
    var chargeTimer = 0f
    var chargeDirection = Vector2.ZERO
    var chargeSpeed = config.chargeSpeed

    val stateMachine = StateMachine(EnemyState.IDLE)
    private var patrolTarget = Vector2.ZERO
    var stateTimer = 0f

    private var moveAnimTime = 0f
    var moveAnimPhase = 0f
    var idleTime = 0f
    var walkBlend = 0f

    private var prepareAttackTimer = 0f

    var comboCount = 0
    var comboTimer = 0f

    var canPhaseShift = config.canPhaseShift
    var phaseShiftCooldown = config.phaseShiftCooldown
    var phaseShiftTimer = 0f

    var canFlameDash = config.canFlameDash
    var flameDashCooldown = config.flameDashCooldown
    var flameDashTimer = 0f
    var isFlameDashing = false
    var flameDashDuration = 0f
    var flameDashDir = Vector2.ZERO

    var castCount = 0
    var lavaPoolTimer = 5f

    var shieldBashCooldown = config.shieldBashCooldown
    var shieldBashTimer = 0f
    var isShieldBashing = false
    var shieldBashDuration = 0f
    var shieldBashDir = Vector2.ZERO
    var shieldThrown = false

    var multiSpearCount = 0
    var multiSpearTimer = 0f
    var isRetreating = false

    var canGroundSlam = config.canGroundSlam
    var groundSlamCooldown = config.groundSlamCooldown
    var groundSlamTimer = 0f
    var isGroundSlamming = false
    var groundSlamPhase = 0
    var groundSlamHoverTimer = 0f

    var chargeComboCount = 0
    var chargeComboMax = config.chargeComboMax
    var chargePauseTimer = 0f
    var meteorCooldown = config.meteorCooldown
    var meteorTimer = 0f
    var canMeteor = config.canMeteor
    var meteorTargetPos = Vector2.ZERO
    var isCastingMeteor = false
    var meteorCastTimer = 0f

    var meleeComboStep = 0
    var meleeComboTimer = 0f
    var canDodgeRoll = config.canDodgeRoll
    var dodgeRollCooldown = config.dodgeRollCooldown
    var dodgeRollTimer = 0f
    var isDodging = false
    var dodgeRollDuration = 0f
    var dodgeRollDir = Vector2.ZERO

    var bossEnrageTimer = 0f
    var bossEnrageCooldown = 3f
    var bossSpecialReady = false

    var hurtTimer = 0f

    var surroundSlot = -1
    var slowTimer = 0f
    var freezeTimer = 0f
    var knockbackVx = 0f
    var knockbackVy = 0f

    fun applyKnockback(vx: Float, vy: Float) {
        knockbackVx += vx
        knockbackVy += vy
    }

    private fun updateKnockback(dt: Float) {
        if (knockbackVx != 0f || knockbackVy != 0f) {
            position.x += knockbackVx * dt
            position.y += knockbackVy * dt
            knockbackVx *= 0.85f
            knockbackVy *= 0.85f
            if (kotlin.math.abs(knockbackVx) < 1f) knockbackVx = 0f
            if (kotlin.math.abs(knockbackVy) < 1f) knockbackVy = 0f
        }
    }

    fun effectiveSpeed(base: Float): Float {
        if (freezeTimer > 0) return 0f
        if (slowTimer > 0) return base * 0.4f
        return base
    }

    init {
        position = Vector2(spawnPos.x, spawnPos.y)
    }

    fun takeDamage(amount: Float, game: Game) {
        if (isDead || isDodging || isPhaseTransitioning) return

        if (hasShield && !shieldThrown && !isShieldBashing) {
            val playerDir = if (game.player.position.x > position.x) 1 else -1
            if (playerDir == shieldDirection) {
                health -= (amount * 0.2f).toInt()
                val knockDir = if (position.x > game.player.position.x) 1f else -1f
                position.x += knockDir * 20f
                if (health <= 0) {
                    health = 0
                    isDead = true
                    deathAnimationTimer = deathAnimationDuration
                    stateMachine.transitionTo(EnemyState.DEAD)
                }
                return
            }
        }

        health -= amount.toInt()
        stateMachine.transitionTo(EnemyState.HURT)
        stateTimer = 0f
        hurtTimer = 0.2f

        for (i in 0..3) {
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2((Random.nextFloat() - 0.5f) * 80f, (Random.nextFloat() - 0.5f) * 80f),
                color = android.graphics.Color.WHITE,
                life = 0.3f,
                size = 2f
            ))
        }

        if (health <= 0) {
            health = 0
            isDead = true
            deathAnimationTimer = deathAnimationDuration
            stateMachine.transitionTo(EnemyState.DEAD)

            for (i in 0..8) {
                game.particles.add(Particle(
                    position = Vector2(position.x, position.y),
                    velocity = Vector2((Random.nextFloat() - 0.5f) * 120f, (Random.nextFloat() - 0.5f) * 120f),
                    color = android.graphics.Color.parseColor("#FF6644"),
                    life = 0.6f,
                    size = 4f
                ))
            }
        } else {
            checkPhaseTransition(game)
        }
    }

    private fun checkPhaseTransition(game: Game) {
        if (!isBoss || phaseThresholds.isEmpty()) return
        val hpRatio = health.toFloat() / maxHealth.toFloat()
        val nextPhase = phase + 1
        if (nextPhase <= phaseThresholds.size && hpRatio <= phaseThresholds[nextPhase - 1]) {
            enterNextPhase(game)
        }
    }

    private fun enterNextPhase(game: Game) {
        phase++
        isPhaseTransitioning = true
        phaseTransitionTimer = 1.0f
        for (i in 0..20) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val spd = 100f + Random.nextFloat() * 80f
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2(cos(angle) * spd, sin(angle) * spd),
                color = config.phaseTransitionColor,
                life = 0.8f,
                size = 6f
            ))
        }
        game.shake(10f, 0.3f)
        game.audioManager.play("boss_phase")
        behavior.enterNextPhase(this, phase)
    }

    private fun updateBossEnrage(dt: Float, game: Game) {
        if (!isBoss || phase < 1 || bossEnrageCooldown <= 0f) return
        bossEnrageTimer += dt
        if (bossEnrageTimer >= bossEnrageCooldown) {
            bossEnrageTimer = 0f
            behavior.executeBossEnrage(this, game)
        }
    }

    override fun update(dt: Float, game: Game) {
        if (isDead) {
            if (deathAnimationTimer > 0) {
                deathAnimationTimer -= dt
                if (deathAnimationTimer <= 0) {
                    deathAnimationTimer = 0f
                    deathAnimationDone = true
                }
            }
            return
        }

        if (isPhaseTransitioning) {
            phaseTransitionTimer -= dt
            if (phaseTransitionTimer <= 0f) {
                isPhaseTransitioning = false
            }
            return
        }

        stateMachine.update(dt)
        attackCooldownTimer -= dt
        if (attackCooldownTimer < 0) attackCooldownTimer = 0f

        if (canSummon) {
            summonTimer -= dt
        }
        if (leavesFireTrail) {
            fireTrailTimer -= dt
        }

        when (stateMachine.currentState) {
            EnemyState.CHASE, EnemyState.PATROL -> {
                walkBlend = minOf(1f, walkBlend + dt * 6f)
                moveAnimTime += dt
                moveAnimPhase = moveAnimTime * 4f
                idleTime = 0f
            }
            EnemyState.IDLE -> {
                walkBlend = maxOf(0f, walkBlend - dt * 5f)
                idleTime += dt
            }
            else -> {
                walkBlend = maxOf(0f, walkBlend - dt * 5f)
                idleTime = 0f
            }
        }

        if (hasShield && !shieldThrown) {
            shieldDirection = if (game.player.position.x > position.x) 1 else -1
            facingRight = shieldDirection > 0
        }

        behavior.updateTypeTimers(this, dt, game)

        updateBossEnrage(dt, game)

        updateKnockback(dt)

        val distToPlayer = position.distanceTo(game.player.position)
        val toPlayer = game.player.position - position

        if (stateMachine.currentState != EnemyState.HURT && stateMachine.currentState != EnemyState.PREPARE_ATTACK) {
            if (toPlayer.x > 5f) facingRight = true
            else if (toPlayer.x < -5f) facingRight = false
        }

        when (stateMachine.currentState) {
            EnemyState.IDLE -> updateIdle(dt, game, distToPlayer)
            EnemyState.PATROL -> updatePatrol(dt, game, distToPlayer)
            EnemyState.CHASE -> updateChase(dt, game, toPlayer, distToPlayer)
            EnemyState.PREPARE_ATTACK -> updatePrepareAttack(dt, game, toPlayer)
            EnemyState.ATTACK -> updateAttack(dt, game, toPlayer, distToPlayer)
            EnemyState.HURT -> updateHurt(dt, game)
            EnemyState.DEAD -> {}
        }
    }

    private fun updateIdle(dt: Float, game: Game, distToPlayer: Float) {
        stateTimer += dt
        if (distToPlayer < aggroRange) {
            stateMachine.transitionTo(EnemyState.CHASE)
            return
        }
        if (stateTimer > 2f) {
            stateTimer = 0f
            patrolTarget = Vector2(
                position.x + (Random.nextFloat() - 0.5f) * 100f,
                position.y + (Random.nextFloat() - 0.5f) * 100f
            )
            stateMachine.transitionTo(EnemyState.PATROL)
        }
    }

    private fun updatePatrol(dt: Float, game: Game, distToPlayer: Float) {
        if (distToPlayer < aggroRange) {
            stateMachine.transitionTo(EnemyState.CHASE)
            return
        }

        val dir = patrolTarget - position
        if (dir.magnitude > 5f) {
            val norm = dir.normalized
            position.x += norm.x * effectiveSpeed(speed) * 0.3f * dt
            position.y += norm.y * effectiveSpeed(speed) * 0.3f * dt
            if (abs(norm.x) > 0.3f) facingRight = norm.x > 0
        } else {
            stateMachine.transitionTo(EnemyState.IDLE)
        }
    }

    private fun updateChase(dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float) {
        val player = game.player

        if (distToPlayer > aggroRange * 1.5f) {
            stateMachine.transitionTo(EnemyState.IDLE)
            return
        }

        // Ranged non-boss: kiting behavior
        if (isRanged && !isBoss) {
            val preferredDist = attackRange * 0.6f
            val kitingDir: Vector2
            if (distToPlayer < preferredDist * 0.5f) {
                kitingDir = (position - player.position).normalized
                position.x += kitingDir.x * effectiveSpeed(speed) * 1.2f * dt
                position.y += kitingDir.y * effectiveSpeed(speed) * 1.2f * dt
                if (abs(kitingDir.x) > 0.15f) facingRight = kitingDir.x > 0
            } else if (distToPlayer > preferredDist * 1.3f) {
                kitingDir = toPlayer.normalized
                position.x += kitingDir.x * effectiveSpeed(speed) * 0.8f * dt
                position.y += kitingDir.y * effectiveSpeed(speed) * 0.8f * dt
                if (abs(kitingDir.x) > 0.15f) facingRight = kitingDir.x > 0
            } else {
                val strafeAngle = atan2(toPlayer.y, toPlayer.x) + Math.PI.toFloat() / 2f * (if (surroundSlot % 2 == 0) 1f else -1f)
                kitingDir = Vector2(cos(strafeAngle), sin(strafeAngle))
                position.x += kitingDir.x * effectiveSpeed(speed) * 0.4f * dt
                position.y += kitingDir.y * effectiveSpeed(speed) * 0.4f * dt
            }
            if (distToPlayer <= attackRange && attackCooldownTimer <= 0f) {
                startPrepareAttack(game, toPlayer)
            }
            return
        }

        // Type-specific chase behavior (retreat, phase shift, flame dash, shield bash, etc.)
        if (behavior.updateChase(this, dt, game, toPlayer, distToPlayer)) return

        // Continue flame dash
        if (isFlameDashing) {
            updateFlameDash(dt, game)
            return
        }

        // Continue shield bash
        if (isShieldBashing) {
            updateShieldBash(dt, game)
            return
        }

        // Dodge roll
        if (isDodging) {
            updateDodgeRoll(dt)
            return
        }

        // Boss meteor cast
        if (isCastingMeteor) {
            updateMeteorCast(dt, game)
            return
        }

        // Melee surround positioning
        val moveTarget: Vector2
        if (surroundSlot >= 0 && !isBoss) {
            val slotAngle = surroundSlot * Math.PI.toFloat() * 2f / 6f
            val surroundRadius = 45f
            moveTarget = Vector2(
                player.position.x + cos(slotAngle) * surroundRadius,
                player.position.y + sin(slotAngle) * surroundRadius
            )
        } else {
            moveTarget = player.position
        }

        val dir = (moveTarget - position).normalized
        position.x += dir.x * effectiveSpeed(speed) * dt
        position.y += dir.y * effectiveSpeed(speed) * dt
        if (abs(dir.x) > 0.15f) facingRight = dir.x > 0

        // Fire trail
        if (leavesFireTrail && fireTrailTimer <= 0) {
            fireTrailTimer = 0.3f
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2.ZERO,
                color = android.graphics.Color.parseColor("#FF6622"),
                life = 1.5f,
                size = 6f,
                damage = 3f,
                isFireTrail = true
            ))
        }

        // Boss special: charge
        if (canCharge && !isCharging && distToPlayer < 200f && distToPlayer > 80f && attackCooldownTimer <= 0) {
            isCharging = true
            chargeTimer = 0.6f
            chargeDirection = dir
        }

        if (isCharging) {
            chargeTimer -= dt
            position.x += chargeDirection.x * chargeSpeed * dt
            position.y += chargeDirection.y * chargeSpeed * dt
            if (chargeTimer <= 0) {
                isCharging = false
                attackCooldownTimer = attackCooldown
            }
            if (position.distanceTo(game.player.position) < 30f) {
                game.player.takeDamage(attackDamage * 2, game)
            }
        }

        if (distToPlayer <= attackRange && !isCharging) {
            if (attackCooldownTimer <= 0f) {
                startPrepareAttack(game, toPlayer)
            }
        }
    }

    private fun updatePrepareAttack(dt: Float, game: Game, toPlayer: Vector2) {
        prepareAttackTimer -= dt
        if (toPlayer.x > 5f) facingRight = true
        else if (toPlayer.x < -5f) facingRight = false

        if (prepareAttackTimer <= 0f) {
            stateMachine.transitionTo(EnemyState.ATTACK)
        }
    }

    fun startPrepareAttack(game: Game, toPlayer: Vector2, duration: Float = 0.3f) {
        prepareAttackTimer = duration
        stateMachine.transitionTo(EnemyState.PREPARE_ATTACK)
        if (toPlayer.x > 5f) facingRight = true
        else if (toPlayer.x < -5f) facingRight = false
    }

    private fun updateAttack(dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float) {
        stateTimer += dt

        if (stateTimer < dt + 0.001f) {
            behavior.attack(this, game, distToPlayer)
        }

        if (isGroundSlamming) {
            updateGroundSlam(dt, game)
            return
        }

        // Charge combo (Inferno Titan)
        if (canCharge && chargeComboCount > 0 && stateTimer >= 0.6f) {
            stateTimer = 0f
            chargeComboCount--
            val dir = (game.player.position - position).normalized
            position.x += dir.x * speed * 5f * 0.15f
            position.y += dir.y * speed * 5f * 0.15f
            dealDamageIfClose(game, attackDamage)
            if (chargeComboCount <= 0) {
                attackCooldownTimer = attackCooldown
                stateMachine.transitionTo(EnemyState.CHASE)
            }
            return
        }

        if (stateTimer >= 0.5f) {
            attackCooldownTimer = attackCooldown
            stateMachine.transitionTo(EnemyState.CHASE)
        }
    }

    private fun updateHurt(dt: Float, game: Game) {
        stateTimer += dt
        hurtTimer -= dt
        if (stateTimer > 0.3f) {
            stateMachine.transitionTo(EnemyState.CHASE)
        }
    }

    fun dealDamageIfClose(game: Game, damage: Int, range: Float = attackRange.toFloat()) {
        val dist = position.distanceTo(game.player.position)
        if (dist < range) {
            game.player.takeDamage(damage, game)
        }
    }

    // ========================
    // Shared special ability methods (called by behaviors)
    // ========================

    fun startFlameDash(toPlayer: Vector2) {
        isFlameDashing = true
        flameDashDuration = 0.25f
        flameDashDir = toPlayer.normalized
        flameDashTimer = flameDashCooldown
    }

    private fun updateFlameDash(dt: Float, game: Game) {
        flameDashDuration -= dt
        position.x += flameDashDir.x * 600f * dt
        position.y += flameDashDir.y * 600f * dt
        game.particles.add(Particle(
            position = Vector2(position.x + (Random.nextFloat() - 0.5f) * 8f, position.y + (Random.nextFloat() - 0.5f) * 8f),
            velocity = Vector2((Random.nextFloat() - 0.5f) * 20f, -30f),
            color = android.graphics.Color.parseColor("#FF6600"),
            life = 1.5f,
            size = 8f,
            damage = 5f,
            isFireTrail = true
        ))
        val dist = position.distanceTo(game.player.position)
        if (dist < 30f) {
            game.player.takeDamage(8, game)
        }
        if (flameDashDuration <= 0f) {
            isFlameDashing = false
        }
    }

    fun dropLavaPool(game: Game) {
        for (i in 0..3) {
            val offsetX = (Random.nextFloat() - 0.5f) * 12f
            val offsetY = (Random.nextFloat() - 0.5f) * 12f
            game.particles.add(Particle(
                position = Vector2(position.x + offsetX, position.y + offsetY),
                velocity = Vector2((Random.nextFloat() - 0.5f) * 10f, -15f),
                color = android.graphics.Color.parseColor("#FF4400"),
                life = 3f,
                size = 10f,
                damage = 2f,
                isFireTrail = true
            ))
        }
    }

    fun startShieldBash(toPlayer: Vector2) {
        isShieldBashing = true
        shieldBashDuration = 0.2f
        shieldBashDir = toPlayer.normalized
        shieldBashTimer = shieldBashCooldown
    }

    private fun updateShieldBash(dt: Float, game: Game) {
        shieldBashDuration -= dt
        position.x += shieldBashDir.x * 400f * dt
        position.y += shieldBashDir.y * 400f * dt
        val dist = position.distanceTo(game.player.position)
        if (dist < 40f) {
            game.player.takeDamage(15, game)
            val knockDir = (game.player.position - position).normalized
            game.player.position.x += knockDir.x * 50f
            game.player.position.y += knockDir.y * 50f
            isShieldBashing = false
            shieldBashDuration = 0f
        }
        if (shieldBashDuration <= 0f) {
            isShieldBashing = false
        }
    }

    fun startGroundSlam() {
        isGroundSlamming = true
        groundSlamPhase = 0
        groundSlamHoverTimer = 0.5f
        groundSlamTimer = groundSlamCooldown
    }

    private fun updateGroundSlam(dt: Float, game: Game) {
        when (groundSlamPhase) {
            0 -> {
                groundSlamHoverTimer -= dt
                if (groundSlamHoverTimer <= 0f) {
                    groundSlamPhase = 2
                }
            }
            2 -> {
                val slamRadius = if (phase >= 2) 100f else 80f
                val slamDamage = 20
                val dist = position.distanceTo(game.player.position)
                if (dist < slamRadius) {
                    game.player.takeDamage(slamDamage, game)
                }
                for (i in 0..12) {
                    val angle = i * Math.PI.toFloat() * 2f / 12f
                    val spd = 120f
                    game.particles.add(Particle(
                        position = Vector2(position.x, position.y),
                        velocity = Vector2(cos(angle) * spd, sin(angle) * spd),
                        color = android.graphics.Color.parseColor("#AAAA44"),
                        life = 0.5f,
                        size = 6f
                    ))
                }
                game.shake(8f, 0.15f)
                isGroundSlamming = false
                groundSlamPhase = 0
                attackCooldownTimer = attackCooldown
                stateMachine.transitionTo(EnemyState.CHASE)
            }
        }
    }

    fun summonMinions(game: Game) {
        for (i in 0 until summonCount) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val dist = 60f + Random.nextFloat() * 40f
            val spawnX = position.x + cos(angle) * dist
            val spawnY = position.y + sin(angle) * dist
            val minion = Enemy(EnemyType.SKELETON, Vector2(spawnX, spawnY), layerIndex)
            game.enemies.add(minion)
        }
        for (i in 0..8) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2(cos(angle) * 60f, sin(angle) * 60f),
                color = android.graphics.Color.parseColor("#44FF44"),
                life = 0.4f,
                size = 5f
            ))
        }
    }

    fun startMeteorCast(game: Game) {
        isCastingMeteor = true
        meteorCastTimer = 1.2f
        meteorTargetPos = Vector2(game.player.position.x, game.player.position.y)
        meteorTimer = meteorCooldown
    }

    private fun updateMeteorCast(dt: Float, game: Game) {
        meteorCastTimer -= dt
        if (meteorCastTimer > 0f) {
            game.particles.add(Particle(
                position = Vector2(meteorTargetPos.x, meteorTargetPos.y),
                velocity = Vector2.ZERO,
                color = android.graphics.Color.parseColor("#FF6600"),
                life = 0.1f,
                size = 25f + (1.2f - meteorCastTimer) * 10f
            ))
        }
        if (meteorCastTimer <= 0f) {
            val dist = game.player.position.distanceTo(meteorTargetPos)
            if (dist < 60f) {
                game.player.takeDamage(25, game)
            }
            for (i in 0..15) {
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
                val spd = 80f + Random.nextFloat() * 60f
                game.particles.add(Particle(
                    position = Vector2(meteorTargetPos.x, meteorTargetPos.y),
                    velocity = Vector2(cos(angle) * spd, sin(angle) * spd),
                    color = android.graphics.Color.parseColor("#FF4400"),
                    life = 0.6f,
                    size = 7f,
                    damage = 3f,
                    isFireTrail = true
                ))
            }
            game.shake(10f, 0.2f)
            isCastingMeteor = false
        }
    }

    private fun updateDodgeRoll(dt: Float) {
        dodgeRollDuration -= dt
        position.x += dodgeRollDir.x * speed * 2f * dt
        position.y += dodgeRollDir.y * speed * 2f * dt
        if (dodgeRollDuration <= 0f) {
            isDodging = false
        }
    }

    fun fireSpear(game: Game, dir: Vector2) {
        game.projectiles.add(Projectile(
            position = Vector2(position.x + dir.x * 15f, position.y + dir.y * 15f),
            velocity = dir * projectileSpeed,
            damage = attackDamage.toFloat(),
            type = ProjectileType.SPEAR,
            maxRange = 400f,
            isEnemyProjectile = true,
            angle = dir.angle
        ))
    }

    override fun render(canvas: Canvas, renderer: IsometricRenderer) {
        if (deathAnimationDone) return
        renderer.renderEnemy(canvas, this)
    }
}
