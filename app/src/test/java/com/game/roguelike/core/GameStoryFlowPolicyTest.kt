package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertFalse
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
    fun `intro continues to seven gods blessing story before run starts`() {
        val gameStateSource = readSource("src/main/kotlin/com/game/roguelike/core/GameState.kt")
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val screenSource = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")
        val introStorySource = screenSource.substringAfter("fun renderIntroStory").substringBefore("fun renderBlessingStory")
        val blessingStorySource = screenSource.substringAfter("fun renderBlessingStory").substringBefore("fun renderEndingStory")

        assertTrue(gameStateSource.contains("BLESSING_STORY"))
        assertTrue(gameSource.contains("GameState.BLESSING_STORY"))
        assertTrue(gameSource.contains("continueIntroStory()"))
        assertTrue(gameSource.contains("gameState = GameState.BLESSING_STORY"))
        assertTrue(gameSource.contains("skipBlessingStory()"))
        assertTrue(screenSource.contains("renderBlessingStory"))
        assertTrue(introStorySource.contains("drawStoryButton(canvas, storyNextBtnRect, \"下一页\""))
        assertTrue(introStorySource.contains("drawStoryButton(canvas, storySkipBtnRect, \"跳过\""))
        assertTrue(screenSource.contains("宙斯"))
        assertTrue(screenSource.contains("阿佛洛狄忒"))
        assertTrue(screenSource.contains("雅典娜"))
        assertTrue(screenSource.contains("阿瑞斯"))
        assertTrue(screenSource.contains("赫尔墨斯"))
        assertTrue(screenSource.contains("波塞冬"))
        assertTrue(screenSource.contains("哈迪斯"))
        assertTrue(blessingStorySource.contains("点击任意位置进入游戏"))
        assertFalse(blessingStorySource.contains("drawStoryButton(canvas, storyNextBtnRect"))
        assertFalse(blessingStorySource.contains("drawStoryButton(canvas, storySkipBtnRect"))
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
        assertTrue(screenSource.contains("renderBlessingStory"))
        assertTrue(screenSource.contains("renderEndingStory"))
        assertTrue(screenSource.contains("renderFailureStory"))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
