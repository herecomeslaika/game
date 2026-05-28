package com.game.roguelike.level

import com.game.roguelike.core.RoomType
import kotlin.random.Random

class Layer private constructor(
    val rooms: List<Room>,
    val index: Int
) {
    companion object {
        fun create(layerIndex: Int): Layer {
            val rooms = mutableListOf<Room>()
            val roomCount = when (layerIndex) {
                0 -> 6
                1 -> 6
                2 -> 5
                else -> 5
            }

            // Entry room (smaller)
            val entrySizes = listOf(Pair(16, 12), Pair(18, 14))
            val (ew, eh) = entrySizes[Random.nextInt(entrySizes.size)]
            rooms.add(Room(ew, eh, RoomType.ENTRY, layerIndex))

            // Middle rooms: combat + reward + shop
            for (i in 1 until roomCount - 1) {
                val type = when {
                    i == 2 -> RoomType.REWARD
                    i == 4 && layerIndex > 0 -> RoomType.SHOP
                    else -> RoomType.COMBAT
                }
                val (w, h) = when (type) {
                    RoomType.COMBAT -> {
                        val sizes = listOf(Pair(18, 12), Pair(20, 14), Pair(22, 16))
                        sizes[Random.nextInt(sizes.size)]
                    }
                    RoomType.REWARD -> {
                        val sizes = listOf(Pair(16, 12), Pair(18, 14))
                        sizes[Random.nextInt(sizes.size)]
                    }
                    RoomType.SHOP -> {
                        val sizes = listOf(Pair(16, 12), Pair(18, 14))
                        sizes[Random.nextInt(sizes.size)]
                    }
                    else -> Pair(20, 14)
                }
                rooms.add(Room(w, h, type, layerIndex))
            }

            // Boss room (larger)
            val bossSizes = listOf(Pair(24, 16), Pair(26, 18))
            val (bw, bh) = bossSizes[Random.nextInt(bossSizes.size)]
            rooms.add(Room(bw, bh, RoomType.BOSS, layerIndex))

            return Layer(rooms, layerIndex)
        }
    }
}