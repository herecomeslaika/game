package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class MenuReturnPolicyTest {

    @Test
    fun `menu start button uses stable opaque color`() {
        val source = readSource("src/main/kotlin/com/game/roguelike/rendering/ScreenRenderer.kt")
        val menuBody = source.substringAfter("fun renderMenu")
            .substringBefore("\n    fun renderMultiplayerLobby")

        assertTrue(menuBody.contains("START_BUTTON_COLOR"))
        assertFalse(menuBody.contains("Color.argb(alpha, 255, 215, 0)"))
    }

    @Test
    fun `returning to menu keeps a single looping main bgm`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val audioSource = readSource("src/main/kotlin/com/game/roguelike/audio/AudioManager.kt")
        val enterMenuBody = gameSource.substringAfter("private fun enterMainMenu()")
            .substringBefore("\n    fun selectBlessing")

        assertFalse(audioSource.contains("forceRestart"))
        assertTrue(enterMenuBody.contains("playBgm(context, R.raw.bgm_main)"))
        assertFalse(enterMenuBody.contains("forceRestart"))
    }

    @Test
    fun `game loop start is idempotent`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val startBody = gameSource.substringAfter("fun start(holder: SurfaceHolder)")
            .substringBefore("\n    fun stop()")

        assertTrue(gameSource.contains("@Volatile\r\n    private var running") ||
            gameSource.contains("@Volatile\n    private var running"))
        assertTrue(gameSource.contains("@Synchronized\r\n    fun start") ||
            gameSource.contains("@Synchronized\n    fun start"))
        assertTrue(gameSource.contains("@Synchronized\r\n    fun stop") ||
            gameSource.contains("@Synchronized\n    fun stop"))
        assertTrue(startBody.contains("if (running) return"))
    }

    @Test
    fun `bgm player operations are synchronized to prevent overlapping players`() {
        val audioSource = readSource("src/main/kotlin/com/game/roguelike/audio/AudioManager.kt")

        listOf("playBgm", "stopBgm", "pauseBgm", "resumeBgm").forEach { functionName ->
            assertTrue(
                audioSource.contains("@Synchronized\r\n    fun $functionName") ||
                    audioSource.contains("@Synchronized\n    fun $functionName"),
                "$functionName should be synchronized"
            )
        }
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
