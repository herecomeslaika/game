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

enum class EnemyType {
    SKELETON, WRAITH, MEGA_SKELETON,
    FLAME_DANCER, LAVA_CASTER, INFERNO_TITAN,
    SHIELD_BEARER, SPEAR_THROWER, CHAMPION
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

    // Summon (for mega skeleton)
    var canSummon = false
    var summonCooldown = 5f
    var summonTimer = 0f

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
    private var stateTimer = 0f

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
            }
            EnemyType.MEGA_SKELETON -> {
                maxHealth = 200; health = 200; speed = 60f; attackDamage = 15
                attackRange = 50f; attackCooldown = 1.5f; goldDrop = 100
                canSummon = true; width = 32f; height = 60f
                name = "巨型骷髅"
            }
            EnemyType.FLAME_DANCER -> {
                maxHealth = 40; health = 40; speed = 140f; attackDamage = 10
                attackRange = 35f; attackCooldown = 0.8f; goldDrop = 15
                leavesFireTrail = true
                name = "火焰舞者"
            }
            EnemyType.LAVA_CASTER -> {
                maxHealth = 35; health = 35; speed = 50f; attackDamage = 12
                isRanged = true; projectileSpeed = 200f; projectileType = ProjectileType.FIREBALL
                attackRange = 250f; attackCooldown = 1.8f; goldDrop = 18
                name = "熔岩术士"
            }
            EnemyType.INFERNO_TITAN -> {
                maxHealth = 350; health = 350; speed = 50f; attackDamage = 20
                isRanged = true; projectileSpeed = 160f; projectileType = ProjectileType.FIREBALL
                attackRange = 200f; attackCooldown = 2f; goldDrop = 150
                canCharge = true; width = 40f; height = 70f
                name = "炼狱泰坦"
            }
            EnemyType.SHIELD_BEARER -> {
                maxHealth = 60; health = 60; speed = 70f; attackDamage = 10
                attackRange = 40f; attackCooldown = 1.5f; goldDrop = 20
                hasShield = true
                name = "持盾守卫"
            }
            EnemyType.SPEAR_THROWER -> {
                maxHealth = 45; health = 45; speed = 60f; attackDamage = 14
                isRanged = true; projectileSpeed = 250f; projectileType = ProjectileType.SPEAR
                attackRange = 300f; attackCooldown = 2f; goldDrop = 22
                name = "投矛手"
            }
            EnemyType.CHAMPION -> {
                maxHealth = 500; health = 500; speed = 70f; attackDamage = 18
                isRanged = true; projectileSpeed = 220f; projectileType = ProjectileType.SPEAR
                attackRange = 150f; attackCooldown = 1.5f; goldDrop = 200
                hasShield = true; width = 32f; height = 60f
                name = "冠军勇士"
            }
        }
    }

    override fun update(dt: Float, game: Game) {
        if (isDead) {
            deathAnimationDone = true
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

        val distToPlayer = position.distanceTo(game.player.position)

        when (stateMachine.currentState) {
            EnemyState.IDLE -> updateIdle(dt, game, distToPlayer)
            EnemyState.PATROL -> updatePatrol(dt, game, distToPlayer)
            EnemyState.CHASE -> updateChase(dt, game, distToPlayer)
            EnemyState.ATTACK -> updateAttack(dt, game, distToPlayer)
            EnemyState.HURT -> updateHurt(dt, game)
            EnemyState.DEAD -> {}
        }

        // Update shield direction
        if (hasShield) {
            shieldDirection = if (game.player.position.x > position.x) 1 else -1
            facingRight = shieldDirection > 0
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
            position.x += norm.x * speed * 0.3f * dt
            position.y += norm.y * speed * 0.3f * dt
            facingRight = norm.x > 0
        } else {
            stateMachine.transitionTo(EnemyState.IDLE)
        }
    }

    private fun updateChase(dt: Float, game: Game, distToPlayer: Float) {
        if (distToPlayer > aggroRange * 1.5f) {
            stateMachine.transitionTo(EnemyState.IDLE)
            return
        }

        if (distToPlayer <= attackRange) {
            stateMachine.transitionTo(EnemyState.ATTACK)
            return
        }

        val playerPos = game.player.position
        val dir = (playerPos - position).normalized
        position.x += dir.x * speed * dt
        position.y += dir.y * speed * dt
        facingRight = dir.x > 0

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
    }

    private fun updateAttack(dt: Float, game: Game, distToPlayer: Float) {
        if (attackCooldownTimer > 0) {
            // Wait for cooldown
            if (distToPlayer > attackRange * 1.5f) {
                stateMachine.transitionTo(EnemyState.CHASE)
            }
            return
        }

        val playerPos = game.player.position
        facingRight = playerPos.x > position.x

        if (isRanged) {
            // Ranged attack
            val dir = (playerPos - position).normalized
            val proj = Projectile(
                position = Vector2(position.x + dir.x * 15f, position.y + dir.y * 15f),
                velocity = dir * projectileSpeed,
                damage = attackDamage.toFloat(),
                type = projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = dir.angle
            )
            game.projectiles.add(proj)
        } else {
            // Melee attack
            if (distToPlayer < attackRange + 10f) {
                game.player.takeDamage(attackDamage, game)
            }
        }

        // Boss: summon minions
        if (canSummon && summonTimer <= 0) {
            summonTimer = summonCooldown
            for (i in 0..1) {
                val offset = Vector2((Random.nextFloat() - 0.5f) * 60f, (Random.nextFloat() - 0.5f) * 60f)
                val minion = Enemy(EnemyType.SKELETON, position + offset, layerIndex)
                game.enemies.add(minion)
            }
        }

        // Boss phase transition
        if (isBoss && health.toFloat() / maxHealth < phaseThreshold && phase == 0) {
            phase = 1
            speed *= 1.3f
            attackDamage = (attackDamage * 1.5f).toInt()
            attackCooldown *= 0.7f
        }

        attackCooldownTimer = attackCooldown
        stateMachine.transitionTo(EnemyState.CHASE)
    }

    private fun updateHurt(dt: Float, game: Game) {
        stateTimer += dt
        if (stateTimer > 0.3f) {
            stateMachine.transitionTo(EnemyState.CHASE)
        }
    }

    fun takeDamage(amount: Float, game: Game) {
        if (isDead) return

        // Shield check
        if (hasShield && phase == 0) {
            val playerDir = if (game.player.position.x > position.x) 1 else -1
            if (playerDir == shieldDirection) {
                // Blocked! Reduce damage by 80%
                health -= (amount * 0.2f).toInt()
            } else {
                health -= amount.toInt()
            }
        } else {
            health -= amount.toInt()
        }

        stateMachine.transitionTo(EnemyState.HURT)
        stateTimer = 0f

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
        }
    }

    override fun render(canvas: Canvas, renderer: IsometricRenderer) {
        if (isDead) return
        renderer.renderEnemy(canvas, this)
    }
}
