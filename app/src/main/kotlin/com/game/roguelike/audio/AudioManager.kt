package com.game.roguelike.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import com.game.roguelike.R
import kotlin.random.Random

class AudioManager(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .build()

    private var bgmPlayer: MediaPlayer? = null
    private var currentBgmResId: Int = 0

    var sfxVolume = 1f
    var bgmVolume = 0.5f
    var muted = false

    private val soundMap = mutableMapOf<String, Int>()

    private var footstepTimer = 0f
    private val footstepInterval = 0.28f

    fun loadSounds(context: Context) {
        val sounds = listOf(
            "attack1" to R.raw.sfx_attack1,
            "attack2" to R.raw.sfx_attack2,
            "attack3" to R.raw.sfx_attack3,
            "hit" to R.raw.sfx_hit,
            "crit" to R.raw.sfx_crit,
            "player_hurt" to R.raw.sfx_player_hurt,
            "dash" to R.raw.sfx_dash,
            "special" to R.raw.sfx_special,
            "enemy_death" to R.raw.sfx_enemy_death,
            "boss_phase" to R.raw.sfx_boss_phase,
            "pickup" to R.raw.sfx_pickup,
            "blessing" to R.raw.sfx_blessing,
            "level_up" to R.raw.sfx_level_up,
            "footstep" to R.raw.sfx_footstep,
            "ui_click" to R.raw.sfx_ui_click,
            "cooldown_ready" to R.raw.sfx_cooldown_ready
        )
        for ((name, resId) in sounds) {
            try {
                soundMap[name] = soundPool.load(context, resId, 1)
            } catch (_: Exception) {
            }
        }
    }

    fun play(name: String) {
        if (muted || sfxVolume <= 0f) return
        val soundId = soundMap[name] ?: return
        val pitch = 1f + (Random.nextFloat() - 0.5f) * 0.1f
        soundPool.play(soundId, sfxVolume, sfxVolume, 0, 0, pitch)
    }

    fun playWithPitch(name: String, pitch: Float) {
        if (muted || sfxVolume <= 0f) return
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, sfxVolume, sfxVolume, 0, 0, pitch.coerceIn(0.5f, 2f))
    }

    fun playCrit() {
        if (muted || sfxVolume <= 0f) return
        val soundId = soundMap["crit"] ?: return
        soundPool.play(soundId, sfxVolume, sfxVolume, 0, 0, 1f)
        play("hit")
    }

    fun tryFootstep(dt: Float, isMoving: Boolean) {
        if (!isMoving || muted || sfxVolume <= 0f) {
            footstepTimer = 0f
            return
        }
        footstepTimer += dt
        if (footstepTimer >= footstepInterval) {
            footstepTimer -= footstepInterval
            val soundId = soundMap["footstep"] ?: return
            val pitch = 0.9f + Random.nextFloat() * 0.2f
            soundPool.play(soundId, sfxVolume * 0.5f, sfxVolume * 0.5f, 0, 0, pitch)
        }
    }

    fun playBgm(context: Context, resId: Int) {
        if (currentBgmResId == resId) return
        stopBgm()
        currentBgmResId = resId
        try {
            bgmPlayer = MediaPlayer.create(context, resId).apply {
                isLooping = true
                setVolume(
                    if (muted) 0f else bgmVolume,
                    if (muted) 0f else bgmVolume
                )
                start()
            }
        } catch (_: Exception) {
        }
    }

    fun stopBgm() {
        bgmPlayer?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {
            }
        }
        bgmPlayer = null
        currentBgmResId = 0
    }

    fun pauseBgm() {
        bgmPlayer?.let {
            try {
                if (it.isPlaying) it.pause()
            } catch (_: Exception) {
            }
        }
    }

    fun resumeBgm() {
        bgmPlayer?.let {
            try {
                if (!it.isPlaying) it.start()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        stopBgm()
        try {
            soundPool.release()
        } catch (_: Exception) {
        }
    }
}
