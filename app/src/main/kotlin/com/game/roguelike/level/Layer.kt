package com.game.roguelike.level

class Layer(
    val floorNum: Int,
    val floorMap: FloorMap = DungeonGenerator.generateFloor(floorNum)
) {
    val rooms = mutableMapOf<Int, Room>()
    var currentRoomId: Int = floorMap.startRoomId

    init {
        // Generate the starting room immediately
        generateRoomIfNeeded(currentRoomId)
    }

    fun getCurrentRoom(): Room {
        generateRoomIfNeeded(currentRoomId)
        return rooms[currentRoomId]!!
    }

    fun getConnectedRooms(): List<RoomNode> {
        return floorMap.getVisibleConnectedRooms(currentRoomId)
    }

    fun goToRoom(targetId: Int): Room {
        val node = floorMap.getRoom(targetId) ?: return getCurrentRoom()
        currentRoomId = targetId
        node.isVisited = true
        generateRoomIfNeeded(targetId)
        return rooms[targetId]!!
    }

    fun isBossRoom(): Boolean {
        return currentRoomId == floorMap.bossRoomId
    }

    fun allBossCleared(): Boolean {
        val bossRoom = floorMap.getRoom(floorMap.bossRoomId)
        return bossRoom != null && rooms[floorMap.bossRoomId]?.cleared == true
    }

    private fun generateRoomIfNeeded(roomId: Int) {
        if (rooms.containsKey(roomId)) return
        val node = floorMap.getRoom(roomId) ?: return
        val room = DungeonGenerator.generateRoomFromNode(node, floorNum)
        room.roomId = roomId
        rooms[roomId] = room
    }
}