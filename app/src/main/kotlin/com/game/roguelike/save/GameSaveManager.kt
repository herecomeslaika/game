package com.game.roguelike.save

import android.content.Context

class GameSaveManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSave(): Boolean = prefs.contains(KEY_SAVE)

    fun save(data: GameSaveData) {
        prefs.edit().putString(KEY_SAVE, GameSaveCodec.encode(data)).apply()
    }

    fun load(): GameSaveData? {
        val raw = prefs.getString(KEY_SAVE, null) ?: return null
        return GameSaveCodec.decode(raw)
    }

    fun deleteSave() {
        prefs.edit().remove(KEY_SAVE).apply()
    }

    private companion object {
        const val PREFS_NAME = "hades_roguelike_save"
        const val KEY_SAVE = "current_run"
    }
}
