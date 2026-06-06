package com.game.roguelike.rendering

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class MultiplayerScreenPolicyTest {

    @Test
    fun `multiplayer screens do not draw colored title bars`() {
        val source = readSource()

        listOf("renderMultiplayerLobby", "renderRoomList", "renderRoomWaiting").forEach { functionName ->
            val body = functionBody(source, functionName)
            assertFalse(body.contains("titleBarHeight"), "$functionName should not draw a colored title bar")
            assertFalse(body.contains("titleBgPaint"), "$functionName should not draw a colored title background")
        }
    }

    @Test
    fun `multiplayer screens do not render large page titles`() {
        val source = readSource()

        val lobbyBody = functionBody(source, "renderMultiplayerLobby")
        val roomListBody = functionBody(source, "renderRoomList")
        val waitingBody = functionBody(source, "renderRoomWaiting")

        assertFalse(lobbyBody.contains("联机大厅"))
        assertFalse(roomListBody.contains("可用房间"))
        assertFalse(waitingBody.contains("等待开始"))
    }

    @Test
    fun `waiting room has only one waiting-for-player hint`() {
        val source = readSource()

        assertTrue(source.contains("等待玩家加入房间"))
        assertFalse(source.contains("等待玩家加入..."))
    }

    @Test
    fun `room list and waiting room use compact layout markers`() {
        val source = readSource()

        assertTrue(source.contains("drawCompactRoomSearchEmptyState"))
        assertTrue(source.contains("drawCompactRoomSummary"))
    }

    private fun functionBody(source: String, functionName: String): String {
        return source.substringAfter("fun $functionName")
            .substringBefore("\n    fun ")
            .substringBefore("\n    private fun ")
    }

    private fun readSource(): String {
        return String(Files.readAllBytes(Paths.get("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")))
    }
}
