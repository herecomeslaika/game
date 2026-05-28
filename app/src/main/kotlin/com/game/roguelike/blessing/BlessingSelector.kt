package com.game.roguelike.blessing

import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.GodType
import kotlin.random.Random

class BlessingSelector {
    var currentOffering: List<Blessing> = emptyList()
        private set

    fun generateOffering(layerIndex: Int, ownedBlessings: List<Blessing>) {
        val ownedIds = ownedBlessings.map { it.id }.toSet()
        val ownedGods = ownedBlessings.map { it.god }.toSet()

        // Determine rarity pool based on layer
        val availableByRarity = when (layerIndex) {
            0 -> {
                val pool = mutableListOf<BlessingRarity>()
                repeat(3) { pool.add(BlessingRarity.COMMON) }
                pool.add(BlessingRarity.RARE)
                pool
            }
            1 -> {
                val pool = mutableListOf<BlessingRarity>()
                repeat(2) { pool.add(BlessingRarity.COMMON) }
                pool.add(BlessingRarity.RARE)
                pool.add(BlessingRarity.RARE)
                pool
            }
            2 -> {
                val pool = mutableListOf<BlessingRarity>()
                pool.add(BlessingRarity.COMMON)
                repeat(2) { pool.add(BlessingRarity.RARE) }
                pool.add(BlessingRarity.EPIC)
                pool
            }
            else -> listOf(BlessingRarity.COMMON)
        }

        // Pick 3 random blessings
        val candidates = mutableListOf<Blessing>()
        for (rarity in availableByRarity) {
            val pool = Blessing.ALL_BLESSINGS
                .filter { it.rarity == rarity && it.id !in ownedIds }
            if (pool.isNotEmpty()) {
                candidates.add(pool[Random.nextInt(pool.size)])
            }
        }

        // Fill to 3 if not enough from rarity pool
        while (candidates.size < 3) {
            val pool = Blessing.ALL_BLESSINGS.filter { it.id !in ownedIds && it.id !in candidates.map { c -> c.id } }
            if (pool.isEmpty()) break
            candidates.add(pool[Random.nextInt(pool.size)])
        }

        // 4th slot: god-affinity recommendation (blessing from a god you already have)
        val recommendation = Blessing.ALL_BLESSINGS
            .filter { it.id !in ownedIds && it.id !in candidates.map { c -> c.id } && it.god in ownedGods }
            .randomOrNull()
        if (recommendation != null) {
            candidates.add(recommendation)
        }

        // Check for available Duo blessings
        val availableDuos = Blessing.getAvailableDuo(ownedGods)
            .filter { it.id !in ownedIds }
        if (availableDuos.isNotEmpty()) {
            // Add one Duo blessing as extra option (replaces recommendation if slot 4)
            if (candidates.size > 3) {
                candidates[3] = availableDuos[Random.nextInt(availableDuos.size)]
            } else {
                candidates.add(availableDuos[Random.nextInt(availableDuos.size)])
            }
        }

        currentOffering = candidates.distinctBy { it.id }.take(4)
    }

    fun selectBlessing(index: Int): Blessing? {
        return if (index in currentOffering.indices) currentOffering[index] else null
    }
}
