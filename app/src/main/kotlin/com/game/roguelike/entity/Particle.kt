package com.game.roguelike.entity

import android.graphics.Canvas
import android.graphics.Color
import com.game.roguelike.core.Game
import com.game.roguelike.rendering.IsometricRenderer
import com.game.roguelike.util.Vector2

class Particle(
    position: Vector2,
    velocity: Vector2,
    val color: Int,
    val life: Float,
    val size: Float = 3f,
    val damage: Float = 0f,
    val isFireTrail: Boolean = false,
    val heightOffset: Float = 0f
) {
    var position = Vector2(position.x, position.y)
    var velocity = Vector2(velocity.x, velocity.y)
    var maxLife = life
    var isDead = false

    fun update(dt: Float) {
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        maxLife -= dt
        if (maxLife <= 0) isDead = true
        // Slow down
        velocity.x *= 0.95f
        velocity.y *= 0.95f
    }
}