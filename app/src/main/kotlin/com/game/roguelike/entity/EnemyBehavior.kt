package com.game.roguelike.entity

import com.game.roguelike.combat.Projectile
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.core.Game
import com.game.roguelike.util.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

interface EnemyBehavior {
    fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {}
    fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean = false
    fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {}
    fun enterNextPhase(enemy: Enemy, phase: Int) {}
    fun executeBossEnrage(enemy: Enemy, game: Game) {}
}

object SkeletonBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        val comboDmg = when (enemy.comboCount) {
            0 -> enemy.attackDamage
            1 -> (enemy.attackDamage * 1.5f).toInt()
            else -> (enemy.attackDamage * 1.875f).toInt()
        }
        val range = if (enemy.comboCount >= 2) 50f else enemy.attackRange
        enemy.dealDamageIfClose(game, comboDmg, range)
        enemy.comboCount = (enemy.comboCount + 1) % 3
        enemy.comboTimer = 3f
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.comboTimer > 0f) {
            enemy.comboTimer -= dt
            if (enemy.comboTimer <= 0f) enemy.comboCount = 0
        }
    }
}

object WraithBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        val dir = (game.player.position - enemy.position).normalized
        val baseAngle = atan2(dir.y, dir.x)
        for (i in -1..1) {
            val angle = baseAngle + i * 0.2f
            val projDir = Vector2(cos(angle), sin(angle))
            game.projectiles.add(Projectile(
                position = Vector2(enemy.position.x + projDir.x * 15f, enemy.position.y + projDir.y * 15f),
                velocity = projDir * enemy.projectileSpeed,
                damage = enemy.attackDamage.toFloat(),
                type = enemy.projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = projDir.angle
            ))
        }
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        if (enemy.canPhaseShift && distToPlayer < 60f && enemy.phaseShiftTimer <= 0f) {
            performPhaseShift(enemy, game)
            return true
        }
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.phaseShiftTimer > 0f) enemy.phaseShiftTimer -= dt
    }

    private fun performPhaseShift(enemy: Enemy, game: Game) {
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val dist = 100f + Random.nextFloat() * 100f
        val targetX = game.player.position.x + cos(angle) * dist
        val targetY = game.player.position.y + sin(angle) * dist
        for (i in 0..5) {
            val a = Random.nextFloat() * Math.PI.toFloat() * 2f
            game.particles.add(Particle(
                position = Vector2(enemy.position.x, enemy.position.y),
                velocity = Vector2(cos(a) * 40f, sin(a) * 40f),
                color = android.graphics.Color.parseColor("#8844CC"),
                life = 0.4f, size = 4f
            ))
        }
        enemy.position.x = targetX
        enemy.position.y = targetY
        for (i in 0..5) {
            val a = Random.nextFloat() * Math.PI.toFloat() * 2f
            game.particles.add(Particle(
                position = Vector2(enemy.position.x, enemy.position.y),
                velocity = Vector2(cos(a) * 40f, sin(a) * 40f),
                color = android.graphics.Color.parseColor("#AA66EE"),
                life = 0.4f, size = 4f
            ))
        }
        enemy.phaseShiftTimer = enemy.phaseShiftCooldown
        val dir = (game.player.position - enemy.position).normalized
        val baseAngle = atan2(dir.y, dir.x)
        for (i in -1..1) {
            val a = baseAngle + i * 0.2f
            val projDir = Vector2(cos(a), sin(a))
            game.projectiles.add(Projectile(
                position = Vector2(enemy.position.x + projDir.x * 15f, enemy.position.y + projDir.y * 15f),
                velocity = projDir * enemy.projectileSpeed,
                damage = enemy.attackDamage.toFloat(),
                type = enemy.projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = projDir.angle
            ))
        }
    }
}

object FlameDancerBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        enemy.dealDamageIfClose(game, enemy.attackDamage)
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        if (enemy.canFlameDash && !enemy.isFlameDashing &&
            distToPlayer >= 80f && distToPlayer <= 120f && enemy.flameDashTimer <= 0f) {
            enemy.startFlameDash(toPlayer)
            return true
        }
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.flameDashTimer > 0f) enemy.flameDashTimer -= dt
    }
}

object LavaCasterBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        val dir = (game.player.position - enemy.position).normalized
        val baseAngle = atan2(dir.y, dir.x)
        enemy.castCount++
        if (enemy.castCount % 3 == 0) {
            for (i in -1..1) {
                val angle = baseAngle + i * 0.26f
                val projDir = Vector2(cos(angle), sin(angle))
                game.projectiles.add(Projectile(
                    position = Vector2(enemy.position.x + projDir.x * 15f, enemy.position.y + projDir.y * 15f),
                    velocity = projDir * enemy.projectileSpeed,
                    damage = enemy.attackDamage.toFloat(),
                    type = enemy.projectileType,
                    maxRange = 400f,
                    isEnemyProjectile = true,
                    angle = projDir.angle
                ))
            }
        } else {
            game.projectiles.add(Projectile(
                position = Vector2(enemy.position.x + dir.x * 15f, enemy.position.y + dir.y * 15f),
                velocity = dir * enemy.projectileSpeed,
                damage = enemy.attackDamage.toFloat(),
                type = enemy.projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = dir.angle
            ))
        }
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.lavaPoolTimer > 0f) {
            enemy.lavaPoolTimer -= dt
            if (enemy.lavaPoolTimer <= 0f) {
                enemy.dropLavaPool(game)
                enemy.lavaPoolTimer = 5f
            }
        }
    }
}

object ShieldBearerBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        enemy.dealDamageIfClose(game, enemy.attackDamage)
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        if (!enemy.shieldThrown && distToPlayer < 45f && enemy.shieldBashTimer <= 0f) {
            enemy.startShieldBash(toPlayer)
            return true
        }
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.shieldBashTimer > 0f) enemy.shieldBashTimer -= dt
    }
}

object SpearThrowerBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        val dir = (game.player.position - enemy.position).normalized
        enemy.fireSpear(game, dir)
        enemy.multiSpearCount = 1
        enemy.multiSpearTimer = 0.15f
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        if (distToPlayer < 80f) {
            enemy.isRetreating = true
            val awayDir = (enemy.position - game.player.position).normalized
            enemy.position.x += awayDir.x * enemy.speed * 0.6f * dt
            enemy.position.y += awayDir.y * enemy.speed * 0.6f * dt
            if (distToPlayer < enemy.attackRange && enemy.attackCooldownTimer <= 0f) {
                enemy.startPrepareAttack(game, toPlayer)
            }
            return true
        }
        enemy.isRetreating = false
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.multiSpearCount > 0 && enemy.multiSpearTimer > 0f) {
            enemy.multiSpearTimer -= dt
            if (enemy.multiSpearTimer <= 0f && enemy.multiSpearCount > 0) {
                val dir = (game.player.position - enemy.position).normalized
                enemy.fireSpear(game, dir)
                enemy.multiSpearCount--
                if (enemy.multiSpearCount > 0) enemy.multiSpearTimer = 0.15f
            }
        }
    }
}

object MegaSkeletonBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        if (enemy.canGroundSlam && enemy.groundSlamTimer <= 0f && distToPlayer < 120f) {
            game.bossWarnings.add(BossWarning.circle(
                position = enemy.position,
                radius = if (enemy.phase >= 2) 104f else 82f,
                warnDuration = 0.45f,
                damage = 18,
                color = android.graphics.Color.parseColor("#AAFF66"),
                effect = BossWarningEffect.SLAM
            ))
            enemy.startGroundSlam()
            return
        }
        enemy.dealDamageIfClose(game, enemy.attackDamage)
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        if (enemy.canSummon && enemy.summonTimer <= 0f) {
            enemy.summonMinions(game)
            enemy.summonTimer = enemy.summonCooldown
        }
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.groundSlamTimer > 0f) enemy.groundSlamTimer -= dt
        if (enemy.summonTimer > 0f) enemy.summonTimer -= dt
    }

    override fun enterNextPhase(enemy: Enemy, phase: Int) {
        when (phase) {
            1 -> {
                enemy.canGroundSlam = true; enemy.summonCount = 3; enemy.summonCooldown = 3.6f; enemy.speed *= 1.25f
                enemy.attackCooldown *= 0.78f
            }
            2 -> {
                enemy.attackDamage = (enemy.attackDamage * 1.35f).toInt(); enemy.summonCount = 5; enemy.summonCooldown = 2.6f
                enemy.bossEnrageCooldown = 3.1f
            }
        }
    }

    override fun executeBossEnrage(enemy: Enemy, game: Game) {
        when (enemy.bossSkillIndex++ % if (enemy.phase >= 2) 3 else 2) {
            0 -> enemy.startBoneWallLockdown(game)
            1 -> {
                if (enemy.canSummon && enemy.summonTimer <= 0f) {
                    enemy.summonMinions(game)
                    enemy.summonTimer = enemy.summonCooldown
                }
                enemy.triggerCorpseBursts(game)
            }
            else -> enemy.startTripleGroundSlam(game)
        }
    }
}

object InfernoTitanBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        if (distToPlayer < enemy.attackRange) {
            enemy.dealDamageIfClose(game, enemy.attackDamage)
            if (enemy.phase == 0 && enemy.chargeComboCount <= 0) {
                enemy.chargeComboCount = enemy.chargeComboMax - 1
            }
        } else if (enemy.isRanged) {
            val dir = (game.player.position - enemy.position).normalized
            game.projectiles.add(Projectile(
                position = Vector2(enemy.position.x + dir.x * 15f, enemy.position.y + dir.y * 15f),
                velocity = dir * enemy.projectileSpeed,
                damage = enemy.attackDamage.toFloat(),
                type = enemy.projectileType,
                maxRange = 400f,
                isEnemyProjectile = true,
                angle = dir.angle
            ))
        }
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        if (enemy.canMeteor && enemy.meteorTimer <= 0f && distToPlayer > 60f) {
            enemy.startMeteorRain(game)
            return true
        }
        if (enemy.isBoss && enemy.phase >= 1 && !enemy.isFlameDashing && enemy.flameDashTimer <= 0f &&
            distToPlayer > 85f && distToPlayer < 230f) {
            enemy.startBlazingCharge(game)
            return true
        }
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.meteorTimer > 0f) enemy.meteorTimer -= dt
    }

    override fun enterNextPhase(enemy: Enemy, phase: Int) {
        when (phase) {
            1 -> {
                enemy.canMeteor = true; enemy.speed *= 1.35f; enemy.chargeComboMax = 3; enemy.meteorCooldown = 3.8f
                enemy.attackCooldown *= 0.68f
                enemy.bossEnrageCooldown = 3.5f
            }
        }
    }

    override fun executeBossEnrage(enemy: Enemy, game: Game) {
        when (enemy.bossSkillIndex++ % 3) {
            0 -> enemy.startMeteorRain(game)
            1 -> enemy.startBlazingCharge(game)
            else -> enemy.startLavaCross(game)
        }
    }
}

object ChampionBehavior : EnemyBehavior {
    override fun attack(enemy: Enemy, game: Game, distToPlayer: Float) {
        if (enemy.phase == 1 && distToPlayer < 60f) {
            enemy.meleeComboStep++
            val comboDmg = (enemy.attackDamage * (1f + enemy.meleeComboStep * 0.2f)).toInt()
            enemy.dealDamageIfClose(game, comboDmg, 55f)
            if (enemy.meleeComboStep >= 3) {
                enemy.meleeComboStep = 0
            }
            return
        }
        if (distToPlayer > 60f && enemy.isRanged) {
            enemy.startSpearFan(game)
        } else {
            enemy.dealDamageIfClose(game, enemy.attackDamage)
        }
    }

    override fun updateChase(enemy: Enemy, dt: Float, game: Game, toPlayer: Vector2, distToPlayer: Float): Boolean {
        val thrustTriggerRange = if (enemy.phase >= 2) 230f else 150f
        if (enemy.isBoss && enemy.phase >= 1 && distToPlayer < thrustTriggerRange && enemy.dodgeRollTimer <= 0f) {
            enemy.startHeroicThrust(game)
            enemy.dodgeRollTimer = enemy.dodgeRollCooldown
            return true
        }
        return false
    }

    override fun updateTypeTimers(enemy: Enemy, dt: Float, game: Game) {
        if (enemy.dodgeRollTimer > 0f) enemy.dodgeRollTimer -= dt
    }

    override fun enterNextPhase(enemy: Enemy, phase: Int) {
        when (phase) {
            1 -> {
                enemy.canDodgeRoll = true; enemy.speed *= 1.22f; enemy.shieldThrown = false
                enemy.attackDamage = (enemy.attackDamage * 1.15f).toInt()
                enemy.attackCooldown *= 0.8f
                enemy.dodgeRollCooldown = 3.2f
                enemy.bossEnrageCooldown = 2.4f
            }
            2 -> {
                enemy.speed *= 1.28f
                enemy.attackDamage = (enemy.attackDamage * 1.3f).toInt()
                enemy.attackCooldown *= 0.62f
                enemy.projectileSpeed *= 1.22f
                enemy.dodgeRollCooldown = 1.9f
                enemy.bossEnrageCooldown = 1.55f
            }
        }
    }

    override fun executeBossEnrage(enemy: Enemy, game: Game) {
        when (enemy.bossSkillIndex++ % if (enemy.phase >= 2) 4 else if (enemy.phase >= 1) 3 else 2) {
            0 -> enemy.startShieldCounter(game)
            1 -> enemy.startSpearFan(game)
            2 -> enemy.startHeroicThrust(game)
            else -> {
                enemy.startSpearFan(game)
                enemy.startHeroicThrust(game)
            }
        }
    }
}

object EnemyBehaviors {
    private val registry = mutableMapOf<EnemyType, EnemyBehavior>()

    fun forType(type: EnemyType): EnemyBehavior = registry[type]!!

    private fun register(type: EnemyType, behavior: EnemyBehavior) {
        registry[type] = behavior
    }

    init {
        register(EnemyType.SKELETON, SkeletonBehavior)
        register(EnemyType.WRAITH, WraithBehavior)
        register(EnemyType.MEGA_SKELETON, MegaSkeletonBehavior)
        register(EnemyType.FLAME_DANCER, FlameDancerBehavior)
        register(EnemyType.LAVA_CASTER, LavaCasterBehavior)
        register(EnemyType.INFERNO_TITAN, InfernoTitanBehavior)
        register(EnemyType.SHIELD_BEARER, ShieldBearerBehavior)
        register(EnemyType.SPEAR_THROWER, SpearThrowerBehavior)
        register(EnemyType.CHAMPION, ChampionBehavior)
    }
}
