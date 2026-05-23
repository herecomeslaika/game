package com.game.roguelike.combat

import com.game.roguelike.core.Game
import com.game.roguelike.entity.Enemy
import com.game.roguelike.util.Vector2
import com.game.roguelike.util.Rect
import kotlin.math.atan2
import kotlin.math.sqrt

enum class ProjectileType {
    KNIFE,
    MAGIC_BOLT,
    FIREBALL,
    SPEAR,
    ZEUS_BOLT,
    METEOR
}

class Projectile(
    position: Vector2,
    velocity: Vector2,
    val damage: Float,
    val type: ProjectileType,
    val maxRange: Float = 400f,
    val pierce: Boolean = false,
    val explosive: Boolean = false,
    val isEnemyProjectile: Boolean = false,
    val angle: Float = 0f,
    val target: Enemy? = null
) {
    var position = Vector2(position.x, position.y)
    var velocity = Vector2(velocity.x, velocity.y)
    var distanceTraveled = 0f
    var shouldRemove = false
    private val hitEnemies = mutableSetOf<Enemy>()

    fun update(dt: Float, game: Game) {
        // Homing: gently steer toward locked target
        if (target != null && !target.isDead) {
            val toTarget = (target.position - position).normalized
            val currentDir = velocity.normalized
            val blended = Vector2(
                currentDir.x + toTarget.x * 4f * dt,
                currentDir.y + toTarget.y * 4f * dt
            ).normalized
            val speed = velocity.magnitude
            velocity.x = blended.x * speed
            velocity.y = blended.y * speed
        }

        val dx = velocity.x * dt
        val dy = velocity.y * dt
        position.x += dx
        position.y += dy
        distanceTraveled += sqrt(dx * dx + dy * dy)

        if (distanceTraveled >= maxRange) {
            shouldRemove = true
            return
        }

        if (!isEnemyProjectile) {
            // Player projectile - check enemy hits
            for (enemy in game.enemies) {
                if (enemy.isDead || hitEnemies.contains(enemy)) continue
                if (position.distanceTo(enemy.position) < 25f) {
                    enemy.takeDamage(damage, game)
                    hitEnemies.add(enemy)

                    if (explosive) {
                        // AOE damage
                        for (other in game.enemies) {
                            if (other.isDead || other === enemy || hitEnemies.contains(other)) continue
                            if (other.position.distanceTo(position) < 60f) {
                                other.takeDamage(damage * 0.6f, game)
                                hitEnemies.add(other)
                            }
                        }
                        // Explosion particles
                        for (i in 0..10) {
                            game.particles.add(com.game.roguelike.entity.Particle(
                                position = Vector2(position.x, position.y),
                                velocity = Vector2((Math.random().toFloat() - 0.5f) * 150f, (Math.random().toFloat() - 0.5f) * 150f),
                                color = android.graphics.Color.parseColor("#FF8800"),
                                life = 0.4f,
                                size = 5f
                            ))
                        }
                    }

                    if (!pierce) {
                        shouldRemove = true
                        return
                    }
                }
            }
        } else {
            // Enemy projectile - check player hit
            if (position.distanceTo(game.player.position) < 20f) {
                game.player.takeDamage(damage.toInt(), game)
                shouldRemove = true
            }
        }
    }
}
