package com.game.roguelike.entity

import com.game.roguelike.util.Vector2

class MeteorMark(
    position: Vector2,
    val delay: Float = 1.5f,
    val radius: Float = 92f,
    val damage: Float = 46f
) {
    val position = Vector2(position.x, position.y)
    var timer = delay
        private set
    var isDead = false
        private set

    val progress: Float
        get() = (1f - timer / delay).coerceIn(0f, 1f)

    fun update(dt: Float): Boolean {
        if (isDead) return false
        timer -= dt
        if (timer <= 0f) {
            isDead = true
            return true
        }
        return false
    }
}
