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
    get() = when (this) {
        EnemyType.MEGA_SKELETON -> "冥骨巨灵"
        EnemyType.INFERNO_TITAN -> "炼狱泰坦"
        EnemyType.CHAMPION -> "永恒冠军"
        else -> ""
    }

val EnemyType.bossTitle: String
    get() = when (this) {
        EnemyType.MEGA_SKELETON -> "塔耳塔洛斯之主"
        EnemyType.INFERNO_TITAN -> "阿斯福德的烈焰"
        EnemyType.CHAMPION -> "伊利西昂的荣光"
        else -> ""
    }

class Enemy(
    val type: EnemyType,
    spawnPos: Vector2,
    val layerIndex: Int = 0,
    val isBoss: Boolean = false
) : Entity() {

    override var width = 16f
    override var height = 32f
    var maxHealth = 30
    var health = 30
    var speed = 80f
    var attackDamage = 5
    var attackRange = 40f
    var attackCooldown = 1.5f
    var attackCooldownTimer = 0f
    var aggroRange = 250f
    var goldDrop = 10
    var name = ""
    var facingRight = true
    var isDead = false
    var deathAnimationDone = false

    var deathAnimationTimer = 0f
    val deathAnimationDuration = 0.6f

    // Shield bearer specific
    var hasShield = false
    var shieldDirection = 1 // 1 = right, -1 = left

    // Ranged attack
    var isRanged = false
    var projectileSpeed = 200f
    var projectileType = ProjectileType.MAGIC_BOLT

    // Phase (for bosses)
    var phase = 0
    var phaseThreshold = 0.5f
    var phaseThresholds = floatArrayOf()
    var isPhaseTransitioning = false
    var phaseTransitionTimer = 0f

    // Summon (for mega skeleton)
    var canSummon = false
    var summonCooldown = 5f
    var summonTimer = 0f
    var summonCount = 2

    // Fire trail (for flame dancer)
    var leavesFireTrail = false
    var fireTrailTimer = 0f

    // Charge (for inferno titan)
    var canCharge = false
    var isCharging = false
    var chargeTimer = 0f
    var chargeDirection = Vector2.ZERO
    var chargeSpeed = 400f

    val stateMachine = StateMachine(EnemyState.IDLE)
    private var patrolTarget = Vector2.ZERO
    var stateTimer = 0f

    // Animation state
    private var moveAnimTime = 0f
    var moveAnimPhase = 0f
    var idleTime = 0f
    var walkBlend = 0f

    // --- New attack pattern fields ---

    // PREPARE_ATTACK wind-up
    private var prepareAttackTimer = 0f

    // Skeleton combo
    var comboCount = 0
    var comboTimer = 0f

    // Wraith phase shift
    var canPhaseShift = false
    var phaseShiftCooldown = 4f
    var phaseShiftTimer = 0f

    // Flame dancer dash
    var canFlameDash = false
    var flameDashCooldown = 2.5f
    var flameDashTimer = 0f
    var isFlameDashing = false
    var flameDashDuration = 0f
    var flameDashDir = Vector2.ZERO

    // Lava caster
    var castCount = 0
    var lavaPoolTimer = 5f

    // Shield bearer bash
    var shieldBashCooldown = 3f
    var shieldBashTimer = 0f
    var isShieldBashing = false
    var shieldBashDuration = 0f
    var shieldBashDir = Vector2.ZERO
    var shieldThrown = false

    // Spear thrower
    var multiSpearCount = 0
    var multiSpearTimer = 0f
    var isRetreating = false

    // Mega Skeleton ground slam
    var canGroundSlam = false
    var groundSlamCooldown = 3f
    var groundSlamTimer = 0f
    var isGroundSlamming = false
    var groundSlamPhase = 0 // 0=jumping up, 2=landing
    var groundSlamHoverTimer = 0f

    // Inferno Titan charge combo + meteor
    var chargeComboCount = 0
    var chargeComboMax = 1
    var chargePauseTimer = 0f
    var meteorCooldown = 8f
    var meteorTimer = 0f
    var canMeteor = false
    var meteorTargetPos = Vector2.ZERO
    var isCastingMeteor = false
    var meteorCastTimer = 0f

    // Champion melee combo + dodge
    var meleeComboStep = 0
    var meleeComboTimer = 0f
    var canDodgeRoll = false
    var dodgeRollCooldown = 8f
    var dodgeRollTimer = 0f
    var isDodging = false
    var dodgeRollDuration = 0f
    var dodgeRollDir = Vector2.ZERO

    // Boss enrage: periodic special attacks in later phases
    var bossEnrageTimer = 0f
    var bossEnrageCooldown = 3f
    var bossSpecialReady = false

    // Hurt
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

    /** Get effective speed considering slow/freeze */
    fun effectiveSpeed(base: Float): Float {
        if (freezeTimer > 0) return 0f
        if (slowTimer > 0) return base * 0.4f
        return base
    }

    init {
        position = Vector2(spawnPos.x, spawnPos.y)
        configure()
    }

    private fun configure() {
        when (type) {
            EnemyType.SKELETON -> {
                maxHealth = 30; health = 30; speed = 80f; attackDamage = 8
                attackRange = 40f; attackCooldown = 1.2f; goldDrop = 8
                name = "骷髅兵"
            }
            EnemyType.WRAITH -> {
                maxHealth = 20; health = 20; speed = 60f; attackDamage = 6
                isRanged = true; projectileSpeed = 180f; projectileType = ProjectileType.MAGIC_BOLT
                attackRange = 200f; attackCooldown = 2f; goldDrop = 12
                name = "幽灵"
                canPhaseShift = true; phaseShiftCooldown = 4f; phaseShiftTimer = 0f
            }
            EnemyType.MEGA_SKELETON -> {
                maxHealth = 200; health = 200; speed = 60f; attackDamage = 15
                attackRange = 50f; attackCooldown = 1.5f; goldDrop = 100
                canSummon = true; width = 32f; height = 60f
                name = "巨型骷髅"
                phaseThresholds = floatArrayOf(0.6f, 0.3f)
                groundSlamCooldown = 3f; groundSlamTimer = 0f
                summonCount = 2
            }
            EnemyType.FLAME_DANCER -> {
                maxHealth = 40; health = 40; speed = 140f; attackDamage = 10
                attackRange = 35f; attackCooldown = 0.8f; goldDrop = 15
                leavesFireTrail = true
                name = "火焰舞者"
                canFlameDash = true; flameDashCooldown = 2.5f; flameDashTimer = 0f
            }
            EnemyType.LAVA_CASTER -> {
                maxHealth = 35; health = 35; speed = 50f; attackDamage = 12
                isRanged = true; projectileSpeed = 200f; projectileType = ProjectileType.FIREBALL
                attackRange = 250f; attackCooldown = 1.8f; goldDrop = 18
                name = "熔岩术士"
                castCount = 0; lavaPoolTimer = 5f
            }
            EnemyType.INFERNO_TITAN -> {
                maxHealth = 350; health = 350; speed = 50f; attackDamage = 20
                isRanged = true; projectileSpeed = 160f; projectileType = ProjectileType.FIREBALL
                attackRange = 200f; attackCooldown = 2f; goldDrop = 150
                canCharge = true; width = 40f; height = 70f
                name = "炼狱泰坦"
                phaseThresholds = floatArrayOf(0.5f)
                chargeComboCount = 0; chargeComboMax = 3
                meteorCooldown = 8f; meteorTimer = 0f; canMeteor = false
            }
            EnemyType.SHIELD_BEARER -> {
                maxHealth = 60; health = 60; speed = 70f; attackDamage = 10
                attackRange = 40f; attackCooldown = 1.5f; goldDrop = 20
                hasShield = true
                name = "持盾守卫"
                shieldBashCooldown = 3f; shieldBashTimer = 0f; shieldThrown = false
            }
            EnemyType.SPEAR_THROWER -> {
                maxHealth = 45; health = 45; speed = 60f; attackDamage = 14
                isRanged = true; projectileSpeed = 250f; projectileType = ProjectileType.SPEAR
                attackRange = 300f; attackCooldown = 2f; goldDrop = 22
                name = "投矛手"
                multiSpearCount = 0; multiSpearTimer = 0f
            }
            EnemyType.CHAMPION -> {
                maxHealth = 500; health = 500; speed = 70f; attackDamage = 18
                isRanged = true; projectileSpeed = 220f; projectileType = ProjectileType.SPEAR
                attackRange = 150f; attackCooldown = 1.5f; goldDrop = 200
                hasShield = true; width = 32f; height = 60f
                name = "冠军勇士"
                phaseThresholds = floatArrayOf(0.5f)
                meleeComboStep = 0; meleeComboTimer = 0f
                canDodgeRoll = false; dodgeRollCooldown = 8f; dodgeRollTimer = 0f
            }
        }
    }

    fun takeDamage(amount: Float, game: Game) {
        if (isDead || isDodging || isPhaseTransitioning) return

        // Shield check
        if (hasShield && !shieldThrown && !isShieldBashing) {
            val playerDir = if (game.player.position.x > position.x) 1 else -1
            if (playerDir == shieldDirection) {
                // Blocked! Reduce damage by 80%
                health -= (amount * 0.2f).toInt()
                // Knockback on block
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

        // Hit particles
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

            // Death particles
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
        phaseTransitionTimer = 1.0f // Longer transition for dramatic effect
        // Burst particles
        for (i in 0..20) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val spd = 100f + Random.nextFloat() * 80f
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2(cos(angle) * spd, sin(angle) * spd),
                color = when (type) {
                    EnemyType.MEGA_SKELETON -> android.graphics.Color.parseColor("#66FF66")
                    EnemyType.INFERNO_TITAN -> android.graphics.Color.parseColor("#FF6633")
                    EnemyType.CHAMPION -> android.graphics.Color.parseColor("#6699FF")
                    else -> android.graphics.Color.WHITE
                },
                life = 0.8f,
                size = 6f
            ))
        }
        game.shake(10f, 0.3f)
        game.audioManager.play("boss_phase")
        // Type-specific phase changes with new mechanics
        when (type) {
            EnemyType.MEGA_SKELETON -> {
                when (phase) {
                    1 -> {
                        canGroundSlam = true; summonCount = 3; summonCooldown = 4f; speed *= 1.3f
                        attackCooldown *= 0.8f // Faster attacks
                    }
                    2 -> {
                        attackDamage = (attackDamage * 1.3f).toInt(); summonCount = 4; summonCooldown = 3f
                        bossEnrageCooldown = 4f // Periodic ground slam enrage
                    }
                }
            }
            EnemyType.INFERNO_TITAN -> {
                when (phase) {
                    1 -> {
                        canMeteor = true; speed *= 1.3f; chargeComboMax = 2; meteorCooldown = 5f
                        attackCooldown *= 0.75f // Faster attacks
                        bossEnrageCooldown = 5f // Periodic meteor barrage
                    }
                }
            }
            EnemyType.CHAMPION -> {
                when (phase) {
                    1 -> {
                        canDodgeRoll = true; speed *= 1.4f; shieldThrown = true
                        attackDamage = (attackDamage * 1.2f).toInt()
                        dodgeRollCooldown = 4f // Dodge more frequently
                        bossEnrageCooldown = 3f // Periodic spear barrage
                    }
                }
            }
            else -> {}
        }
    }

    /** Boss enrage: execute a special attack on timer when in later phases */
    private fun updateBossEnrage(dt: Float, game: Game) {
        if (!isBoss || phase < 1 || bossEnrageCooldown <= 0f) return
        bossEnrageTimer += dt
        if (bossEnrageTimer >= bossEnrageCooldown) {
            bossEnrageTimer = 0f
            executeBossEnrageAttack(game)
        }
    }

    private fun executeBossEnrageAttack(game: Game) {
        when (type) {
            EnemyType.MEGA_SKELETON -> {
                // Enrage: automatic ground slam without entering ATTACK state
                if (canGroundSlam && groundSlamTimer <= 0f) {
                    startGroundSlam()
                } else if (canSummon && summonTimer <= 0f) {
                    summonMinions(game)
                    summonTimer = summonCooldown
                }
            }
            EnemyType.INFERNO_TITAN -> {
                // Enrage: fire 8 fireballs in all directions
                for (i in 0..7) {
                    val angle = i * Math.PI.toFloat() / 4f
                    val projDir = Vector2(cos(angle), sin(angle))
                    game.projectiles.add(Projectile(
                        position = Vector2(position.x + projDir.x * 20f, position.y + projDir.y * 20f),
                        velocity = projDir * projectileSpeed * 0.7f,
                        damage = attackDamage.toFloat() * 0.5f,
                        type = ProjectileType.FIREBALL,
                        maxRange = 350f,
                        isEnemyProjectile = true,
                        angle = projDir.angle
                    ))
                }
                game.shake(4f, 0.1f)
            }
            EnemyType.CHAMPION -> {
                // Enrage: rapid triple spear fan
                val dir = (game.player.position - position).normalized
                val baseAngle = atan2(dir.y, dir.x)
                for (i in -1..1) {
                    val angle = baseAngle + i * 0.3f
                    val projDir = Vector2(cos(angle), sin(angle))
                    game.projectiles.add(Projectile(
                        position = Vector2(position.x + projDir.x * 15f, position.y + projDir.y * 15f),
                        velocity = projDir * projectileSpeed,
                        damage = attackDamage.toFloat() * 0.6f,
                        type = ProjectileType.SPEAR,
                        maxRange = 400f,
                        isEnemyProjectile = true,
                        angle = projDir.angle
                    ))
                }
            }
            else -> {}
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

        // Phase transition (invincible pause)
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

        // Update animation - smooth transitions
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

        // Update shield direction
        if (hasShield && !shieldThrown) {
            shieldDirection = if (game.player.position.x > position.x) 1 else -1
            facingRight = shieldDirection > 0
        }

        // Type-specific continuous timers
        updateTypeTimers(dt, game)

        // Boss enrage timer
        updateBossEnrage(dt, game)

        // Apply knockback
        updateKnockback(dt)

        val distToPlayer = position.distanceTo(game.player.position)
        val toPlayer = game.player.position - position

        // Update facing (except during hurt/prepare_attack)
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

    private fun updateTypeTimers(dt: Float, game: Game) {
        // Wraith phase shift cooldown
        if (canPhaseShift && phaseShiftTimer > 0f) phaseShiftTimer -= dt
        // Flame dash cooldown
        if (canFlameDash && flameDashTimer > 0f) flameDashTimer -= dt
        // Shield bash cooldown
        if (type == EnemyType.SHIELD_BEARER && shieldBashTimer > 0f) shieldBashTimer -= dt
        // Dodge roll cooldown
        if (canDodgeRoll && dodgeRollTimer > 0f) dodgeRollTimer -= dt
        // Meteor cooldown
        if (canMeteor && meteorTimer > 0f) meteorTimer -= dt
        // Ground slam cooldown
        if (canGroundSlam && groundSlamTimer > 0f) groundSlamTimer -= dt
        // Combo timeout (skeleton)
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) comboCount = 0
        }
        // Lava pool timer
        if (type == EnemyType.LAVA_CASTER && lavaPoolTimer > 0f) {
            lavaPoolTimer -= dt
            if (lavaPoolTimer <= 0f) {
                dropLavaPool(game)
                lavaPoolTimer = 5f
            }
        }
        // Multi-spear timer
        if (multiSpearCount > 0 && multiSpearTimer > 0f) {
            multiSpearTimer -= dt
            if (multiSpearTimer <= 0f && multiSpearCount > 0) {
                val dir = (game.player.position - position).normalized
                fireSpear(game, dir)
                multiSpearCount--
                if (multiSpearCount > 0) multiSpearTimer = 0.15f
            }
        }
    }

    // ========================
    // State update methods
    // ========================

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

        // === Ranged enemies: maintain preferred distance (kiting) ===
        if (isRanged && !isBoss) {
            val preferredDist = attackRange * 0.6f
            val kitingDir: Vector2
            if (distToPlayer < preferredDist * 0.5f) {
                // Too close: retreat away from player
                kitingDir = (position - player.position).normalized
                position.x += kitingDir.x * effectiveSpeed(speed) * 1.2f * dt
                position.y += kitingDir.y * effectiveSpeed(speed) * 1.2f * dt
                if (abs(kitingDir.x) > 0.15f) facingRight = kitingDir.x > 0
            } else if (distToPlayer > preferredDist * 1.3f) {
                // Too far: approach but stop at preferred distance
                kitingDir = toPlayer.normalized
                position.x += kitingDir.x * effectiveSpeed(speed) * 0.8f * dt
                position.y += kitingDir.y * effectiveSpeed(speed) * 0.8f * dt
                if (abs(kitingDir.x) > 0.15f) facingRight = kitingDir.x > 0
            } else {
                // At preferred distance: strafe sideways
                val strafeAngle = atan2(toPlayer.y, toPlayer.x) + Math.PI.toFloat() / 2f * (if (surroundSlot % 2 == 0) 1f else -1f)
                kitingDir = Vector2(cos(strafeAngle), sin(strafeAngle))
                position.x += kitingDir.x * effectiveSpeed(speed) * 0.4f * dt
                position.y += kitingDir.y * effectiveSpeed(speed) * 0.4f * dt
            }
            // Ranged enemies can attack while kiting
            if (distToPlayer <= attackRange && attackCooldownTimer <= 0f) {
                startPrepareAttack(game, toPlayer)
            }
            return
        }

        // Retreat behavior for spear throwers (boss Champion ranged mode)
        if (type == EnemyType.SPEAR_THROWER && distToPlayer < 80f) {
            isRetreating = true
            val awayDir = (position - player.position).normalized
            position.x += awayDir.x * speed * 0.6f * dt
            position.y += awayDir.y * speed * 0.6f * dt
            // Still attack while retreating
            if (distToPlayer < attackRange && attackCooldownTimer <= 0f) {
                startPrepareAttack(game, toPlayer)
            }
            return
        }
        isRetreating = false

        // Wraith: phase shift when player gets too close
        if (type == EnemyType.WRAITH && canPhaseShift && distToPlayer < 60f && phaseShiftTimer <= 0f) {
            performPhaseShift(game)
            return
        }

        // Flame dancer: flame dash at medium range
        if (type == EnemyType.FLAME_DANCER && canFlameDash && !isFlameDashing &&
            distToPlayer >= 80f && distToPlayer <= 120f && flameDashTimer <= 0f) {
            startFlameDash(toPlayer)
            return
        }

        // Continue flame dash
        if (isFlameDashing) {
            updateFlameDash(dt, game)
            return
        }

        // Shield bearer: shield bash when close
        if (type == EnemyType.SHIELD_BEARER && !shieldThrown && distToPlayer < 45f && shieldBashTimer <= 0f) {
            startShieldBash(toPlayer)
            return
        }

        // Continue shield bash
        if (isShieldBashing) {
            updateShieldBash(dt, game)
            return
        }

        // Dodge roll (Champion)
        if (isDodging) {
            updateDodgeRoll(dt)
            return
        }

        // Boss specials during chase
        if (isBoss) {
            // Mega Skeleton: summon minions
            if (type == EnemyType.MEGA_SKELETON && canSummon && summonTimer <= 0f) {
                summonMinions(game)
                summonTimer = summonCooldown
            }
            // Inferno Titan: meteor
            if (type == EnemyType.INFERNO_TITAN && canMeteor && meteorTimer <= 0f && distToPlayer > 60f) {
                startMeteorCast(game)
                return
            }
            // Continue meteor cast
            if (isCastingMeteor) {
                updateMeteorCast(dt, game)
                return
            }
        }

        // === Melee enemies: surround positioning ===
        val moveTarget: Vector2
        if (surroundSlot >= 0 && !isBoss) {
            // Move toward assigned surround slot position instead of directly at player
            val slotAngle = surroundSlot * Math.PI.toFloat() * 2f / 6f // max 6 slots
            val surroundRadius = 45f // orbit radius around player
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
            // Damage player on contact
            if (position.distanceTo(game.player.position) < 30f) {
                game.player.takeDamage(attackDamage * 2, game)
            }
        }

        // Enter attack range
        if (distToPlayer <= attackRange && !isCharging) {
            if (attackCooldownTimer <= 0f) {
                startPrepareAttack(game, toPlayer)
            }
        }
    }

    private fun updatePrepareAttack(dt: Float, game: Game, toPlayer: Vector2) {
        prepareAttackTimer -= dt
        // Face player during wind-up
        if (toPlayer.x > 5f) facingRight = true
        else if (toPlayer.x < -5f) facingRight = false

        if (prepareAttackTimer <= 0f) {
            stateMachine.transitionTo(EnemyState.ATTACK)
        }
    }

    private fun startPrepareAttack(game: Game, toPlayer: Vector2, duration: Float = 0.3f) {
        prepareAttackTimer = duration
        stateMachine.transitionTo(EnemyState.PREPARE_ATTACK)
        if (toPlayer.x > 5f) facingRight = true
        else if (toPlayer.x < -5f) facingRight = false
    }

    private fun updateAttack(dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float) {
        stateTimer += dt

        // Execute attack at the start
        if (stateTimer < dt + 0.001f) {
            when (type) {
                EnemyType.SKELETON -> skeletonAttack(game)
                EnemyType.WRAITH -> wraithAttack(game)
                EnemyType.FLAME_DANCER -> flameDancerAttack(game)
                EnemyType.LAVA_CASTER -> lavaCasterAttack(game)
                EnemyType.SHIELD_BEARER -> shieldBearerAttack(game)
                EnemyType.SPEAR_THROWER -> spearThrowerAttack(game)
                EnemyType.MEGA_SKELETON -> megaSkeletonAttack(game, distToPlayer)
                EnemyType.INFERNO_TITAN -> infernoTitanAttack(game, distToPlayer)
                EnemyType.CHAMPION -> championAttack(game, distToPlayer)
            }
        }

        // Ground slam has its own timing
        if (isGroundSlamming) {
            updateGroundSlam(dt, game)
            return
        }

        // Charge combo (Inferno Titan)
        if (type == EnemyType.INFERNO_TITAN && chargeComboCount > 0 && stateTimer >= 0.6f) {
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

        // Attack duration
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

    // ========================
    // Per-type attack methods
    // ========================

    private fun skeletonAttack(game: Game) {
        val comboDmg = when (comboCount) {
            0 -> attackDamage
            1 -> (attackDamage * 1.5f).toInt()
            else -> (attackDamage * 1.875f).toInt()
        }
        val range = if (comboCount >= 2) 50f else attackRange
        dealDamageIfClose(game, comboDmg, range)
        comboCount = (comboCount + 1) % 3
        comboTimer = 3f
    }

    private fun wraithAttack(game: Game) {
        val dir = (game.player.position - position).normalized
        val baseAngle = atan2(dir.y, dir.x)
        // Fire 3 magic bolts in spread pattern
        for (i in -1..1) {
            val angle = baseAngle + i * 0.2f
            val projDir = Vector2(cos(angle), sin(angle))
            game.projectiles.add(Projectile(
                position = Vector2(position.x + projDir.x * 15f, position.y + projDir.y * 15f),
                velocity = projDir * projectileSpeed,
                damage = attackDamage.toFloat(),
                type = projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = projDir.angle
            ))
        }
    }

    private fun flameDancerAttack(game: Game) {
        dealDamageIfClose(game, attackDamage)
    }

    private fun lavaCasterAttack(game: Game) {
        val dir = (game.player.position - position).normalized
        val baseAngle = atan2(dir.y, dir.x)
        castCount++
        if (castCount % 3 == 0) {
            // Triple fireball fan
            for (i in -1..1) {
                val angle = baseAngle + i * 0.26f
                val projDir = Vector2(cos(angle), sin(angle))
                game.projectiles.add(Projectile(
                    position = Vector2(position.x + projDir.x * 15f, position.y + projDir.y * 15f),
                    velocity = projDir * projectileSpeed,
                    damage = attackDamage.toFloat(),
                    type = projectileType,
                    maxRange = 400f,
                    isEnemyProjectile = true,
                    angle = projDir.angle
                ))
            }
        } else {
            // Single fireball
            game.projectiles.add(Projectile(
                position = Vector2(position.x + dir.x * 15f, position.y + dir.y * 15f),
                velocity = dir * projectileSpeed,
                damage = attackDamage.toFloat(),
                type = projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = dir.angle
            ))
        }
    }

    private fun shieldBearerAttack(game: Game) {
        dealDamageIfClose(game, attackDamage)
    }

    private fun spearThrowerAttack(game: Game) {
        val dir = (game.player.position - position).normalized
        fireSpear(game, dir)
        // Multi-spear: fire a second spear after delay
        multiSpearCount = 1
        multiSpearTimer = 0.15f
    }

    private fun fireSpear(game: Game, dir: Vector2) {
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

    private fun megaSkeletonAttack(game: Game, distToPlayer: Float) {
        if (canGroundSlam && groundSlamTimer <= 0f && distToPlayer < 120f) {
            startGroundSlam()
            return
        }
        dealDamageIfClose(game, attackDamage)
    }

    private fun infernoTitanAttack(game: Game, distToPlayer: Float) {
        // Melee + charge combo
        if (distToPlayer < attackRange) {
            dealDamageIfClose(game, attackDamage)
            if (phase == 0 && chargeComboCount <= 0) {
                chargeComboCount = chargeComboMax - 1
            }
        } else if (isRanged) {
            // Fire fireball at range
            val dir = (game.player.position - position).normalized
            game.projectiles.add(Projectile(
                position = Vector2(position.x + dir.x * 15f, position.y + dir.y * 15f),
                velocity = dir * projectileSpeed,
                damage = attackDamage.toFloat(),
                type = projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = dir.angle
            ))
        }
    }

    private fun championAttack(game: Game, distToPlayer: Float) {
        if (phase == 1 && distToPlayer < 60f) {
            // Melee combo in phase 2
            meleeComboStep++
            val comboDmg = (attackDamage * (1f + meleeComboStep * 0.2f)).toInt()
            dealDamageIfClose(game, comboDmg, 55f)
            if (meleeComboStep >= 3) {
                meleeComboStep = 0
            }
            return
        }
        // Default: throw spear at range, melee close
        if (distToPlayer > 60f && isRanged) {
            val dir = (game.player.position - position).normalized
            fireSpear(game, dir)
        } else {
            dealDamageIfClose(game, attackDamage)
        }
    }

    // ========================
    // Special ability methods
    // ========================

    private fun performPhaseShift(game: Game) {
        // Teleport to random position 100-200f away from player
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val dist = 100f + Random.nextFloat() * 100f
        val targetX = game.player.position.x + cos(angle) * dist
        val targetY = game.player.position.y + sin(angle) * dist
        // Spawn vanish particles at old position
        for (i in 0..5) {
            val a = Random.nextFloat() * Math.PI.toFloat() * 2f
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2(cos(a) * 40f, sin(a) * 40f),
                color = android.graphics.Color.parseColor("#8844CC"),
                life = 0.4f,
                size = 4f
            ))
        }
        position.x = targetX
        position.y = targetY
        // Spawn appear particles at new position
        for (i in 0..5) {
            val a = Random.nextFloat() * Math.PI.toFloat() * 2f
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2(cos(a) * 40f, sin(a) * 40f),
                color = android.graphics.Color.parseColor("#AA66EE"),
                life = 0.4f,
                size = 4f
            ))
        }
        phaseShiftTimer = phaseShiftCooldown
        // Immediately fire burst of bolts after teleport
        val dir = (game.player.position - position).normalized
        val baseAngle = atan2(dir.y, dir.x)
        for (i in -1..1) {
            val a = baseAngle + i * 0.2f
            val projDir = Vector2(cos(a), sin(a))
            game.projectiles.add(Projectile(
                position = Vector2(position.x + projDir.x * 15f, position.y + projDir.y * 15f),
                velocity = projDir * projectileSpeed,
                damage = attackDamage.toFloat(),
                type = projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = projDir.angle
            ))
        }
    }

    private fun startFlameDash(toPlayer: Vector2) {
        isFlameDashing = true
        flameDashDuration = 0.25f
        flameDashDir = toPlayer.normalized
        flameDashTimer = flameDashCooldown
    }

    private fun updateFlameDash(dt: Float, game: Game) {
        flameDashDuration -= dt
        position.x += flameDashDir.x * 600f * dt
        position.y += flameDashDir.y * 600f * dt
        // Leave intense fire trail
        game.particles.add(Particle(
            position = Vector2(position.x + (Random.nextFloat() - 0.5f) * 8f, position.y + (Random.nextFloat() - 0.5f) * 8f),
            velocity = Vector2((Random.nextFloat() - 0.5f) * 20f, -30f),
            color = android.graphics.Color.parseColor("#FF6600"),
            life = 1.5f,
            size = 8f,
            damage = 5f,
            isFireTrail = true
        ))
        // Damage player if close during dash
        val dist = position.distanceTo(game.player.position)
        if (dist < 30f) {
            game.player.takeDamage(8, game)
        }
        if (flameDashDuration <= 0f) {
            isFlameDashing = false
        }
    }

    private fun dropLavaPool(game: Game) {
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

    private fun startShieldBash(toPlayer: Vector2) {
        isShieldBashing = true
        shieldBashDuration = 0.2f
        shieldBashDir = toPlayer.normalized
        shieldBashTimer = shieldBashCooldown
    }

    private fun updateShieldBash(dt: Float, game: Game) {
        shieldBashDuration -= dt
        position.x += shieldBashDir.x * 400f * dt
        position.y += shieldBashDir.y * 400f * dt
        // Damage + knockback on contact
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

    private fun startGroundSlam() {
        isGroundSlamming = true
        groundSlamPhase = 0 // Jump up
        groundSlamHoverTimer = 0.5f
        groundSlamTimer = groundSlamCooldown
    }

    private fun updateGroundSlam(dt: Float, game: Game) {
        when (groundSlamPhase) {
            0 -> {
                // Jumping up (hover)
                groundSlamHoverTimer -= dt
                if (groundSlamHoverTimer <= 0f) {
                    groundSlamPhase = 2 // Go to landing
                }
            }
            2 -> {
                // Landing - deal damage in radius
                val slamRadius = if (phase >= 2) 100f else 80f
                val slamDamage = 20
                val dist = position.distanceTo(game.player.position)
                if (dist < slamRadius) {
                    game.player.takeDamage(slamDamage, game)
                }
                // Shockwave particles
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

    private fun summonMinions(game: Game) {
        for (i in 0 until summonCount) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val dist = 60f + Random.nextFloat() * 40f
            val spawnX = position.x + cos(angle) * dist
            val spawnY = position.y + sin(angle) * dist
            val minion = Enemy(EnemyType.SKELETON, Vector2(spawnX, spawnY), layerIndex)
            game.enemies.add(minion)
        }
        // Summon particles
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

    private fun startMeteorCast(game: Game) {
        isCastingMeteor = true
        meteorCastTimer = 1.2f // Telegraph duration
        meteorTargetPos = Vector2(game.player.position.x, game.player.position.y)
        meteorTimer = meteorCooldown
    }

    private fun updateMeteorCast(dt: Float, game: Game) {
        meteorCastTimer -= dt
        // Telegraph particle (pulsing circle on ground)
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
            // Impact!
            val dist = game.player.position.distanceTo(meteorTargetPos)
            if (dist < 60f) {
                game.player.takeDamage(25, game)
            }
            // Explosion particles
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

    // Champion: dodge roll
    private fun startDodgeRoll(toPlayer: Vector2) {
        isDodging = true
        dodgeRollDuration = 0.3f
        // Roll perpendicular to player direction
        val perpAngle = atan2(toPlayer.y, toPlayer.x) + (if (Random.nextBoolean()) Math.PI.toFloat() / 2f else -Math.PI.toFloat() / 2f)
        dodgeRollDir = Vector2(cos(perpAngle), sin(perpAngle))
        dodgeRollTimer = dodgeRollCooldown
    }

    private fun updateDodgeRoll(dt: Float) {
        dodgeRollDuration -= dt
        position.x += dodgeRollDir.x * speed * 2f * dt
        position.y += dodgeRollDir.y * speed * 2f * dt
        if (dodgeRollDuration <= 0f) {
            isDodging = false
        }
    }

    // ========================
    // Utility methods
    // ========================

    private fun dealDamageIfClose(game: Game, damage: Int, range: Float = attackRange.toFloat()) {
        val dist = position.distanceTo(game.player.position)
        if (dist < range) {
            game.player.takeDamage(damage, game)
        }
    }

    override fun render(canvas: Canvas, renderer: IsometricRenderer) {
        if (deathAnimationDone) return
        renderer.renderEnemy(canvas, this)
    }
}
