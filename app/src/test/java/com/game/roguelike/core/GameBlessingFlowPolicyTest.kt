package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class GameBlessingFlowPolicyTest {

    @Test
    fun `boss room clear rewards require the entrance-spawned boss fight to have started`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val clearBlock = gameSource.substringAfter("// Check room clear")
            .substringBefore("// Spike damage")

        assertTrue(clearBlock.contains("RoomType.COMBAT"))
        assertTrue(clearBlock.contains("RoomType.ELITE"))
        assertTrue(clearBlock.contains("RoomType.BOSS"))
        assertTrue(clearBlock.contains("bossFightStarted"))
    }

    @Test
    fun `entering boss room resets boss reward gate and starts entrance sequence`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val bossLoadBlock = gameSource.substringAfter("RoomType.BOSS -> {")
            .substringBefore("RoomType.EVENT -> {")

        assertTrue(bossLoadBlock.contains("GameState.BOSS_ENTRANCE"))
        assertTrue(bossLoadBlock.contains("bossFightStarted = false"))
        assertFalse(bossLoadBlock.contains("GameState.BLESSING_SELECT"))
        assertFalse(bossLoadBlock.contains("generateOffering"))
    }

    @Test
    fun `boss entrance marks boss fight as started only after the boss spawns`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val spawnBlock = gameSource.substringAfter("room.spawnBoss(this)")
            .substringBefore("gameState = GameState.PLAYING")

        assertTrue(spawnBlock.contains("bossFightStarted = true"))
    }

    @Test
    fun `blessing select layout is scaled down by twenty percent`() {
        val uiSource = readSource("src/main/kotlin/com/game/roguelike/ui/BlessingSelectUI.kt")

        assertTrue(uiSource.contains("SIZE_SCALE = 0.8f"))
        assertTrue(uiSource.contains("panelTop = h * 0.156f"))
        assertTrue(uiSource.contains("panelBottom = h * 0.844f"))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
