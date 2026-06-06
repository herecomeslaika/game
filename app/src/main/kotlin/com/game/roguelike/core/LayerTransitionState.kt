package com.game.roguelike.core

class LayerTransitionState {
    var pendingTargetLayerIndex: Int? = null
        private set

    fun beginFrom(currentLayerIndex: Int): Boolean {
        if (pendingTargetLayerIndex != null) return false
        pendingTargetLayerIndex = currentLayerIndex + 1
        return true
    }

    fun consumeTargetLayerIndex(): Int? {
        val target = pendingTargetLayerIndex
        pendingTargetLayerIndex = null
        return target
    }

    fun reset() {
        pendingTargetLayerIndex = null
    }
}
