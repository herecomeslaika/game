package com.game.roguelike.event

import com.game.roguelike.entity.Player

data class GameEvent(
    val id: String,
    val title: String,
    val description: String,
    val npcName: String,
    val npcColor: Int,
    val options: List<EventOption>
)

data class EventOption(
    val text: String,
    val costDescription: String,
    val rewardDescription: String,
    val effect: EventEffect
)

sealed class EventEffect {
    data class LoseHpGiveGold(val hpPercent: Float, val gold: Int) : EventEffect()
    data class LoseGoldGiveBlessing(val gold: Int) : EventEffect()
    data class LoseMaxHpGiveDamage(val maxHpPercent: Float, val damageBonus: Float) : EventEffect()
    data class GambleGold(val stakePercent: Float, val multiplier: Float) : EventEffect()
    data class RestHeal(val healPercent: Float) : EventEffect()
    data class RandomBlessingOrCurse(val blessingChance: Float) : EventEffect()
    data class UpgradeWeapon(val damageBonus: Float) : EventEffect()
    data class LoseHpGiveBlessing(val hpPercent: Float) : EventEffect()
    data class SpendGoldGiveDamage(val gold: Int, val damageBonus: Float) : EventEffect()
    data class FateWheel(val outcomes: List<FateOutcome>) : EventEffect()
    data class LoseAllGoldHealAndDamage(val healPercent: Float, val damageBonus: Float) : EventEffect()
    data class CoinFlipBlessing(val gold: Int, val chance: Float) : EventEffect()
    data class AlchemistExperiment(val hpCost: Float, val successHeal: Float) : EventEffect()
}

data class FateOutcome(
    val description: String,
    val apply: (Player) -> Unit
)
