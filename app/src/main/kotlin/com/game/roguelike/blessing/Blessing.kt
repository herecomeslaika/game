package com.game.roguelike.blessing

import com.game.roguelike.core.BlessingType
import com.game.roguelike.core.Game
import com.game.roguelike.entity.Player

enum class BlessingRarity {
    COMMON, RARE, EPIC
}

data class Blessing(
    val name: String,
    val type: BlessingType,
    val description: String,
    val rarity: BlessingRarity,
    val applyTo: (Player, Game) -> Unit
)

object BlessingData {
    val all = listOf(
        // ATTACK
        Blessing("迅捷打击", BlessingType.ATTACK, "攻击速度+30%", BlessingRarity.COMMON) { p, _ ->
            p.attackSpeedMultiplier += 0.3f
        },
        Blessing("血色锋刃", BlessingType.ATTACK, "连招伤害+50%", BlessingRarity.RARE) { p, _ ->
            p.attackDamage1 *= 1.3f; p.attackDamage2 *= 1.5f; p.attackDamage3 *= 1.5f
        },
        Blessing("连锁斩击", BlessingType.ATTACK, "连招命中溅射周围敌人", BlessingRarity.EPIC) { p, _ ->
            p.comboSplashRadius = 60f
        },
        Blessing("重击", BlessingType.ATTACK, "普攻伤害+40%", BlessingRarity.COMMON) { p, _ ->
            p.attackDamage1 *= 1.4f; p.attackDamage2 *= 1.4f; p.attackDamage3 *= 1.4f
        },
        // SPECIAL
        Blessing("三重飞刀", BlessingType.SPECIAL, "一次投掷3把飞刀", BlessingRarity.RARE) { p, _ ->
            p.knifeCount = 3
        },
        Blessing("穿透之刃", BlessingType.SPECIAL, "飞刀穿透敌人", BlessingRarity.COMMON) { p, _ ->
            p.knifePierce = true
        },
        Blessing("爆裂飞刀", BlessingType.SPECIAL, "飞刀命中后爆炸", BlessingRarity.EPIC) { p, _ ->
            p.knifeExplosive = true
        },
        Blessing("速射", BlessingType.SPECIAL, "飞刀冷却-40%", BlessingRarity.COMMON) { p, _ ->
            p.specialCooldown *= 0.6f
        },
        // DASH
        Blessing("疾步冲刺", BlessingType.DASH, "冲刺冷却-50%", BlessingRarity.COMMON) { p, _ ->
            p.dashCooldown *= 0.5f
        },
        Blessing("冲刺打击", BlessingType.DASH, "冲刺时造成伤害", BlessingRarity.RARE) { p, _ ->
            p.dashDamage = 8f
        },
        Blessing("残影", BlessingType.DASH, "冲刺留下伤害轨迹", BlessingRarity.EPIC) { p, _ ->
            p.dashTrailDamage = 5f
        },
        Blessing("长距冲刺", BlessingType.DASH, "冲刺距离+50%", BlessingRarity.COMMON) { p, _ ->
            p.dashDuration *= 1.5f
        },
        // SUPPORT
        Blessing("宙斯之雷", BlessingType.SUPPORT, "每8秒闪电攻击最近敌人", BlessingRarity.RARE) { p, g ->
            p.supportCooldown = 8f
        },
        Blessing("雅典娜之盾", BlessingType.SUPPORT, "每10秒自动抵挡一次攻击", BlessingRarity.EPIC) { p, _ ->
            p.athenaShieldActive = true
        },
        Blessing("战神之怒", BlessingType.SUPPORT, "击杀后+20%伤害5秒", BlessingRarity.RARE) { p, _ ->
            p.warCryDamageBonus = p.attackDamage1 * 0.2f
        },
        Blessing("治愈恩典", BlessingType.SUPPORT, "恢复30%生命值", BlessingRarity.COMMON) { p, _ ->
            p.health = (p.health + (p.maxHealth * 0.3f).toInt()).coerceAtMost(p.maxHealth)
        },
    )

    fun getForLayer(layerIndex: Int): List<Blessing> {
        return when (layerIndex) {
            0 -> all.filter { it.rarity != BlessingRarity.EPIC }
            1 -> all
            2 -> all
            else -> all
        }
    }
}