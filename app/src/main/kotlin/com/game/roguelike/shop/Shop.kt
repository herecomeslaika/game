package com.game.roguelike.shop

import com.game.roguelike.core.Game
import com.game.roguelike.entity.Player
import kotlin.random.Random

enum class ShopItemRarity {
    COMMON, RARE, EPIC
}

data class ShopItem(
    val name: String,
    val description: String,
    val baseCost: Int,
    val rarity: ShopItemRarity,
    val applyEffect: (Player, Game) -> Unit
) {
    var sold = false
    var cost = baseCost
}

class Shop {
    var items = listOf<ShopItem>()
    var isOpen = false
    var purchaseFailedTimer = 0f

    fun open(currentGold: Int, layerIndex: Int) {
        isOpen = true
        generateItems(layerIndex)
    }

    fun close() {
        isOpen = false
    }

    private fun generateItems(layerIndex: Int) {
        val priceMultiplier = 1f + layerIndex * 0.3f
        val pool = buildPool()
        val selected = selectFromPool(pool, 5)
        items = selected.map { item ->
            val scaledCost = (item.baseCost * priceMultiplier).toInt()
            item.copy(baseCost = item.baseCost).also { it.cost = scaledCost; it.sold = false }
        }
    }

    private fun selectFromPool(pool: List<ShopItem>, count: Int): List<ShopItem> {
        val byRarity = pool.groupBy { it.rarity }
        val result = mutableListOf<ShopItem>()

        // Guarantee at least 1 from each rarity tier
        for (rarity in ShopItemRarity.entries) {
            val tier = byRarity[rarity] ?: continue
            if (tier.isNotEmpty() && result.size < count) {
                result.add(tier.random(Random))
            }
        }

        // Fill remaining slots from full pool
        val remaining = pool.filter { item -> result.none { it.name == item.name } }
        while (result.size < count && remaining.isNotEmpty()) {
            val pick = remaining.random(Random)
            result.add(pick)
        }

        return result
    }

    private fun buildPool(): List<ShopItem> {
        return commonItems() + rareItems() + epicItems()
    }

    private fun commonItems() = listOf(
        ShopItem("生命药水", "恢复30点生命", 30, ShopItemRarity.COMMON) { p, _ ->
            p.health = (p.health + 30).coerceAtMost(p.maxHealth)
        },
        ShopItem("小生命药水", "恢复15点生命", 15, ShopItemRarity.COMMON) { p, _ ->
            p.health = (p.health + 15).coerceAtMost(p.maxHealth)
        },
        ShopItem("生命上限+25", "+25最大生命", 60, ShopItemRarity.COMMON) { p, _ ->
            p.maxHealth += 25; p.health += 25
        },
        ShopItem("攻击+5", "+5基础伤害", 50, ShopItemRarity.COMMON) { p, _ ->
            p.attackDamage1 += 5f; p.attackDamage2 += 5f; p.attackDamage3 += 5f
        },
        ShopItem("速度+30", "+30移动速度", 40, ShopItemRarity.COMMON) { p, _ ->
            p.speed += 30f
        },
        ShopItem("冲刺冷却-0.1s", "冲刺更频繁", 45, ShopItemRarity.COMMON) { p, _ ->
            p.dashCooldown = (p.dashCooldown - 0.1f).coerceAtLeast(0.15f)
        },
        ShopItem("飞刀伤害+5", "特殊攻击更强", 40, ShopItemRarity.COMMON) { p, _ ->
            p.specialDamage += 5f
        }
    )

    private fun rareItems() = listOf(
        ShopItem("生命上限+50", "+50最大生命", 90, ShopItemRarity.RARE) { p, _ ->
            p.maxHealth += 50; p.health += 50
        },
        ShopItem("攻击+10", "+10基础伤害", 100, ShopItemRarity.RARE) { p, _ ->
            p.attackDamage1 += 10f; p.attackDamage2 += 10f; p.attackDamage3 += 10f
        },
        ShopItem("暴击率+15%", "更容易暴击", 110, ShopItemRarity.RARE) { p, _ ->
            p.critChance += 0.15f
        },
        ShopItem("暴击倍率+0.5", "暴击伤害更高", 120, ShopItemRarity.RARE) { p, _ ->
            p.critMultiplier += 0.5f
        },
        ShopItem("冲刺伤害+10", "冲刺时造成伤害", 80, ShopItemRarity.RARE) { p, _ ->
            p.dashDamage += 10f
        },
        ShopItem("飞刀数量+1", "多投一把飞刀", 130, ShopItemRarity.RARE) { p, _ ->
            p.knifeCount += 1
        },
        ShopItem("冰霜减速+0.5s", "命中减速敌人", 90, ShopItemRarity.RARE) { p, _ ->
            p.slowOnHit += 0.5f
        },
        ShopItem("击杀回血+5", "击杀恢复生命", 100, ShopItemRarity.RARE) { p, _ ->
            p.killHealAmount += 5
        }
    )

    private fun epicItems() = listOf(
        ShopItem("生命上限+100", "+100最大生命", 160, ShopItemRarity.EPIC) { p, _ ->
            p.maxHealth += 100; p.health += 100
        },
        ShopItem("攻击+15", "+15基础伤害", 180, ShopItemRarity.EPIC) { p, _ ->
            p.attackDamage1 += 15f; p.attackDamage2 += 15f; p.attackDamage3 += 15f
        },
        ShopItem("连击必暴", "连击必定暴击", 200, ShopItemRarity.EPIC) { p, _ ->
            p.comboAlwaysCrit = true
        },
        ShopItem("雷电链弹", "命中弹射闪电", 200, ShopItemRarity.EPIC) { p, _ ->
            p.lightningBounce = true
        },
        ShopItem("雷霆飞刀", "飞刀爆炸", 220, ShopItemRarity.EPIC) { p, _ ->
            p.knifeExplosive = true
        },
        ShopItem("雅典娜之盾", "格挡一次攻击", 180, ShopItemRarity.EPIC) { p, _ ->
            p.athenaShieldActive = true
        },
        ShopItem("全伤害+10", "所有伤害提升", 200, ShopItemRarity.EPIC) { p, _ ->
            p.allDamageBonus += 10f
        }
    )
}
