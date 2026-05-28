package com.game.roguelike.entity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.game.roguelike.core.Game
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.util.Vector2
import kotlin.math.sin

class GhostSummon(
    spawnPos: Vector2
) : Entity() {

    override var width = 12f
    override var height = 28f
    var lifeTimer = 3f
    var attackTimer = 0f
    var attackCooldown = 0.5f
    var attackDamage = 10f
    var attackRange = 50f
    var isDead = false
    var animTime = 0f

    init {
        position.set(spawnPos)
    }

    override fun update(dt: Float, game: Game) {
        if (isDead) return
        lifeTimer -= dt
        animTime += dt
        if (lifeTimer <= 0f) {
            isDead = true
            return
        }

        // Find nearest alive enemy and move toward it
        val nearest = game.enemies.filter { !it.isDead }.minByOrNull {
            it.position.distanceTo(position)
        }

        if (nearest != null) {
            val dir = (nearest.position - position).normalized
            val speed = 200f
            position.x += dir.x * speed * dt
            position.y += dir.y * speed * dt

            // Attack when close enough
            val dist = position.distanceTo(nearest.position)
            attackTimer -= dt
            if (dist < attackRange && attackTimer <= 0f) {
                nearest.takeDamage(attackDamage, game)
                attackTimer = attackCooldown
                // Ghost attack particles
                for (i in 0..3) {
                    game.particles.add(Particle(
                        position = Vector2(nearest.position.x, nearest.position.y),
                        velocity = Vector2((kotlin.random.Random.nextFloat() - 0.5f) * 60f, -40f),
                        color = Color.parseColor("#AA44FF"),
                        life = 0.3f,
                        size = 3f
                    ))
                }
            }
        } else {
            // No enemies — hover near player
            val toPlayer = game.player.position - position
            if (toPlayer.magnitude > 30f) {
                val dir = toPlayer.normalized
                position.x += dir.x * 100f * dt
                position.y += dir.y * 100f * dt
            }
        }
    }

    override fun render(canvas: Canvas, renderer: IsometricRenderer) {
        if (isDead) return
        val (sx, sy) = renderer.worldToScreen(position)
        val floatBob = sin(animTime * 4f) * 3f
        val fadeAlpha = if (lifeTimer < 0.5f) (lifeTimer / 0.5f * 180).toInt() else 180

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Ghost body
        paint.color = Color.argb(fadeAlpha, 170, 68, 255)
        paint.style = Paint.Style.FILL
        val path = android.graphics.Path().apply {
            moveTo(sx - 8f, sy - 4f + floatBob)
            lineTo(sx - 8f, sy - 24f + floatBob)
            quadTo(sx, sy - 38f + floatBob, sx + 8f, sy - 24f + floatBob)
            lineTo(sx + 8f, sy - 4f + floatBob)
            // Wavy bottom
            val wave = sin(animTime * 5f) * 2f
            lineTo(sx + 5f, sy + 2f + wave + floatBob)
            lineTo(sx, sy - 1f - wave + floatBob)
            lineTo(sx - 5f, sy + 2f + wave + floatBob)
            close()
        }
        canvas.drawPath(path, paint)

        // Inner glow
        paint.color = Color.argb(fadeAlpha / 2, 200, 120, 255)
        canvas.drawPath(android.graphics.Path().apply {
            moveTo(sx - 4f, sy - 8f + floatBob)
            lineTo(sx - 4f, sy - 20f + floatBob)
            quadTo(sx, sy - 30f + floatBob, sx + 4f, sy - 20f + floatBob)
            lineTo(sx + 4f, sy - 8f + floatBob)
            close()
        }, paint)

        // Eyes
        paint.color = Color.argb(fadeAlpha, 255, 200, 50)
        canvas.drawCircle(sx - 3f, sy - 28f + floatBob, 2.5f, paint)
        canvas.drawCircle(sx + 3f, sy - 28f + floatBob, 2.5f, paint)

        // Fading sparkles
        for (i in 0..2) {
            val px = sx + sin(animTime * 3f + i * 2.1f) * 10f
            val py = sy - 15f + sin(animTime * 2.5f + i * 1.7f) * 8f + floatBob
            paint.color = Color.argb(fadeAlpha / 3, 200, 150, 255)
            canvas.drawCircle(px, py, 1.5f, paint)
        }
    }
}