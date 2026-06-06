package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class GameStoryFlowPolicyTest {

    @Test
    fun `menu uses main background music`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertTrue(gameSource.contains("R.raw.bgm_main"))
    }

    @Test
    fun `start button opens skippable intro before run starts`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertTrue(gameSource.contains("GameState.INTRO_STORY"))
        assertTrue(gameSource.contains("startIntroStory()"))
        assertTrue(gameSource.contains("skipIntroStory()"))
    }

    @Test
    fun `victory opens skippable ending story before returning to menu`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertTrue(gameSource.contains("GameState.ENDING_STORY"))
        assertTrue(gameSource.contains("skipEndingStory()"))
        assertTrue(gameSource.contains("fun skipEndingStory() {\r\n        enterMainMenu()") ||
            gameSource.contains("fun skipEndingStory() {\n        enterMainMenu()"))
    }

    @Test
    fun `player death opens skippable failure story before returning to menu`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertTrue(gameSource.contains("GameState.FAILURE_STORY"))
        assertTrue(gameSource.contains("skipFailureStory()"))
        assertTrue(gameSource.contains("gameState = GameState.FAILURE_STORY"))
    }

    @Test
    fun `story screens have dedicated renderers`() {
        val screenSource = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")

        assertTrue(screenSource.contains("renderIntroStory"))
        assertTrue(screenSource.contains("renderEndingStory"))
        assertTrue(screenSource.contains("renderFailureStory"))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
