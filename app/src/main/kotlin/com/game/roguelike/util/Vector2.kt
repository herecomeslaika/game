package com.game.roguelike.util

import kotlinx.serialization.Serializable
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Serializable
data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    val magnitude: Float get() = sqrt(x * x + y * y)

    val normalized: Vector2
        get() {
            val mag = magnitude
            return if (mag > 0) Vector2(x / mag, y / mag) else Vector2(0f, 0f)
        }

    val angle: Float get() = atan2(y, x)

    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector2(x / scalar, y / scalar)

    fun distanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun dot(other: Vector2): Float = x * other.x + y * other.y

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun set(v: Vector2) {
        this.x = v.x
        this.y = v.y
    }

    fun lerp(target: Vector2, t: Float): Vector2 {
        return Vector2(
            x + (target.x - x) * t,
            y + (target.y - y) * t
        )
    }

    companion object {
        fun fromAngle(angle: Float, magnitude: Float = 1f): Vector2 {
            return Vector2(cos(angle) * magnitude, sin(angle) * magnitude)
        }

        val ZERO = Vector2(0f, 0f)
        val UP = Vector2(0f, -1f)
        val DOWN = Vector2(0f, 1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
    }
}
