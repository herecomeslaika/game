package com.game.roguelike.network

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class UdpServer(private val port: Int = 8888) {
    private var socket: DatagramSocket? = null
    var isRunning = false
        private set
    private val connectedClients = mutableListOf<InetAddress>()
    private val json = Json { ignoreUnknownKeys = true }

    // 收到帧数据的回调
    var onFrameReceived: ((NetworkFrame, InetAddress) -> Unit)? = null

    fun start() {
        try {
            socket = DatagramSocket(port)
            isRunning = true
            Thread {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                while (isRunning) {
                    try {
                        socket?.receive(packet)
                        val data = packet.data.copyOf(packet.length)
                        val frame = json.decodeFromString<NetworkFrame>(String(data))
                        val clientAddr = packet.address
                        if (!connectedClients.contains(clientAddr)) {
                            connectedClients.add(clientAddr)
                        }
                        onFrameReceived?.invoke(frame, clientAddr)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        } catch (e: SocketException) {
            e.printStackTrace()
        }
    }

    // 广播帧给所有连接的客户端
    fun broadcastFrame(frame: NetworkFrame) {
        if (!isRunning || connectedClients.isEmpty()) return
        Thread {
            try {
                val data = json.encodeToString(frame).toByteArray()
                connectedClients.forEach { client ->
                    val packet = DatagramPacket(data, data.size, client, port)
                    socket?.send(packet)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 发送给指定客户端
    fun sendToClient(frame: NetworkFrame, client: InetAddress) {
        if (!isRunning) return
        Thread {
            try {
                val data = json.encodeToString(frame).toByteArray()
                val packet = DatagramPacket(data, data.size, client, port)
                socket?.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        socket?.close()
        connectedClients.clear()
    }
}