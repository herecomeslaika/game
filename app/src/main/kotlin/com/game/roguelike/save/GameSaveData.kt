package com.game.roguelike.save

data class GameSaveData(
    val version: Int,
    val layerIndex: Int,
    val roomId: Int,
    val roomCleared: Boolean,
    val playerHealth: Int,
    val playerMaxHealth: Int,
    val gold: Int,
    val blessingIds: List<String>,
    val state: String,
    val offeringIds: List<String>,
    val bossRelicIds: List<String> = emptyList(),
    val droppedBossRelics: List<String> = emptyList()
)
