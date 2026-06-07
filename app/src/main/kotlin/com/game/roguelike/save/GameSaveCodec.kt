package com.game.roguelike.save

import java.io.StringReader
import java.io.StringWriter
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Properties

object GameSaveCodec {
    fun encode(save: GameSaveData): String {
        val props = Properties().apply {
            setProperty("version", save.version.toString())
            setProperty("layerIndex", save.layerIndex.toString())
            setProperty("roomId", save.roomId.toString())
            setProperty("roomCleared", save.roomCleared.toString())
            setProperty("playerHealth", save.playerHealth.toString())
            setProperty("playerMaxHealth", save.playerMaxHealth.toString())
            setProperty("gold", save.gold.toString())
            setProperty("blessingIds", save.blessingIds.encodeList())
            setProperty("state", save.state)
            setProperty("offeringIds", save.offeringIds.encodeList())
            setProperty("bossRelicIds", save.bossRelicIds.encodeList())
            setProperty("droppedBossRelics", save.droppedBossRelics.encodeList())
        }
        return StringWriter().use { writer ->
            props.store(writer, null)
            writer.toString()
        }
    }

    fun decode(raw: String): GameSaveData? {
        return try {
            val props = Properties().apply { load(StringReader(raw)) }
            GameSaveData(
                version = props.getProperty("version", "1").toInt(),
                layerIndex = props.getProperty("layerIndex").toInt(),
                roomId = props.getProperty("roomId").toInt(),
                roomCleared = props.getProperty("roomCleared", "false").toBoolean(),
                playerHealth = props.getProperty("playerHealth").toInt(),
                playerMaxHealth = props.getProperty("playerMaxHealth").toInt(),
                gold = props.getProperty("gold").toInt(),
                blessingIds = props.getProperty("blessingIds", "").decodeList(),
                state = props.getProperty("state", "PLAYING"),
                offeringIds = props.getProperty("offeringIds", "").decodeList(),
                bossRelicIds = props.getProperty("bossRelicIds", "").decodeList(),
                droppedBossRelics = props.getProperty("droppedBossRelics", "").decodeList()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun List<String>.encodeList(): String {
        return joinToString(",") { URLEncoder.encode(it, "UTF-8") }
    }

    private fun String.decodeList(): List<String> {
        if (isBlank()) return emptyList()
        return split(",").map { URLDecoder.decode(it, "UTF-8") }.filter { it.isNotBlank() }
    }
}
