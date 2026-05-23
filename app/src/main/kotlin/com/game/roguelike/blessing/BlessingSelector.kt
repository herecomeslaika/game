package com.game.roguelike.blessing

import kotlin.random.Random

class BlessingSelector {

    var currentOffering: List<Blessing> = listOf()
        private set

    fun generateOffering(layerIndex: Int, ownedBlessings: List<Blessing> = emptyList()) {
        val pool = BlessingData.getForLayer(layerIndex).filter { blessing ->
            ownedBlessings.none { it.name == blessing.name }
        }
        val shuffled = pool.shuffled(Random)
        currentOffering = shuffled.take(3)
    }

    fun select(index: Int): Blessing? {
        return currentOffering.getOrNull(index)
    }
}
