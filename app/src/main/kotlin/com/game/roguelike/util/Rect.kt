package com.game.roguelike.util

data class Rect(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f
) {
    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2
    val centerY: Float get() = y + height / 2

    fun intersects(other: Rect): Boolean {
        return left < other.right && right > other.left &&
               top < other.bottom && bottom > other.top
    }

    fun contains(px: Float, py: Float): Boolean {
        return px >= left && px <= right && py >= top && py <= bottom
    }

    fun set(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}
