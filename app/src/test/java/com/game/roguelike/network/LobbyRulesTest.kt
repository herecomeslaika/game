package com.game.roguelike.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LobbyRulesTest {

    @Test
    fun `host is ready by default when creating a room`() {
        val host = LobbyRules.createHostPlayer()

        assertEquals(1, host.playerId)
        assertEquals("房主", host.name)
        assertTrue(host.isHost)
        assertTrue(host.isReady)
    }

    @Test
    fun `game can start when host is ready and one guest is ready`() {
        val players = listOf(
            LobbyRules.createHostPlayer(),
            LobbyPlayer(playerId = 2, name = "玩家2", isReady = true, isHost = false)
        )

        assertTrue(LobbyRules.canStartGame(players))
    }

    @Test
    fun `game cannot start before a ready guest joins`() {
        assertFalse(LobbyRules.canStartGame(listOf(LobbyRules.createHostPlayer())))

        val players = listOf(
            LobbyRules.createHostPlayer(),
            LobbyPlayer(playerId = 2, name = "玩家2", isReady = false, isHost = false)
        )
        assertFalse(LobbyRules.canStartGame(players))
    }

    @Test
    fun `room info mirrors current player count`() {
        val info = RoomInfo(
            roomId = "room",
            roomCode = "1234",
            hostIp = "192.168.1.2",
            playerCount = 1,
            maxPlayers = 4,
            roomName = "测试房间"
        )
        val players = listOf(
            LobbyRules.createHostPlayer(),
            LobbyPlayer(playerId = 2, name = "玩家2")
        )

        assertEquals(2, LobbyRules.syncPlayerCount(info, players).playerCount)
    }
}
