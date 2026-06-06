package com.game.roguelike.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import com.game.roguelike.util.Vector2

// 同步帧数据，所有需要同步的操作都封装在这里
@Serializable
sealed class NetworkFrame

// 玩家输入帧
@Serializable
data class PlayerInputFrame(
    val playerId: Int,
    @Contextual val moveDir: Vector2,
    val isAttacking: Boolean,
    val attackType: Int,
    val isDashing: Boolean,
    val timestamp: Long
) : NetworkFrame()

// 玩家状态同步
@Serializable
data class PlayerStateSync(
    val playerId: Int,
    @Contextual val position: Vector2,
    val facingRight: Boolean,
    val health: Int,
    val maxHealth: Int,
    val attack: Int,
    val blessingCount: Int,
    val gold: Int,
    val isDead: Boolean
) : NetworkFrame()

// 敌人状态同步
@Serializable
data class EnemyStateSync(
    val enemyId: Int,
    @Contextual val position: Vector2,
    val health: Int,
    val state: Int,
    val facingRight: Boolean
) : NetworkFrame()

// 房间信息
@Serializable
data class RoomInfo(
    val roomId: String,
    val roomCode: String = "",    // 4位房间密码
    val hostIp: String,
    val playerCount: Int,
    val maxPlayers: Int = 4,
    val roomName: String
)

// 聊天/快捷发言
@Serializable
data class QuickChat(
    val playerId: Int,
    val message: String
) : NetworkFrame()

// 等待房间中的玩家信息
@Serializable
data class LobbyPlayer(
    val playerId: Int,
    val name: String,
    val isReady: Boolean = false,
    val isHost: Boolean = false
)

// 房间操作指令
@Serializable
data class RoomCommand(
    val type: CommandType,
    val data: String = ""
) : NetworkFrame() {
    enum class CommandType {
        START_GAME,
        PLAYER_JOIN,
        PLAYER_LEAVE,
        PLAYER_READY,
        KICK_PLAYER,
        ROOM_INFO,          // 房间信息同步（房间码、玩家列表）
        REVIVE_PLAYER,
        PICKUP_ITEM,
        ROOM_COMPLETE
    }
}
