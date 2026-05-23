package com.game.roguelike.util

class Animation(
    val frameCount: Int,
    val frameDuration: Float,
    val loop: Boolean = true
) {
    var currentFrame: Int = 0
        private set
    var elapsed: Float = 0f
        private set
    var finished: Boolean = false
        private set

    val progress: Float get() = elapsed / (frameCount * frameDuration)

    fun update(dt: Float) {
        if (finished && !loop) return
        elapsed += dt
        val totalDuration = frameCount * frameDuration
        if (elapsed >= totalDuration) {
            if (loop) {
                elapsed %= totalDuration
            } else {
                elapsed = totalDuration
                finished = true
            }
        }
        currentFrame = (elapsed / frameDuration).toInt().coerceIn(0, frameCount - 1)
    }

    fun reset() {
        currentFrame = 0
        elapsed = 0f
        finished = false
    }
}
