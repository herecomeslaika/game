package com.game.roguelike.shop

import com.game.roguelike.core.Game
import com.game.roguelike.entity.Player

data class ShopItem(
    val name: String,
    val description: String,
    val cost: Int,
    val applyEffect: (Player, Game) -> Unit
)

class Shop {
    var items = listOf<ShopItem>()
    var isOpen = false

    @Suppress("UNUSED_PARAMETER")
    fun open(currentGold: Int) {
        isOpen = true
        generateItems()
    }

    fun close() {
        isOpen = false
    }

    private fun generateItems() {
        items = listOf(
            ShopItem("生命药水", "恢复30点生命值", 30) { p, _ ->
                p.health = (p.health + 30).coerceAtMost(p.maxHealth)
            },
            ShopItem("生命上限提升", "+25最大生命", 80) { p, _ ->
                p.maxHealth += 25
                p.health += 25
            },
            ShopItem("攻击提升", "+10基础伤害", 100) { p, _ ->
                p.attackDamage1 += 10f
                p.attackDamage2 += 10f
                p.attackDamage3 += 10f
                p.specialDamage += 10f
            },
            ShopItem("暴击提升", "+15%暴击率", 120) { p, _ ->
                p.critChance += 0.15f
            }
        )
    }
}
