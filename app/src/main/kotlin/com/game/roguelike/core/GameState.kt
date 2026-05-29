package com.game.roguelike.core

enum class GameState {
    MENU,
    MULTIPLAYER_LOBBY,
    ROOM_LIST,
    ROOM_WAITING,       // 房主等待队友 / 队友等待房主开始
    PLAYING,
    PAUSED,
    BOSS_ENTRANCE,
    BLESSING_SELECT,
    SHOP,
    GAME_OVER,
    VICTORY,
    LAYER_TRANSITION
}
