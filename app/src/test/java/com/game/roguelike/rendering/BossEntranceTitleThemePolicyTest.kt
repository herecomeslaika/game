package com.game.roguelike.rendering

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class BossEntranceTitleThemePolicyTest {

    @Test
    fun `boss title reveal uses a distinct visual theme per layer`() {
        val source = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")

        assertTrue(source.contains("BossEntranceTitleTheme"))
        assertTrue(source.contains("TARTARUS_BOSS_TITLE_THEME"))
        assertTrue(source.contains("ASPHODEL_BOSS_TITLE_THEME"))
        assertTrue(source.contains("ELYSIUM_BOSS_TITLE_THEME"))
        assertTrue(source.contains("bossEntranceTitleTheme(layerIndex)"))
    }

    @Test
    fun `boss title reveal receives current layer index from game render`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val rendererSource = readSource("src/main/kotlin/com/game/roguelike/rendering/IsometricRenderer.kt")
        val screenSource = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")

        assertTrue(rendererSource.contains("fun renderBossEntrance(canvas: Canvas, bossName: String, bossTitle: String, timer: Float, phase: Int, w: Int, h: Int, layerIndex: Int)"))
        assertTrue(screenSource.contains("fun renderBossEntrance(canvas: Canvas, bossName: String, bossTitle: String, timer: Float, phase: Int, w: Int, h: Int, layerIndex: Int)"))
        assertTrue(gameSource.contains("renderer.renderBossEntrance(canvas, bossEntranceName, bossEntranceTitle, bossEntranceTimer, bossEntrancePhase, screenWidth, screenHeight, currentLayerIndex)"))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
