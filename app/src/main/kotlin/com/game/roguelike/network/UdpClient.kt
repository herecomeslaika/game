package com.game.roguelike.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpClient(private val serverAddress: InetAddress, private val port: Int = 8888) {
    private var socket: DatagramSocket? = null
    private var isRunning = false
    private val json = Json { ignoreUnknownKeys = true }

    var onFrameReceived: ((NetworkFrame) -> Unit)? = null

    fun start() {
        try {
            socket = DatagramSocket()
            isRunning = true
            Thread {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                while (isRunning) {
                    try {
                        socket?.receive(packet)
                        val data = packet.data.copyOf(packet.length)
                        val frame = json.decodeFromString<NetworkFrame>(String(data))
                        onFrameReceived?.invoke(frame)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendFrame(frame: NetworkFrame) {
        if (!isRunning) return
        Thread {
            try {
                val data = json.encodeToString(frame).toByteArray()
                val packet = DatagramPacket(data, data.size, serverAddress, port)
                socket?.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        socket?.close()
    }
}