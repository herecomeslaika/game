package com.game.roguelike.core

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class BossRelicFlowPolicyTest {

    @Test
    fun `game tracks boss relic drops and pickup flow`() {
        val source = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")

        assertTrue(source.contains("val bossRelics = mutableListOf<BossRelic>()"))
        assertTrue(source.contains("spawnBossRelic(enemy)"))
        assertTrue(source.contains("updateBossRelics(dt)"))
        assertTrue(source.contains("tryPickupNearbyBossRelic()"))
        assertTrue(source.contains("GameState.BOSS_RELIC_STORY"))
        assertTrue(source.contains("renderBossRelicStory"))
        assertTrue(source.contains("renderer.renderBossRelic(canvas, relic,"))
    }

    @Test
    fun `boss relic pickup always opens story without old blessing popup behavior`() {
        val source = readSource("src/main/kotlin/com/game/roguelike/core/Game.kt")
        val applyBody = source.substringAfter("private fun applyBossRelic").substringBefore("\n    fun spawnMeteorMark")
        val storyTouchBody = source.substringAfter("GameState.BOSS_RELIC_STORY -> {").substringBefore("}")

        assertTrue(applyBody.contains("gameState = GameState.BOSS_RELIC_STORY"))
        assertTrue(applyBody.contains("pendingBossRelicStoryType = type"))
        assertTrue(!applyBody.contains("player.hasBossRelic(type)) return"))
        assertTrue(!applyBody.contains("audioManager.play(\"blessing\")"))
        assertTrue(storyTouchBody.contains("storyTimer < 0.25f"))
    }

    @Test
    fun `titan relic changes special projectile to molten fireball`() {
        val playerSource = readSource("src/main/kotlin/com/game/roguelike/entity/Player.kt")
        val rendererSource = readSource("src/main/kotlin/com/game/roguelike/rendering/EntityRenderer.kt")
        val audioSource = readSource("src/main/kotlin/com/game/roguelike/audio/AudioManager.kt")

        assertTrue(playerSource.contains("moltenStrike"))
        assertTrue(playerSource.contains("ProjectileType.FIREBALL"))
        assertTrue(playerSource.contains("specialDamage + allDamageBonus) * 1.5f"))
        assertTrue(playerSource.contains("game.audioManager.play(\"fireball\")"))
        assertTrue(rendererSource.contains("ProjectileType.FIREBALL"))
        assertTrue(audioSource.contains("\"fireball\" to R.raw.huoqiu_fire"))
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)))
    }
}
