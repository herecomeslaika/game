package com.game.roguelike.network

object LobbyRules {
    fun createHostPlayer(): LobbyPlayer {
        return LobbyPlayer(playerId = 1, name = "房主", isReady = true, isHost = true)
    }

    fun createGuestPlayer(playerId: Int): LobbyPlayer {
        return LobbyPlayer(playerId = playerId, name = "玩家$playerId", isReady = false, isHost = false)
    }

    fun canStartGame(players: List<LobbyPlayer>): Boolean {
        val hostReady = players.any { it.isHost && it.isReady }
        val readyGuestCount = players.count { !it.isHost && it.isReady }
        return hostReady && readyGuestCount >= 1
    }

    fun syncPlayerCount(roomInfo: RoomInfo, players: List<LobbyPlayer>): RoomInfo {
        return roomInfo.copy(playerCount = players.size)
    }
}
