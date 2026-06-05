package com.game.roguelike.entity

import android.graphics.Color
import com.game.roguelike.combat.Projectile
import com.game.roguelike.combat.ProjectileType
import com.game.roguelike.core.Game
import com.game.roguelike.util.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

enum class BossWarningShape {
    CIRCLE,
    LINE,
    CROSS,
    FAN,
    WALL
}

enum class BossWarningEffect {
    BONE,
    CORPSE,
    SLAM,
    FIRE,
    METEOR,
    SPEAR,
    THRUST
}

class BossWarning(
    val shape: BossWarningShape,
    val position: Vector2,
    val start: Vector2,
    val end: Vector2,
    val radius: Float,
    val width: Float,
    val angle: Float,
    val arc: Float,
    val warnDuration: Float,
    val damage: Int,
    val color: Int,
    val effect: BossWarningEffect,
    val lingeringDuration: Float = 0f
) {
    private var timer = warnDuration
    private var lingerTimer = lingeringDuration
    private var contactDamageTimer = 0f
    var resolved = false
        private set

    val warningRatio: Float
        get() = if (warnDuration <= 0f) 0f else (timer / warnDuration).coerceIn(0f, 1f)

    val readyToResolve: Boolean
        get() = timer <= 0f

    val isDead: Boolean
        get() = resolved && lingerTimer <= 0f

    fun updateTimer(dt: Float) {
        if (!resolved) timer -= dt
    }

    fun update(dt: Float, game: Game) {
        if (!resolved) {
            updateTimer(dt)
            if (readyToResolve) {
                resolve(game)
            }
            return
        }

        if (lingerTimer > 0f) {
            lingerTimer -= dt
            contactDamageTimer -= dt
            if (contains(game.player.position) && contactDamageTimer <= 0f && !game.player.isDashInvincible) {
                contactDamageTimer = 0.35f
                game.player.takeDamage(max(1, damage / 4), game)
                val push = (game.player.position - position).normalized
                game.player.position.x += push.x * 18f
                game.player.position.y += push.y * 18f
            }
        }
    }

    fun contains(point: Vector2): Boolean {
        return when (shape) {
            BossWarningShape.CIRCLE -> point.distanceTo(position) <= radius
            BossWarningShape.LINE, BossWarningShape.WALL -> distanceToSegment(point, start, end) <= width / 2f
            BossWarningShape.CROSS -> {
                val horizontal = abs(point.y - position.y) <= width / 2f && abs(point.x - position.x) <= radius
                val vertical = abs(point.x - position.x) <= width / 2f && abs(point.y - position.y) <= radius
                horizontal || vertical
            }
            BossWarningShape.FAN -> {
                val toPoint = point - position
                toPoint.magnitude <= radius && abs(angleDelta(toPoint.angle, angle)) <= arc / 2f
            }
        }
    }

    private fun resolve(game: Game) {
        resolved = true
        if (contains(game.player.position) && !game.player.isDashInvincible) {
            game.player.takeDamage(damage, game)
        }

        when (effect) {
            BossWarningEffect.BONE -> spawnBoneBurst(game)
            BossWarningEffect.CORPSE -> spawnCorpseBlast(game)
            BossWarningEffect.SLAM -> spawnSlam(game)
            BossWarningEffect.FIRE -> spawnFire(game)
            BossWarningEffect.METEOR -> spawnMeteor(game)
            BossWarningEffect.SPEAR -> spawnSpearFan(game)
            BossWarningEffect.THRUST -> spawnThrust(game)
        }
    }

    private fun spawnBoneBurst(game: Game) {
        spawnLineParticles(game, Color.parseColor("#DDEEDD"), 18, 4f)
        game.shake(5f, 0.14f)
    }

    private fun spawnCorpseBlast(game: Game) {
        spawnRadialParticles(game, Color.parseColor("#AAFF88"), 22, 130f, 0.55f, 5f)
        game.shake(7f, 0.18f)
    }

    private fun spawnSlam(game: Game) {
        spawnRadialParticles(game, color, 28, 170f, 0.5f, 6f)
        game.shake(10f, 0.18f)
    }

    private fun spawnFire(game: Game) {
        if (shape == BossWarningShape.CROSS) {
            spawnCrossFire(game)
        } else {
            spawnLineParticles(game, Color.parseColor("#FF6622"), 24, 8f, damage = 3f)
        }
        game.shake(8f, 0.16f)
    }

    private fun spawnMeteor(game: Game) {
        spawnRadialParticles(game, Color.parseColor("#FF4400"), 28, 190f, 0.75f, 8f, damage = 3f)
        game.shake(12f, 0.24f)
    }

    private fun spawnSpearFan(game: Game) {
        val count = 3
        for (i in 0 until count) {
            val a = angle - arc / 2f + arc * i / (count - 1)
            val dir = Vector2(cos(a), sin(a))
            game.projectiles.add(Projectile(
                position = Vector2(position.x + dir.x * 18f, position.y + dir.y * 18f),
                velocity = dir * 250f,
                damage = damage * 0.6f,
                type = ProjectileType.SPEAR,
                maxRange = radius + 80f,
                isEnemyProjectile = true,
                angle = dir.angle
            ))
        }
        spawnRadialParticles(game, Color.parseColor("#88AAFF"), 12, 90f, 0.4f, 4f)
    }

    private fun spawnThrust(game: Game) {
        spawnLineParticles(game, Color.parseColor("#AACCFF"), 18, 5f)
        game.shake(6f, 0.12f)
    }

    private fun spawnCrossFire(game: Game) {
        for (i in -6..6) {
            addHazardParticle(game, Vector2(position.x + i * 24f, position.y), Color.parseColor("#FF5522"), 3f)
            addHazardParticle(game, Vector2(position.x, position.y + i * 24f), Color.parseColor("#FF5522"), 3f)
        }
    }

    private fun spawnLineParticles(game: Game, particleColor: Int, count: Int, size: Float, damage: Float = 0f) {
        val from = if (shape == BossWarningShape.LINE || shape == BossWarningShape.WALL) start else Vector2(position.x - radius, position.y)
        val to = if (shape == BossWarningShape.LINE || shape == BossWarningShape.WALL) end else Vector2(position.x + radius, position.y)

        for (i in 0..count) {
            val t = i / count.toFloat()
            val px = from.x + (to.x - from.x) * t
            val py = from.y + (to.y - from.y) * t
            addHazardParticle(game, Vector2(px, py), particleColor, damage, size)
        }
    }

    private fun spawnRadialParticles(
        game: Game,
        particleColor: Int,
        count: Int,
        speed: Float,
        life: Float,
        size: Float,
        damage: Float = 0f
    ) {
        for (i in 0 until count) {
            val a = Random.nextFloat() * PI.toFloat() * 2f
            val spd = speed * (0.55f + Random.nextFloat() * 0.7f)
            game.particles.add(Particle(
                position = Vector2(position.x, position.y),
                velocity = Vector2(cos(a) * spd, sin(a) * spd),
                color = particleColor,
                life = life,
                size = size,
                damage = damage,
                isFireTrail = damage > 0f
            ))
        }
    }

    private fun addHazardParticle(game: Game, pos: Vector2, particleColor: Int, damage: Float, size: Float = 9f) {
        game.particles.add(Particle(
            position = pos,
            velocity = Vector2((Random.nextFloat() - 0.5f) * 14f, (Random.nextFloat() - 0.5f) * 14f),
            color = particleColor,
            life = if (damage > 0f) 1.6f else 0.65f,
            size = size,
            damage = damage,
            isFireTrail = damage > 0f
        ))
    }

    private fun distanceToSegment(point: Vector2, a: Vector2, b: Vector2): Float {
        val ab = b - a
        val lenSq = ab.x * ab.x + ab.y * ab.y
        if (lenSq <= 0.0001f) return point.distanceTo(a)
        val ap = point - a
        val t = ((ap.x * ab.x + ap.y * ab.y) / lenSq).coerceIn(0f, 1f)
        val closest = Vector2(a.x + ab.x * t, a.y + ab.y * t)
        return point.distanceTo(closest)
    }

    private fun angleDelta(a: Float, b: Float): Float {
        var delta = (a - b) % (PI.toFloat() * 2f)
        if (delta > PI.toFloat()) delta -= PI.toFloat() * 2f
        if (delta < -PI.toFloat()) delta += PI.toFloat() * 2f
        return delta
    }

    companion object {
        fun circle(
            position: Vector2,
            radius: Float,
            warnDuration: Float,
            damage: Int,
            color: Int,
            effect: BossWarningEffect = BossWarningEffect.SLAM
        ) = BossWarning(
            shape = BossWarningShape.CIRCLE,
            position = Vector2(position.x, position.y),
            start = Vector2.ZERO,
            end = Vector2.ZERO,
            radius = radius,
            width = radius * 2f,
            angle = 0f,
            arc = 0f,
            warnDuration = warnDuration,
            damage = damage,
            color = color,
            effect = effect
        )

        fun line(
            start: Vector2,
            end: Vector2,
            width: Float,
            warnDuration: Float,
            damage: Int,
            color: Int,
            effect: BossWarningEffect = BossWarningEffect.THRUST,
            lingeringDuration: Float = 0f
        ) = BossWarning(
            shape = BossWarningShape.LINE,
            position = midpoint(start, end),
            start = Vector2(start.x, start.y),
            end = Vector2(end.x, end.y),
            radius = start.distanceTo(end) / 2f,
            width = width,
            angle = (end - start).angle,
            arc = 0f,
            warnDuration = warnDuration,
            damage = damage,
            color = color,
            effect = effect,
            lingeringDuration = lingeringDuration
        )

        fun wall(
            start: Vector2,
            end: Vector2,
            width: Float,
            warnDuration: Float,
            color: Int
        ) = BossWarning(
            shape = BossWarningShape.WALL,
            position = midpoint(start, end),
            start = Vector2(start.x, start.y),
            end = Vector2(end.x, end.y),
            radius = start.distanceTo(end) / 2f,
            width = width,
            angle = (end - start).angle,
            arc = 0f,
            warnDuration = warnDuration,
            damage = 10,
            color = color,
            effect = BossWarningEffect.BONE,
            lingeringDuration = 3.0f
        )

        fun cross(
            position: Vector2,
            radius: Float,
            width: Float,
            warnDuration: Float,
            damage: Int,
            color: Int
        ) = BossWarning(
            shape = BossWarningShape.CROSS,
            position = Vector2(position.x, position.y),
            start = Vector2.ZERO,
            end = Vector2.ZERO,
            radius = radius,
            width = width,
            angle = 0f,
            arc = 0f,
            warnDuration = warnDuration,
            damage = damage,
            color = color,
            effect = BossWarningEffect.FIRE
        )

        fun fan(
            position: Vector2,
            angle: Float,
            arc: Float,
            radius: Float,
            warnDuration: Float,
            damage: Int,
            color: Int,
            effect: BossWarningEffect = BossWarningEffect.SPEAR
        ) = BossWarning(
            shape = BossWarningShape.FAN,
            position = Vector2(position.x, position.y),
            start = Vector2.ZERO,
            end = Vector2.ZERO,
            radius = radius,
            width = 0f,
            angle = angle,
            arc = arc,
            warnDuration = warnDuration,
            damage = damage,
            color = color,
            effect = effect
        )

        private fun midpoint(a: Vector2, b: Vector2): Vector2 {
            return Vector2((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        }
    }
}
