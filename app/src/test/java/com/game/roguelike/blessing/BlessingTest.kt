package com.game.roguelike.blessing

import com.game.roguelike.core.BlessingRarity
import com.game.roguelike.core.GodType
import com.game.roguelike.entity.Player
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BlessingTest {

    @Test
    fun `getByGod returns only blessings for that god`() {
        val zeusBlessings = Blessing.getByGod(GodType.ZEUS)
        assertTrue(zeusBlessings.isNotEmpty())
        assertTrue(zeusBlessings.all { it.god == GodType.ZEUS })
    }

    @Test
    fun `getByGod for each god returns 4 blessings`() {
        for (god in GodType.entries) {
            val blessings = Blessing.getByGod(god)
            assertEquals(4, blessings.size, "Expected 4 blessings for $god")
        }
    }

    @Test
    fun `getByRarity filters correctly`() {
        val commons = Blessing.getByRarity(BlessingRarity.COMMON)
        assertTrue(commons.isNotEmpty())
        assertTrue(commons.all { it.rarity == BlessingRarity.COMMON })
    }

    @Test
    fun `getAvailableDuo requires both gods owned`() {
        val none = Blessing.getAvailableDuo(emptySet())
        assertTrue(none.isEmpty())

        val onlyZeus = Blessing.getAvailableDuo(setOf(GodType.ZEUS))
        assertTrue(onlyZeus.isEmpty())

        val zeusAphro = Blessing.getAvailableDuo(setOf(GodType.ZEUS, GodType.APHRODITE))
        assertTrue(zeusAphro.any { it.id == "duo_heart_lightning" })

        val zeusAthena = Blessing.getAvailableDuo(setOf(GodType.ZEUS, GodType.ATHENA))
        assertTrue(zeusAthena.any { it.id == "duo_thunder_shield" })
    }

    @Test
    fun `ALL_BLESSINGS contains 28 blessings (7 gods x 4)`() {
        assertEquals(28, Blessing.ALL_BLESSINGS.size)
    }

    @Test
    fun `ALL_DUO_BLESSINGS contains 7 duo blessings`() {
        assertEquals(7, Blessing.ALL_DUO_BLESSINGS.size)
    }

    @Test
    fun `all duo blessings have duoPair set`() {
        for (duo in Blessing.ALL_DUO_BLESSINGS) {
            assertNotNull(duo.duoPair, "Duo blessing ${duo.id} should have duoPair")
            assertEquals(BlessingRarity.DUO, duo.rarity)
        }
    }

    @Test
    fun `SWIFT_THUNDER reduces special cooldown`() {
        val player = Player()
        val originalCooldown = player.specialCooldown
        Blessing.SWIFT_THUNDER.onApply!!.invoke(player)
        assertTrue(player.specialCooldown < originalCooldown)
    }

    @Test
    fun `HEART_STRIKE increases attack damage`() {
        val player = Player()
        val originalDmg1 = player.attackDamage1
        Blessing.HEART_STRIKE.onApply!!.invoke(player)
        assertTrue(player.attackDamage1 > originalDmg1)
    }

    @Test
    fun `DIVINE_SHIELD activates shield`() {
        val player = Player()
        assertFalse(player.athenaShieldActive)
        Blessing.DIVINE_SHIELD.onApply!!.invoke(player)
        assertTrue(player.athenaShieldActive)
    }

    @Test
    fun `DEATH_GAZE sets low HP multiplier`() {
        val player = Player()
        assertEquals(1f, player.lowHpDamageMultiplier)
        Blessing.DEATH_GAZE.onApply!!.invoke(player)
        assertEquals(2f, player.lowHpDamageMultiplier)
    }

    @Test
    fun `UNDERWORLD_SUMMON enables summon`() {
        val player = Player()
        assertFalse(player.hasSummon)
        Blessing.UNDERWORLD_SUMMON.onApply!!.invoke(player)
        assertTrue(player.hasSummon)
    }
}