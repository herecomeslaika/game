package com.game.roguelike.entity

import android.graphics.Canvas
import com.game.roguelike.core.Game
import com.game.roguelike.core.PlayerState
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.util.StateMachine
import com.game.roguelike.util.Vector2
import com.game.roguelike.combat.Projectile
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.level.Room
import kotlin.math.abs

class Player : Entity() {

    override var width = 20f
    override var height = 48f
    var maxHealth = 100
    var health = 100

    var speed = 250f
    var facingRight = true
    var isDead = false
        private set

    // Attack stats (modified by blessings)
    var attackDamage1 = 8f
    var attackDamage2 = 10f
    var attackDamage3 = 15f
    var attackSpeedMultiplier = 1f
    var comboSplashRadius = 0f

    // Special attack stats
    var specialDamage = 12f
    var specialCooldown = 0.8f
    var specialCooldownTimer = 0f
    var knifeCount = 1
    var knifePierce = false
    var knifeExplosive = false

    // Dash stats
    var dashSpeed = 700f
    var dashDuration = 0.15f
    var dashCooldown = 0.5f
    var dashCooldownTimer = 0f
    var dashDamage = 0f
    var dashTrailDamage = 0f

    // Support
    var supportCooldown = 8f
    var supportCooldownTimer = 0f
    var athenaShieldActive = false
    var athenaShieldCooldown = 10f
    var athenaShieldTimer = 0f
    var warCryActive = false
    var warCryTimer = 0f
    var warCryDamageBonus = 0f

    // Combo state
    var comboStep = 0
        private set
    var comboTimer = 0f
    var attackTimer = 0f
    var isAttacking1 = false
        private set
    var isAttacking2 = false
        private set
    var isAttacking3 = false
        private set

    // Dash state
    var isDashing = false
        private set
    var dashTimer = 0f
    var dashDirection = Vector2.ZERO
    var isDashInvincible = false
        private set

    // Hurt state
    var hurtTimer = 0f
    var invincibleTimer = 0f

    // Movement animation
    private var moveAnimTime = 0f
    var moveAnimPhase = 0f
    var idleTime = 0f
    // Smooth transition blend: 0 = idle pose, 1 = full walk animation
    var walkBlend = 0f

    val stateMachine = StateMachine(PlayerState.IDLE)

    fun reset() {
        health = maxHealth
        isDead = false
        position = Vector2(400f, 300f)
        velocity = Vector2.ZERO
        comboStep = 0
        facingRight = true
        walkBlend = 0f
        moveAnimTime = 0f
        moveAnimPhase = 0f
        idleTime = 0f
        stateMachine.transitionTo(PlayerState.IDLE)
        attackDamage1 = 8f
        attackDamage2 = 10f
        attackDamage3 = 15f
        attackSpeedMultiplier = 1f
        comboSplashRadius = 0f
        specialDamage = 12f
        specialCooldown = 0.8f
        knifeCount = 1
        knifePierce = false
        knifeExplosive = false
        dashSpeed = 700f
        dashDuration = 0.15f
        dashCooldown = 0.5f
        dashDamage = 0f
        dashTrailDamage = 0f
        supportCooldown = 8f
        athenaShieldActive = false
        warCryDamageBonus = 0f
    }

    override fun update(dt: Float, game: Game) {
        stateMachine.update(dt)
        updateCooldowns(dt)
        updateWarCry(dt)

        when (stateMachine.currentState) {
            PlayerState.IDLE -> updateIdle(dt, game)
            PlayerState.RUN -> updateRun(dt, game)
            PlayerState.ATTACK1 -> updateAttack1(dt, game)
            PlayerState.ATTACK2 -> updateAttack2(dt, game)
            PlayerState.ATTACK3 -> updateAttack3(dt, game)
            PlayerState.SPECIAL -> updateSpecial(dt, game)
            PlayerState.DASH -> updateDash(dt, game)
            PlayerState.HURT -> updateHurt(dt, game)
            PlayerState.DEAD -> {}
            else -> {}
        }

        // Decrement combo timer when not attacking
        if (comboTimer > 0 && stateMachine.currentState != PlayerState.ATTACK1
            && stateMachine.currentState != PlayerState.ATTACK2
            && stateMachine.currentState != PlayerState.ATTACK3) {
            comboTimer -= dt
            if (comboTimer <= 0) {
                comboStep = 0
            }
        }

        clampToRoom(game.currentRoom)
    }

    private fun updateCooldowns(dt: Float) {
        if (specialCooldownTimer > 0) specialCooldownTimer -= dt
        if (dashCooldownTimer > 0) dashCooldownTimer -= dt
        if (supportCooldownTimer > 0) supportCooldownTimer -= dt
        if (invincibleTimer > 0) invincibleTimer -= dt
        if (athenaShieldTimer > 0) {
            athenaShieldTimer -= dt
            if (athenaShieldTimer <= 0 && !athenaShieldActive) {
                athenaShieldActive = true
            }
        }
    }

    private fun updateWarCry(dt: Float) {
        if (warCryActive) {
            warCryTimer -= dt
            if (warCryTimer <= 0) {
                warCryActive = false
                warCryDamageBonus = 0f
            }
        }
    }

    private fun updateIdle(dt: Float, game: Game) {
        val input = game.inputManager ?: return
        velocity = Vector2.ZERO
        // Smoothly fade out walk animation instead of hard reset
        walkBlend = maxOf(0f, walkBlend - dt * 5f)
        if (moveAnimTime > 0f) {
            moveAnimTime = maxOf(0f, moveAnimTime - dt * 3f)
        }
        idleTime += dt
        if (input.joystickDirection.magnitude > 0.1f) {
            idleTime = 0f
            stateMachine.transitionTo(PlayerState.RUN)
        }
    }

    private fun updateRun(dt: Float, game: Game) {
        val input = game.inputManager ?: return
        // Smoothly blend in walk animation
        walkBlend = minOf(1f, walkBlend + dt * 6f)
        // Advance walk cycle continuously
        moveAnimTime += dt
        moveAnimPhase = moveAnimTime * 4f

        val moveDir = input.joystickDirection
        if (moveDir.magnitude > 0.1f) {
            val tw = 64f
            val th = 32f
            val worldDX = moveDir.x / tw + moveDir.y / th
            val worldDY = -moveDir.x / tw + moveDir.y / th
            val worldDir = Vector2(worldDX, worldDY).normalized

            velocity.x += (worldDir.x * speed - velocity.x) * 12f * dt
            velocity.y += (worldDir.y * speed - velocity.y) * 12f * dt

            position.x += velocity.x * dt
            position.y += velocity.y * dt

            // Only update facing when there's active joystick input
            if (worldDir.x > 0.15f) facingRight = true
            else if (worldDir.x < -0.15f) facingRight = false
        } else {
            // Decelerate smoothly, lock facing during deceleration
            walkBlend = maxOf(0f, walkBlend - dt * 4f)
            velocity.x += (0f - velocity.x) * 10f * dt
            velocity.y += (0f - velocity.y) * 10f * dt
            if (velocity.magnitude < 5f) {
                velocity = Vector2.ZERO
                stateMachine.transitionTo(PlayerState.IDLE)
            } else {
                // Still moving from deceleration, apply position change
                position.x += velocity.x * dt
                position.y += velocity.y * dt
            }
        }
    }

    internal fun startAttack1(game: Game) {
        comboStep = 1
        attackTimer = 0.15f / attackSpeedMultiplier
        isAttacking1 = true
        isAttacking2 = false
        isAttacking3 = false
        stateMachine.transitionTo(PlayerState.ATTACK1)

        // Lunge towards nearest enemy
        lungeTowardEnemy(game, 40f)
        dealDamageToEnemies(game, attackDamage1 + warCryDamageBonus, 50f)
        game.shake(3f, 0.08f)
    }

    private fun updateAttack1(dt: Float, game: Game) {
        attackTimer -= dt
        // Allow slight movement toward enemy during attack
        applyAttackDrift(dt, game)
        if (attackTimer <= 0) {
            isAttacking1 = false
            comboTimer = 0.6f
            stateMachine.transitionTo(PlayerState.IDLE)
        }
    }

    internal fun startAttack2(game: Game) {
        comboStep = 2
        attackTimer = 0.15f / attackSpeedMultiplier
        isAttacking1 = false
        isAttacking2 = true
        isAttacking3 = false
        stateMachine.transitionTo(PlayerState.ATTACK2)

        lungeTowardEnemy(game, 45f)
        dealDamageToEnemies(game, attackDamage2 + warCryDamageBonus, 55f)
        game.shake(4f, 0.1f)
    }

    private fun updateAttack2(dt: Float, game: Game) {
        attackTimer -= dt
        applyAttackDrift(dt, game)
        if (attackTimer <= 0) {
            isAttacking2 = false
            comboTimer = 0.6f
            stateMachine.transitionTo(PlayerState.IDLE)
        }
    }

    internal fun startAttack3(game: Game) {
        comboStep = 3
        attackTimer = 0.2f / attackSpeedMultiplier
        isAttacking1 = false
        isAttacking2 = false
        isAttacking3 = true
        stateMachine.transitionTo(PlayerState.ATTACK3)

        lungeTowardEnemy(game, 55f)
        dealDamageToEnemies(game, attackDamage3 + warCryDamageBonus, 70f)
        game.shake(6f, 0.12f)
    }

    private fun updateAttack3(dt: Float, game: Game) {
        attackTimer -= dt
        applyAttackDrift(dt, game)
        if (attackTimer <= 0) {
            isAttacking3 = false
            comboStep = 0
            comboTimer = 0f
            stateMachine.transitionTo(PlayerState.IDLE)
        }
    }

    // Lunge toward nearest enemy on attack start
    private fun lungeTowardEnemy(game: Game, lungeDist: Float) {
        val nearest = findNearestEnemy(game) ?: return
        val dir = (nearest.position - position).normalized
        position.x += dir.x * lungeDist
        position.y += dir.y * lungeDist
        // Only change facing on meaningful horizontal direction
        if (dir.x > 0.2f) facingRight = true
        else if (dir.x < -0.2f) facingRight = false
    }

    // Slight drift toward enemy during attack animation
    private fun applyAttackDrift(dt: Float, game: Game) {
        val nearest = findNearestEnemy(game) ?: return
        val dist = position.distanceTo(nearest.position)
        if (dist > 40f) {
            val dir = (nearest.position - position).normalized
            val driftSpeed = 120f
            position.x += dir.x * driftSpeed * dt
            position.y += dir.y * driftSpeed * dt
        }
    }

    private fun findNearestEnemy(game: Game): com.game.roguelike.entity.Enemy? {
        return game.enemies.filter { !it.isDead }.minByOrNull { it.position.distanceTo(position) }
    }

    internal fun startSpecial(game: Game) {
        if (specialCooldownTimer > 0) return

        specialCooldownTimer = specialCooldown
        stateMachine.transitionTo(PlayerState.SPECIAL)

        // Auto-lock: find nearest enemy, otherwise use facing direction
        val nearest = findNearestEnemy(game)
        val baseDirection = if (nearest != null) {
            (nearest.position - position).normalized
        } else {
            Vector2.fromAngle(if (facingRight) 0f else 3.14159f)
        }

        val spreadAngle = 0.2f
        for (i in 0 until knifeCount) {
            val offsetAngle = if (knifeCount == 1) 0f
                else (i - (knifeCount - 1) / 2f) * spreadAngle
            val knifeDir = Vector2.fromAngle(baseDirection.angle + offsetAngle)
            val knife = Projectile(
                position = Vector2(position.x + knifeDir.x * 15f, position.y + knifeDir.y * 15f),
                velocity = knifeDir * 500f,
                damage = specialDamage + warCryDamageBonus,
                type = ProjectileType.KNIFE,
                maxRange = 500f,
                pierce = knifePierce,
                explosive = knifeExplosive,
                angle = knifeDir.angle,
                target = nearest
            )
            game.projectiles.add(knife)
        }
    }

    private fun updateSpecial(dt: Float, game: Game) {
        if (stateMachine.stateTime > 0.1f) {
            stateMachine.transitionTo(PlayerState.IDLE)
        }
    }

    internal fun startDash(game: Game) {
        if (dashCooldownTimer > 0) return

        dashCooldownTimer = dashCooldown
        isDashing = true
        isDashInvincible = true
        dashTimer = dashDuration

        val input = game.inputManager
        val moveDir = input?.joystickDirection ?: Vector2.ZERO
        dashDirection = if (moveDir.magnitude > 0.1f) {
            val tw = 64f
            val th = 32f
            val worldDX = moveDir.x / tw + moveDir.y / th
            val worldDY = -moveDir.x / tw + moveDir.y / th
            Vector2(worldDX, worldDY).normalized
        } else Vector2.fromAngle(if (facingRight) 0f else 3.14159f)

        stateMachine.transitionTo(PlayerState.DASH)
    }

    private fun updateDash(dt: Float, game: Game) {
        dashTimer -= dt
        position.x += dashDirection.x * dashSpeed * dt
        position.y += dashDirection.y * dashSpeed * dt

        if (dashTrailDamage > 0) {
            dealDamageToEnemies(game, dashTrailDamage, 30f)
        }
        if (dashDamage > 0) {
            dealDamageToEnemies(game, dashDamage, 35f)
        }

        if (dashTimer <= 0) {
            isDashing = false
            isDashInvincible = false
            stateMachine.transitionTo(PlayerState.IDLE)
        }
    }

    private fun updateHurt(dt: Float, game: Game) {
        hurtTimer -= dt
        invincibleTimer = 0.5f
        if (hurtTimer <= 0) {
            stateMachine.transitionTo(PlayerState.IDLE)
        }
    }

    fun takeDamage(amount: Int, game: Game) {
        if (isDashInvincible || invincibleTimer > 0 || isDead) return

        if (athenaShieldActive) {
            athenaShieldActive = false
            athenaShieldTimer = athenaShieldCooldown
            game.shake(2f, 0.1f)
            return
        }

        health -= amount
        hurtTimer = 0.3f
        stateMachine.transitionTo(PlayerState.HURT)
        game.shake(5f, 0.15f)

        for (i in 0..5) {
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2((Math.random().toFloat() - 0.5f) * 100f, (Math.random().toFloat() - 0.5f) * 100f),
                color = android.graphics.Color.RED,
                life = 0.5f,
                size = 3f
            ))
        }

        if (health <= 0) {
            health = 0
            isDead = true
            stateMachine.transitionTo(PlayerState.DEAD)
        }
    }

    private fun dealDamageToEnemies(game: Game, damage: Float, range: Float) {
        val hitCenter = Vector2(
            position.x + (if (facingRight) 1 else -1) * range / 2,
            position.y
        )

        for (enemy in game.enemies) {
            if (enemy.isDead) continue
            val dist = enemy.position.distanceTo(hitCenter)
            if (dist < range) {
                enemy.takeDamage(damage, game)
                if (comboSplashRadius > 0) {
                    for (other in game.enemies) {
                        if (other === enemy || other.isDead) continue
                        if (other.position.distanceTo(enemy.position) < comboSplashRadius) {
                            other.takeDamage(damage * 0.5f, game)
                        }
                    }
                }
                if (enemy.isDead && warCryDamageBonus > 0) {
                    warCryActive = true
                    warCryTimer = 5f
                }
            }
        }
    }

    fun tryComboAttack(game: Game) {
        when (comboStep) {
            1 -> startAttack2(game)
            2 -> startAttack3(game)
            else -> startAttack1(game)
        }
    }

    fun activateSupport(game: Game) {
        if (supportCooldownTimer > 0) return
        supportCooldownTimer = supportCooldown

        val nearest = game.enemies.filter { !it.isDead }.minByOrNull { it.position.distanceTo(position) }
        if (nearest != null) {
            val bolt = Projectile(
                position = Vector2(nearest.position.x, nearest.position.y - 100f),
                velocity = Vector2(0f, 800f),
                damage = 25f,
                type = ProjectileType.ZEUS_BOLT,
                maxRange = 200f,
                angle = 1.57f
            )
            game.projectiles.add(bolt)
        }
    }

    private fun clampToRoom(room: Room?) {
        if (room == null) return
        val tw = 64f
        val th = 32f
        val worldW = room.width * tw
        val worldH = room.height * th

        // Walls in isometric view extend outward from their grid position.
        // Left/bottom walls have visible faces that extend into the room,
        // so we need larger margins on the left and bottom edges.
        val leftMargin = tw      // left wall extends one tile width into room
        val topMargin = th       // top wall extends one tile height into room
        val rightMargin = tw     // right wall extends one tile width into room
        val bottomMargin = th    // bottom wall extends one tile height into room

        // Also check obstacle tiles and push player away from them
        val gridX = (position.x / tw).toInt().coerceIn(0, room.width - 1)
        val gridY = (position.y / th).toInt().coerceIn(0, room.height - 1)

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = gridX + dx
                val ty = gridY + dy
                if (tx < 0 || tx >= room.width || ty < 0 || ty >= room.height) continue
                val tile = room.getTile(tx, ty)
                if (tile == Room.TILE_OBSTACLE) {
                    val tileMinX = tx * tw
                    val tileMinY = ty * th
                    val tileMaxX = tileMinX + tw
                    val tileMaxY = tileMinY + th
                    val pushMargin = 20f

                    if (position.x > tileMinX - pushMargin && position.x < tileMaxX + pushMargin &&
                        position.y > tileMinY - pushMargin && position.y < tileMaxY + pushMargin
                    ) {
                        // Push out from obstacle center
                        val cx = tileMinX + tw / 2f
                        val cy = tileMinY + th / 2f
                        val dx2 = position.x - cx
                        val dy2 = position.y - cy
                        val oX = (tw / 2f + pushMargin) - abs(dx2)
                        val oY = (th / 2f + pushMargin) - abs(dy2)
                        if (oX > 0 && oY > 0) {
                            if (oX < oY) {
                                position.x += if (dx2 >= 0) oX else -oX
                            } else {
                                position.y += if (dy2 >= 0) oY else -oY
                            }
                        }
                    }
                }
            }
        }

        // Final boundary clamp with isometric wall margins
        position.x = position.x.coerceIn(leftMargin, worldW - rightMargin)
        position.y = position.y.coerceIn(topMargin, worldH - bottomMargin)
    }

    override fun render(canvas: Canvas, renderer: IsometricRenderer) {
        renderer.renderPlayer(canvas, this)
    }
}