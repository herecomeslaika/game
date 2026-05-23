package com.game.roguelike.blessing

import kotlin.random.Random

class BlessingSelector {

    var currentOffering: List<Blessing> = listOf()
        private set

    fun generateOffering(layerIndex: Int) {
        val pool = BlessingData.getForLayer(layerIndex)
        val shuffled = pool.shuffled(Random)
        currentOffering = shuffled.take(3)
    }

    fun select(index: Int): Blessing? {
        return currentOffering.getOrNull(index)
    }
}
