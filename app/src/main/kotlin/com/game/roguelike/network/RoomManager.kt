package com.game.roguelike.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.*
import java.util.*

class RoomManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private var server: UdpServer? = null
    private var client: UdpClient? = null
    var isHost = false
    val discoveredRooms = mutableListOf<RoomInfo>()

    // 当前房间信息
    var currentRoomInfo: RoomInfo? = null
        private set
    var roomCode: String = ""
        private set
    var localPlayerId: Int = 0
        private set

    // 等待房间中的玩家列表
    val lobbyPlayers = mutableListOf<LobbyPlayer>()

    var onRoomDiscovered: ((List<RoomInfo>) -> Unit)? = null
    var onPlayerJoined: ((Int) -> Unit)? = null
    var onGameStart: (() -> Unit)? = null
    var onLobbyUpdated: (() -> Unit)? = null
    var onKicked: (() -> Unit)? = null

    /** 生成4位数字房间码 */
    private fun generateRoomCode(): String {
        return String.format("%04d", Random().nextInt(10000))
    }

    /** 安全获取本机IP（不会因无WiFi而崩溃） */
    fun getLocalIpAddress(): String {
        return try {
            // 优先尝试枚举网络接口
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
            // 回退到WiFi API
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt == 0) "0.0.0.0" else String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                (ipInt shr 8) and 0xff,
                (ipInt shr 16) and 0xff,
                (ipInt shr 24) and 0xff
            )
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    /** 创建房间 — 生成4位房间码，启动UDP服务器，进入等待状态 */
    fun createRoom(roomName: String): String {
        isHost = true
        val code = generateRoomCode()
        roomCode = code
        val localIp = getLocalIpAddress()
        val roomId = UUID.randomUUID().toString().substring(0, 6)
        val roomInfo = RoomInfo(roomId, code, localIp, 1, 4, roomName)
        currentRoomInfo = roomInfo
        localPlayerId = 1

        // 初始化房主自己
        lobbyPlayers.clear()
        lobbyPlayers.add(LobbyRules.createHostPlayer())

        server = UdpServer()
        server?.onFrameReceived = { frame, addr ->
            when (frame) {
                is RoomCommand -> handleHostRoomCommand(frame, addr)
                is PlayerInputFrame -> server?.broadcastFrame(frame)
                else -> server?.broadcastFrame(frame)
            }
        }
        server?.start()

        // 广播房间到局域网
        startRoomBroadcast(roomInfo)

        return code
    }

    /** 房主处理房间指令 */
    private fun handleHostRoomCommand(frame: RoomCommand, addr: InetAddress) {
        when (frame.type) {
            RoomCommand.CommandType.PLAYER_JOIN -> {
                if (lobbyPlayers.size < currentRoomInfo!!.maxPlayers) {
                    val playerId = nextPlayerId()
                    lobbyPlayers.add(LobbyRules.createGuestPlayer(playerId))
                    // 更新房间人数
                    currentRoomInfo = LobbyRules.syncPlayerCount(currentRoomInfo!!, lobbyPlayers)
                    // 通知所有客户端房间信息
                    // 通知加入者其playerId
                    server?.sendToClient(
                        RoomCommand(RoomCommand.CommandType.PLAYER_JOIN, playerId.toString()),
                        addr
                    )
                    broadcastLobbyState()
                    onPlayerJoined?.invoke(playerId)
                    onLobbyUpdated?.invoke()
                }
            }
            RoomCommand.CommandType.PLAYER_READY -> {
                val playerId = frame.data.toIntOrNull() ?: return
                val idx = lobbyPlayers.indexOfFirst { it.playerId == playerId }
                if (idx >= 0) {
                    lobbyPlayers[idx] = lobbyPlayers[idx].copy(isReady = true)
                    // 广播更新
                    broadcastLobbyState()
                    onLobbyUpdated?.invoke()
                }
            }
            RoomCommand.CommandType.START_GAME -> {
                server?.broadcastFrame(RoomCommand(RoomCommand.CommandType.START_GAME))
                onGameStart?.invoke()
            }
            RoomCommand.CommandType.KICK_PLAYER -> {
                val playerId = frame.data.toIntOrNull() ?: return
                lobbyPlayers.removeAll { it.playerId == playerId && !it.isHost }
                currentRoomInfo = LobbyRules.syncPlayerCount(currentRoomInfo!!, lobbyPlayers)
                // 通知被踢玩家
                server?.broadcastFrame(RoomCommand(RoomCommand.CommandType.KICK_PLAYER, playerId.toString()))
                broadcastLobbyState()
                onLobbyUpdated?.invoke()
            }
            else -> {}
        }
    }

    /** 开始游戏（房主调用） */
    fun startGame() {
        if (!isHost) return
        if (LobbyRules.canStartGame(lobbyPlayers)) {
            server?.broadcastFrame(RoomCommand(RoomCommand.CommandType.START_GAME))
            onGameStart?.invoke()
        }
    }

    /** 踢出玩家（房主调用） */
    fun kickPlayer(playerId: Int) {
        if (!isHost) return
        lobbyPlayers.removeAll { it.playerId == playerId && !it.isHost }
        currentRoomInfo = LobbyRules.syncPlayerCount(currentRoomInfo!!, lobbyPlayers)
        server?.broadcastFrame(RoomCommand(RoomCommand.CommandType.KICK_PLAYER, playerId.toString()))
        broadcastLobbyState()
        onLobbyUpdated?.invoke()
    }

    /** 搜索局域网内的房间 */
    fun scanRooms() {
        discoveredRooms.clear()
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 3000
                val buffer = "DISCOVER_ROOM".toByteArray()
                val packet = DatagramPacket(
                    buffer, buffer.size,
                    InetAddress.getByName("255.255.255.255"), 8889
                )
                socket.send(packet)

                val receiveBuffer = ByteArray(2048)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                val deadline = System.currentTimeMillis() + 3000
                while (System.currentTimeMillis() < deadline) {
                    try {
                        socket.soTimeout = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(100)
                        socket.receive(receivePacket)
                        val data = receivePacket.data.copyOf(receivePacket.length)
                        val roomInfo = json.decodeFromString<RoomInfo>(String(data))
                        if (discoveredRooms.none { it.roomId == roomInfo.roomId }) {
                            discoveredRooms.add(roomInfo)
                            onRoomDiscovered?.invoke(discoveredRooms.toList())
                        }
                    } catch (_: SocketTimeoutException) {
                        break
                    }
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /** 通过房间码加入房间 */
    fun joinRoomByCode(code: String) {
        // 先扫描获取房间IP，再连接
        // 或者直接尝试局域网广播加入
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 5000
                // 广播加入请求（携带房间码）
                val joinMsg = "JOIN_ROOM:$code"
                val buffer = joinMsg.toByteArray()
                val packet = DatagramPacket(
                    buffer, buffer.size,
                    InetAddress.getByName("255.255.255.255"), 8888
                )
                socket.send(packet)

                // 等待服务器回复
                val receiveBuffer = ByteArray(2048)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)
                val data = String(receivePacket.data.copyOf(receivePacket.length))

                if (data.startsWith("ROOM_ACCEPTED:")) {
                    val hostIp = data.substringAfter("ROOM_ACCEPTED:")
                    connectToHost(hostIp)
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /** 加入已发现的房间 */
    fun joinRoom(roomInfo: RoomInfo) {
        isHost = false
        localPlayerId = 0
        roomCode = roomInfo.roomCode
        currentRoomInfo = roomInfo
        connectToHost(roomInfo.hostIp)
    }

    /** 连接到房主 */
    private fun connectToHost(hostIp: String) {
        isHost = false
        client = UdpClient(InetAddress.getByName(hostIp))
        client?.onFrameReceived = { frame ->
            when (frame) {
                is RoomCommand -> handleClientRoomCommand(frame)
                else -> Unit
            }
        }
        client?.start()
        // 发送加入请求
        client?.sendFrame(RoomCommand(RoomCommand.CommandType.PLAYER_JOIN))
    }

    /** 客户端处理房间指令 */
    private fun handleClientRoomCommand(frame: RoomCommand) {
        when (frame.type) {
            RoomCommand.CommandType.PLAYER_JOIN -> {
                val pid = frame.data.toIntOrNull() ?: return
                localPlayerId = pid
                if (lobbyPlayers.none { it.playerId == pid }) {
                    lobbyPlayers.add(LobbyRules.createGuestPlayer(pid))
                }
                onPlayerJoined?.invoke(pid)
                onLobbyUpdated?.invoke()
            }
            RoomCommand.CommandType.ROOM_INFO -> {
                // 解析房间信息或玩家列表
                try {
                    val players = json.decodeFromString<List<LobbyPlayer>>(frame.data)
                    lobbyPlayers.clear()
                    lobbyPlayers.addAll(players)
                } catch (_: Exception) {
                    try {
                        val roomInfo = json.decodeFromString<RoomInfo>(frame.data)
                        currentRoomInfo = roomInfo
                    } catch (_: Exception) {}
                }
                onLobbyUpdated?.invoke()
            }
            RoomCommand.CommandType.START_GAME -> {
                onGameStart?.invoke()
            }
            RoomCommand.CommandType.KICK_PLAYER -> {
                onKicked?.invoke()
            }
            else -> {}
        }
    }

    /** 客户端设置准备 */
    fun setReady() {
        if (isHost) return
        val myId = localPlayerId.takeIf { it > 0 } ?: return
        client?.sendFrame(RoomCommand(RoomCommand.CommandType.PLAYER_READY, myId.toString()))
        // 本地立即更新
        val idx = lobbyPlayers.indexOfFirst { it.playerId == myId }
        if (idx >= 0) {
            lobbyPlayers[idx] = lobbyPlayers[idx].copy(isReady = true)
        }
        onLobbyUpdated?.invoke()
    }

    /** 主机广播房间信息（供局域网发现） */
    private fun nextPlayerId(): Int {
        return ((lobbyPlayers.maxOfOrNull { it.playerId } ?: 0) + 1).coerceAtLeast(2)
    }

    private fun broadcastLobbyState() {
        val info = currentRoomInfo ?: return
        currentRoomInfo = LobbyRules.syncPlayerCount(info, lobbyPlayers)
        server?.broadcastFrame(RoomCommand(RoomCommand.CommandType.ROOM_INFO, json.encodeToString(currentRoomInfo!!)))
        server?.broadcastFrame(RoomCommand(RoomCommand.CommandType.ROOM_INFO, json.encodeToString(lobbyPlayers.toList())))
    }

    private fun startRoomBroadcast(roomInfo: RoomInfo) {
        Thread {
            try {
                val serverSocket = DatagramSocket(8889)
                serverSocket.soTimeout = 1000
                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                while (server != null && server!!.isRunning) {
                    try {
                        serverSocket.receive(receivePacket)
                        val msg = String(receivePacket.data.copyOf(receivePacket.length))
                        if (msg == "DISCOVER_ROOM") {
                            // 回复房间信息
                            val data = json.encodeToString(currentRoomInfo ?: roomInfo).toByteArray()
                            val replyPacket = DatagramPacket(
                                data, data.size,
                                receivePacket.address, receivePacket.port
                            )
                            serverSocket.send(replyPacket)
                        }
                    } catch (_: SocketTimeoutException) {
                        // continue
                    }
                }
                serverSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun sendFrame(frame: NetworkFrame) {
        if (isHost) {
            server?.broadcastFrame(frame)
        } else {
            client?.sendFrame(frame)
        }
    }

    fun stop() {
        server?.stop()
        client?.stop()
        currentRoomInfo = null
        roomCode = ""
        localPlayerId = 0
        lobbyPlayers.clear()
        discoveredRooms.clear()
    }
}
