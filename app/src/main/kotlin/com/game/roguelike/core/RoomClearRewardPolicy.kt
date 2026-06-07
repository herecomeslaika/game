package com.game.roguelike.core

object RoomClearRewardPolicy {
    private const val FINAL_BOSS_LAYER_INDEX = 2

    fun canClearAfterCombat(roomType: RoomType, bossFightStarted: Boolean): Boolean {
        return roomType == RoomType.COMBAT ||
            roomType == RoomType.ELITE ||
            (roomType == RoomType.BOSS && bossFightStarted)
    }

    fun grantsBlessing(roomType: RoomType, layerIndex: Int, bossFightStarted: Boolean): Boolean {
        return when (roomType) {
            RoomType.COMBAT, RoomType.ELITE -> true
            RoomType.BOSS -> false
            else -> false
        }
    }
}
