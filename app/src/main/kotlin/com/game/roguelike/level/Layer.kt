package com.game.roguelike.level

import com.game.roguelike.core.RoomType

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

            // Entry room
            rooms.add(Room(20, 14, RoomType.ENTRY, layerIndex))

            // Middle rooms: combat + reward + shop
            for (i in 1 until roomCount - 1) {
                val type = when {
                    i == 2 -> RoomType.REWARD
                    i == 4 && layerIndex > 0 -> RoomType.SHOP
                    else -> RoomType.COMBAT
                }
                rooms.add(Room(20, 14, type, layerIndex))
            }

            // Boss room
            rooms.add(Room(24, 16, RoomType.BOSS, layerIndex))

            return Layer(rooms, layerIndex)
        }
    }
}