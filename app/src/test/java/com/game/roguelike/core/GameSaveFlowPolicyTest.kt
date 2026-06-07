package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class GameSaveFlowPolicyTest {

    @Test
    fun `start button asks whether to continue when a save exists`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val stateSource = readSource("src/main/kotlin/com/game/roguelike/core/GameState.kt")
        val screenSource = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")

        assertTrue(stateSource.contains("LOAD_SAVE_CONFIRM"))
        assertTrue(gameSource.contains("saveManager.hasSave()"))
        assertTrue(gameSource.contains("gameState = GameState.LOAD_SAVE_CONFIRM"))
        assertTrue(gameSource.contains("continueFromSave()"))
        assertTrue(gameSource.contains("deleteSave()"))
        assertTrue(screenSource.contains("发现存档"))
        assertTrue(screenSource.contains("从上次存档继续？"))
    }

    @Test
    fun `returning from gameplay asks whether to save before going to menu`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val stateSource = readSource("src/main/kotlin/com/game/roguelike/core/GameState.kt")
        val screenSource = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")

        assertTrue(stateSource.contains("SAVE_GAME_CONFIRM"))
        assertTrue(gameSource.contains("gameState = GameState.SAVE_GAME_CONFIRM"))
        assertTrue(gameSource.contains("saveCurrentRun()"))
        assertTrue(screenSource.contains("保存进度？"))
        assertTrue(screenSource.contains("保存当前游戏进度吗"))
    }

    @Test
    fun `activity pause auto saves active gameplay`() {
        val activitySource = readSource("src/main/kotlin/com/game/roguelike/MainActivity.kt")
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertTrue(activitySource.contains("autoSaveIfNeeded()"))
        assertTrue(gameSource.contains("fun autoSaveIfNeeded()"))
        assertTrue(gameSource.contains("isSaveableGameplayState()"))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
