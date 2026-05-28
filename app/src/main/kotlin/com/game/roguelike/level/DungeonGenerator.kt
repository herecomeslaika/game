package com.game.roguelike.level

import com.game.roguelike.core.RoomType
import kotlin.random.Random

data class RoomNode(
    val id: Int,
    val type: RoomType,
    val x: Int,
    val y: Int,
    val connections: MutableList<Int> = mutableListOf(),
    var isHidden: Boolean = false,
    var isVisited: Boolean = false
)

data class FloorMap(
    val rooms: MutableList<RoomNode> = mutableListOf(),
    var startRoomId: Int = 0,
    var bossRoomId: Int = 0
) {
    fun getRoom(id: Int): RoomNode? = rooms.find { it.id == id }
    fun getConnectedRooms(fromId: Int): List<RoomNode> {
        val from = getRoom(fromId) ?: return emptyList()
        return from.connections.mapNotNull { getRoom(it) }
    }
    fun getVisibleConnectedRooms(fromId: Int): List<RoomNode> {
        return getConnectedRooms(fromId).filter { !it.isHidden || it.isVisited }
    }
}

object DungeonGenerator {

    fun generateFloor(floorNum: Int): FloorMap {
        val depth = 5 + minOf(floorNum, 3)
        val rng = Random(floorNum * 7919 + 42)

        val map = FloorMap()
        var nextId = 0

        // Build layers: each layer has 1-3 rooms
        val layers = mutableListOf<List<Int>>()

        // Layer 0: start room (always single)
        val startId = nextId++
        map.rooms.add(RoomNode(startId, RoomType.ENTRY, 0, 0))
        layers.add(listOf(startId))

        // Middle layers: branching
        for (layer in 1 until depth - 1) {
            val count = when {
                layer <= 2 -> rng.nextInt(1, 3) // early: 1-2 rooms
                layer >= depth - 2 -> rng.nextInt(1, 3) // late: 1-2 rooms (converge)
                else -> rng.nextInt(2, 4) // middle: 2-3 rooms
            }

            val layerRoomIds = mutableListOf<Int>()
            for (i in 0 until count) {
                val type = pickRoomType(rng, layer, depth, floorNum)
                val id = nextId++
                map.rooms.add(RoomNode(id, type, i, layer))
                layerRoomIds.add(id)
            }
            layers.add(layerRoomIds)
        }

        // Last layer: boss (always single)
        val bossId = nextId++
        map.rooms.add(RoomNode(bossId, RoomType.BOSS, 0, depth - 1))
        layers.add(listOf(bossId))

        map.startRoomId = startId
        map.bossRoomId = bossId

        // Connect layers: each room in layer[i] connects to 1-2 rooms in layer[i+1]
        for (li in 0 until layers.size - 1) {
            val current = layers[li]
            val next = layers[li + 1]

            // Ensure every room in next layer has at least one incoming connection
            val hasIncoming = mutableSetOf<Int>()

            for (roomId in current) {
                val room = map.getRoom(roomId)!!
                // Pick 1-2 connections to next layer
                val connCount = if (next.size == 1) 1 else rng.nextInt(1, minOf(3, next.size + 1))
                val targets = next.shuffled(rng).take(connCount)
                for (t in targets) {
                    if (roomId !in map.getRoom(t)!!.connections) {
                        room.connections.add(t)
                        hasIncoming.add(t)
                    }
                }
            }

            // Fix any orphaned rooms in next layer
            for (t in next) {
                if (t !in hasIncoming) {
                    // Connect from a random room in current layer
                    val from = current.random(rng)
                    map.getRoom(from)!!.connections.add(t)
                }
            }
        }

        // Add hidden rooms (secret chambers)
        val hiddenCount = if (rng.nextFloat() < 0.6f) 1 else 0
        for (h in 0 until hiddenCount) {
            // Pick a random non-start, non-boss room to branch from
            val eligible = map.rooms.filter { it.type != RoomType.ENTRY && it.type != RoomType.BOSS && it.connections.size < 3 }
            if (eligible.isNotEmpty()) {
                val parent = eligible.random(rng)
                val hiddenId = nextId++
                val hiddenRoom = RoomNode(
                    hiddenId,
                    if (rng.nextFloat() < 0.5f) RoomType.TREASURE else RoomType.HIDDEN,
                    parent.x + 1,
                    parent.y,
                    isHidden = true
                )
                map.rooms.add(hiddenRoom)
                parent.connections.add(hiddenId)
            }
        }

        return map
    }

    private fun pickRoomType(rng: Random, layer: Int, depth: Int, floorNum: Int): RoomType {
        // Weighted random based on position in floor
        val roll = rng.nextFloat()
        return when {
            layer == 1 && roll < 0.4f -> RoomType.COMBAT
            layer >= depth - 2 -> RoomType.COMBAT // always combat before boss
            roll < 0.35f -> RoomType.COMBAT
            roll < 0.50f -> RoomType.ELITE        // harder combat
            roll < 0.65f -> RoomType.REWARD
            roll < 0.75f -> RoomType.SHOP
            roll < 0.85f -> RoomType.EVENT         // random event
            roll < 0.93f -> RoomType.REST          // healing fountain
            else -> RoomType.COMBAT
        }
    }

    fun generateRoomFromNode(node: RoomNode, floorNum: Int): Room {
        val width = when (node.type) {
            RoomType.BOSS -> 20
            RoomType.ELITE -> 16
            RoomType.SHOP -> 14
            else -> 12 + (0..3).random()
        }
        val height = when (node.type) {
            RoomType.BOSS -> 20
            RoomType.ELITE -> 16
            RoomType.SHOP -> 14
            else -> 12 + (0..3).random()
        }

        return Room(width, height, node.type, floorNum)
    }
}