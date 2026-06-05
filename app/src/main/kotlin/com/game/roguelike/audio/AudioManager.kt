package com.game.roguelike.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import com.game.roguelike.R

class AudioManager(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .build()

    private var bgmPlayer: MediaPlayer? = null
    private var currentBgmResId: Int = 0

    var volume = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }
    var muted = false

    private val soundMap = mutableMapOf<String, Int>()

    fun loadSounds(context: Context) {
        val sounds = listOf(
            "attack1" to R.raw.sfx_attack1,
            "attack2" to R.raw.sfx_attack2,
            "attack3" to R.raw.sfx_attack3,
            "hit" to R.raw.sfx_hit,
            "player_hurt" to R.raw.sfx_player_hurt,
            "dash" to R.raw.sfx_dash,
            "enemy_death" to R.raw.sfx_enemy_death,
            "boss_phase" to R.raw.sfx_boss_phase,
            "pickup" to R.raw.sfx_pickup,
            "special" to R.raw.sfx_special
        )
        for ((name, resId) in sounds) {
            try {
                soundMap[name] = soundPool.load(context, resId, 1)
            } catch (_: Exception) {
                // Sound file not found — skip silently
            }
        }
    }

    fun play(name: String) {
        if (muted || volume <= 0f) return
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, volume, volume, 0, 0, 1f)
    }

    fun playBgm(context: Context, resId: Int) {
        if (currentBgmResId == resId) return
        stopBgm()
        currentBgmResId = resId
        try {
            bgmPlayer = MediaPlayer.create(context, resId).apply {
                isLooping = true
                setVolume(if (muted) 0f else volume * 0.5f, if (muted) 0f else volume * 0.5f)
                start()
            }
        } catch (_: Exception) {
            // BGM file not found — skip silently
        }
    }

    fun stopBgm() {
        bgmPlayer?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        bgmPlayer = null
        currentBgmResId = 0
    }

    fun pauseBgm() {
        bgmPlayer?.let {
            try {
                if (it.isPlaying) it.pause()
            } catch (_: Exception) {}
        }
    }

    fun resumeBgm() {
        bgmPlayer?.let {
            try {
                if (!it.isPlaying) it.start()
            } catch (_: Exception) {}
        }
    }

    fun release() {
        stopBgm()
        try {
            soundPool.release()
        } catch (_: Exception) {}
    }
}
