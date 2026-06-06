package com.game.roguelike.audio

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class AudioTriggerPolicyTest {

    @Test
    fun `button taps do not play click sounds`() {
        val gameSource = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertFalse(gameSource.contains("\"ui_click\""))
    }

    @Test
    fun `cooldowns do not play ready sounds`() {
        val playerSource = readSource("src/main/kotlin/com/game/roguelike/entity/Player.kt")

        assertFalse(playerSource.contains("\"cooldown_ready\""))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
