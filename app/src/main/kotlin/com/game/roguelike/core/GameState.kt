package com.game.roguelike.core

enum class GameState {
    MENU,
    PLAYING,
    SHOP,
    EVENT,
    BLESSING_SELECT,
    LAYER_TRANSITION,
    BOSS_ENTRANCE,
    PLAYER_DEATH,
    GAME_OVER,
    VICTORY,
    MULTIPLAYER_LOBBY,
    ROOM_LIST,
    ROOM_WAITING
}

enum class LayerId {
    TARTARUS,
    ASPHODEL,
    ELYSIUM
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
}
