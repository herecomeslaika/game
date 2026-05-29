package com.game.roguelike.rendering

import android.content.Context
import android.graphics.*
import com.game.roguelike.R
import com.game.roguelike.network.LobbyPlayer
import com.game.roguelike.network.RoomInfo
import com.game.roguelike.network.RoomManager
import kotlin.math.sin
import kotlin.math.min

class ScreenRenderer(private val renderer: IsometricRenderer, private val context: Context) {

    // 加载主界面人物图片
    private val menuPlayerBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.people)

    // 菜单按钮点击区域
    val startBtnRect = RectF()
    val multiplayerBtnRect = RectF()
    val exitBtnRect = RectF()
    
    // 联机大厅按钮区域
    val createRoomBtnRect = RectF()
    val joinRoomBtnRect = RectF()
    val backToMenuBtnRect = RectF()
    val roomListRects = mutableListOf<RectF>()

    // 等待房间按钮区域
    val startGameBtnRect = RectF()
    val kickPlayerBtnRects = mutableListOf<RectF>()
    val readyBtnRect = RectF()
    val leaveRoomBtnRect = RectF()

    // 加入房间相关
    val joinByCodeBtnRect = RectF()

    fun renderMenu(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // 左侧狂野暗黑风格艺术字标题
        renderer.titlePaint.textSize = 190f
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.textAlign = Paint.Align.LEFT
        // 暗黑风格：暗红色+粗黑描边+文字阴影
        renderer.titlePaint.setShadowLayer(15f, 6f, 6f, Color.BLACK)
        renderer.titlePaint.color = Color.parseColor("#990000")
        renderer.titlePaint.style = Paint.Style.FILL_AND_STROKE
        renderer.titlePaint.strokeWidth = 7f
        canvas.drawText("冥途", w * 0.08f, h * 0.32f, renderer.titlePaint)
        // 重置画笔样式避免影响其他绘制
        renderer.titlePaint.clearShadowLayer()
        renderer.titlePaint.strokeWidth = 0f
        renderer.titlePaint.style = Paint.Style.FILL

        // 菜单按钮列表
        val btnX = w * 0.08f
        var btnY = h * 0.5f
        val btnSpacing = 170f
        val btnWidth = 600f
        val btnHeight = 80f

        // 开始游戏按钮
        val alpha = ((sin(renderer.globalTime * 2f) + 1) * 127 + 128).toInt()
        renderer.subtitlePaint.color = Color.argb(alpha, 255, 215, 0)
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.textSize = 82f
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        startBtnRect.set(btnX - 20f, btnY - 60f, btnX + btnWidth, btnY + 20f)
        canvas.drawText("开始游戏", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        // 联机模式按钮
        renderer.subtitlePaint.color = Color.parseColor("#88FF88")
        multiplayerBtnRect.set(btnX - 20f, btnY - 60f, btnX + btnWidth, btnY + 20f)
        canvas.drawText("联机模式", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        // 选项按钮（暂不实现）
        renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
        canvas.drawText("选项", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        // 退出游戏按钮
        exitBtnRect.set(btnX - 20f, btnY - 60f, btnX + btnWidth, btnY + 20f)
        canvas.drawText("退出游戏", btnX, btnY, renderer.subtitlePaint)

        // ========== 右侧人物图片 ==========
        val cx = w * 0.78f
        val cy = h * 0.52f
        val bitmapWidth = 1480f
        val bitmapHeight = 2060f
        val left = cx - bitmapWidth / 2
        val top = cy - bitmapHeight / 2
        val dstRect = RectF(left, top, left + bitmapWidth, top + bitmapHeight)
        canvas.drawBitmap(menuPlayerBitmap, null, dstRect, renderer.paint)

        // 底部版本信息
        renderer.subtitlePaint.textSize = 30f
        renderer.subtitlePaint.color = Color.parseColor("#666666")
        canvas.drawText("冥途 v1.0", w * 0.08f, h * 0.92f, renderer.subtitlePaint)

        // 恢复画笔默认设置
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT
        renderer.subtitlePaint.textSize = 54f
    }

    fun renderMultiplayerLobby(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // 标题
        renderer.titlePaint.textSize = 120f
        renderer.titlePaint.color = Color.parseColor("#990000")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("联机大厅", w / 2f, h * 0.25f, renderer.titlePaint)

        val btnX = w / 2f
        var btnY = h * 0.45f
        val btnWidth = 500f
        val btnHeight = 90f

        // 创建房间按钮
        renderer.subtitlePaint.textSize = 70f
        renderer.subtitlePaint.color = Color.parseColor("#4CAF50")
        createRoomBtnRect.set(btnX - btnWidth / 2, btnY - btnHeight / 2, btnX + btnWidth / 2, btnY + btnHeight / 2)
        canvas.drawText("创建房间", btnX, btnY + 20f, renderer.subtitlePaint)
        btnY += 180f

        // 加入房间按钮
        renderer.subtitlePaint.color = Color.parseColor("#2196F3")
        joinRoomBtnRect.set(btnX - btnWidth / 2, btnY - btnHeight / 2, btnX + btnWidth / 2, btnY + btnHeight / 2)
        canvas.drawText("加入房间", btnX, btnY + 20f, renderer.subtitlePaint)
        btnY += 180f

        // 返回按钮
        renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
        backToMenuBtnRect.set(btnX - btnWidth / 2, btnY - btnHeight / 2, btnX + btnWidth / 2, btnY + btnHeight / 2)
        canvas.drawText("返回主菜单", btnX, btnY + 20f, renderer.subtitlePaint)

        // 恢复设置
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    }

    fun renderRoomList(canvas: Canvas, w: Int, h: Int, rooms: List<RoomInfo>) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // 标题
        renderer.titlePaint.textSize = 100f
        renderer.titlePaint.color = Color.parseColor("#990000")
        canvas.drawText("可用房间列表", w / 2f, h * 0.15f, renderer.titlePaint)

        val listY = h * 0.25f
        val itemHeight = 100f
        val itemWidth = w * 0.7f
        val itemX = w * 0.15f

        roomListRects.clear()

        if (rooms.isEmpty()) {
            renderer.subtitlePaint.textSize = 50f
            renderer.subtitlePaint.color = Color.parseColor("#888888")
            canvas.drawText("未搜索到房间，请确保设备在同一局域网下", w / 2f, h * 0.5f, renderer.subtitlePaint)
        } else {
            for ((index, room) in rooms.withIndex()) {
                val y = listY + index * (itemHeight + 20f)
                val rect = RectF(itemX, y, itemX + itemWidth, y + itemHeight)
                roomListRects.add(rect)

                // 绘制房间背景
                renderer.paint.color = Color.parseColor("#1AFFFFFF")
                canvas.drawRoundRect(rect, 16f, 16f, renderer.paint)

                // 房间信息 — 显示房间码
                renderer.subtitlePaint.textSize = 45f
                renderer.subtitlePaint.color = Color.WHITE
                renderer.subtitlePaint.textAlign = Paint.Align.LEFT
                val displayCode = if (room.roomCode.isNotEmpty()) "房间号: ${room.roomCode}" else room.roomName
                canvas.drawText("$displayCode  (${room.playerCount}/${room.maxPlayers})", itemX + 30f, y + 60f, renderer.subtitlePaint)
                renderer.subtitlePaint.textSize = 35f
                renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
                canvas.drawText("IP: ${room.hostIp}", w * 0.6f, y + 60f, renderer.subtitlePaint)
            }
        }

        // 返回按钮
        renderer.subtitlePaint.textSize = 60f
        renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        backToMenuBtnRect.set(w / 2f - 200f, h * 0.85f, w / 2f + 200f, h * 0.85f + 70f)
        canvas.drawText("返回", w / 2f, h * 0.85f + 50f, renderer.subtitlePaint)

        // 恢复设置
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    }

    /**
     * 渲染等待房间界面 — 房主看到房间码+玩家列表+开始/踢人按钮
     * 客户端看到玩家列表+准备按钮
     */
    fun renderRoomWaiting(canvas: Canvas, w: Int, h: Int, roomManager: RoomManager) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        val isHost = roomManager.isHost
        val roomCode = roomManager.roomCode
        val players = roomManager.lobbyPlayers

        // 房间码标题区域
        renderer.titlePaint.textSize = 80f
        renderer.titlePaint.color = Color.parseColor("#990000")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("等待房间", w / 2f, h * 0.12f, renderer.titlePaint)

        // 房间码（大号金色显示）
        renderer.titlePaint.textSize = 140f
        renderer.titlePaint.color = Color.parseColor("#FFD700")
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(roomCode, w / 2f, h * 0.27f, renderer.titlePaint)

        // "房间号"提示
        renderer.subtitlePaint.textSize = 36f
        renderer.subtitlePaint.color = Color.parseColor("#888888")
        renderer.subtitlePaint.typeface = Typeface.DEFAULT
        canvas.drawText("房间号", w / 2f, h * 0.32f, renderer.subtitlePaint)

        // 玩家列表
        val listStartY = h * 0.38f
        val itemHeight = 80f
        val itemWidth = w * 0.6f
        val itemX = w * 0.2f

        kickPlayerBtnRects.clear()

        for ((index, player) in players.withIndex()) {
            val y = listStartY + index * (itemHeight + 15f)
            val rect = RectF(itemX, y, itemX + itemWidth, y + itemHeight)

            // 玩家背景
            renderer.paint.color = Color.parseColor("#1AFFFFFF")
            canvas.drawRoundRect(rect, 12f, 12f, renderer.paint)

            // 玩家名称
            renderer.subtitlePaint.textSize = 42f
            renderer.subtitlePaint.textAlign = Paint.Align.LEFT
            renderer.subtitlePaint.color = if (player.isHost) Color.parseColor("#FFD700") else Color.WHITE
            val displayName = if (player.isHost) "${player.name} 👑" else player.name
            canvas.drawText(displayName, itemX + 20f, y + 50f, renderer.subtitlePaint)

            // 准备状态
            renderer.subtitlePaint.textSize = 36f
            renderer.subtitlePaint.textAlign = Paint.Align.RIGHT
            renderer.subtitlePaint.color = if (player.isReady) Color.parseColor("#4CAF50") else Color.parseColor("#FF5252")
            val statusText = if (player.isReady) "已准备" else "未准备"
            canvas.drawText(statusText, itemX + itemWidth - 100f, y + 50f, renderer.subtitlePaint)

            // 踢人按钮（仅房主看到，且不是自己）
            if (isHost && !player.isHost) {
                val kickRect = RectF(itemX + itemWidth + 15f, y + 10f, itemX + itemWidth + 80f, y + itemHeight - 10f)
                kickPlayerBtnRects.add(kickRect)
                renderer.paint.color = Color.parseColor("#FF5252")
                canvas.drawRoundRect(kickRect, 8f, 8f, renderer.paint)
                renderer.subtitlePaint.textSize = 28f
                renderer.subtitlePaint.textAlign = Paint.Align.CENTER
                renderer.subtitlePaint.color = Color.WHITE
                canvas.drawText("踢出", kickRect.centerX(), kickRect.centerY() + 10f, renderer.subtitlePaint)
            }
        }

        // 底部按钮区域
        val btnY = h * 0.78f
        val btnWidth = 350f
        val btnHeight = 80f

        if (isHost) {
            // 房主：开始游戏按钮（所有人都准备才可点击）
            val allReady = players.all { it.isReady } && players.size >= 2
            renderer.subtitlePaint.textSize = 60f
            renderer.subtitlePaint.textAlign = Paint.Align.CENTER
            if (allReady) {
                // 闪烁效果
                val alpha = ((sin(renderer.globalTime * 3f) + 1) * 60 + 135).toInt()
                renderer.subtitlePaint.color = Color.argb(alpha, 76, 175, 80)
                startGameBtnRect.set(w / 2f - btnWidth / 2, btnY - btnHeight / 2, w / 2f + btnWidth / 2, btnY + btnHeight / 2)
                canvas.drawText("开始游戏", w / 2f, btnY + 20f, renderer.subtitlePaint)
            } else {
                renderer.subtitlePaint.color = Color.parseColor("#555555")
                val hint = if (players.size < 2) "等待玩家加入..." else "等待玩家准备..."
                canvas.drawText(hint, w / 2f, btnY + 20f, renderer.subtitlePaint)
                startGameBtnRect.set(0f, 0f, 0f, 0f) // 无效区域
            }
        } else {
            // 客户端：准备按钮
            val myPlayer = players.firstOrNull { !it.isHost }
            val isReady = myPlayer?.isReady == true
            renderer.subtitlePaint.textSize = 60f
            renderer.subtitlePaint.textAlign = Paint.Align.CENTER
            if (isReady) {
                renderer.subtitlePaint.color = Color.parseColor("#4CAF50")
                readyBtnRect.set(w / 2f - btnWidth / 2, btnY - btnHeight / 2, w / 2f + btnWidth / 2, btnY + btnHeight / 2)
                canvas.drawText("✓ 已准备", w / 2f, btnY + 20f, renderer.subtitlePaint)
            } else {
                val alpha = ((sin(renderer.globalTime * 3f) + 1) * 60 + 135).toInt()
                renderer.subtitlePaint.color = Color.argb(alpha, 33, 150, 243)
                readyBtnRect.set(w / 2f - btnWidth / 2, btnY - btnHeight / 2, w / 2f + btnWidth / 2, btnY + btnHeight / 2)
                canvas.drawText("准备", w / 2f, btnY + 20f, renderer.subtitlePaint)
            }
        }

        // 离开房间按钮
        renderer.subtitlePaint.textSize = 45f
        renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
        leaveRoomBtnRect.set(w / 2f - 150f, h * 0.9f, w / 2f + 150f, h * 0.9f + 60f)
        canvas.drawText("离开房间", w / 2f, h * 0.9f + 40f, renderer.subtitlePaint)

        // 恢复设置
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(200, 20, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        renderer.titlePaint.color = Color.parseColor("#FF2222")
        canvas.drawText("游戏结束", w / 2f, h * 0.4f, renderer.titlePaint)

        renderer.subtitlePaint.color = Color.parseColor("#AA8888")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, renderer.subtitlePaint)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(200, 0, 20, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        renderer.titlePaint.color = Color.parseColor("#FFD700")
        canvas.drawText("通关!", w / 2f, h * 0.4f, renderer.titlePaint)

        renderer.subtitlePaint.color = Color.parseColor("#DDAA66")
        canvas.drawText("点击返回", w / 2f, h * 0.55f, renderer.subtitlePaint)
    }

    fun drawFade(canvas: Canvas, alpha: Float) {
        renderer.paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, renderer.screenWidth.toFloat(), renderer.screenHeight.toFloat(), renderer.paint)
    }

    fun renderBossEntrance(canvas: Canvas, bossName: String, bossTitle: String, timer: Float, phase: Int, w: Int, h: Int) {
        when (phase) {
            0 -> {
                // Phase 0: dark overlay fading in
                val overlayAlpha = min(timer / 1.5f, 1f) * 180f
                renderer.paint.color = Color.argb(overlayAlpha.toInt(), 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Center glow
                if (timer > 0.5f) {
                    val glowAlpha = min((timer - 0.5f) / 1f, 1f) * 60f
                    renderer.paint.color = Color.argb(glowAlpha.toInt(), 170, 68, 255)
                    val glowRadius = 80f + timer * 60f
                    canvas.drawCircle(w / 2f, h / 2f, glowRadius, renderer.paint)
                }
            }
            1 -> {
                // Phase 1: dark overlay + boss name with gold outline
                renderer.paint.color = Color.argb(180, 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Glow behind name
                val glowPulse = (sin(timer * 4f) + 1f) * 0.5f
                renderer.paint.color = Color.argb((40 + glowPulse * 30).toInt(), 170, 68, 255)
                canvas.drawCircle(w / 2f, h * 0.42f, 120f + glowPulse * 30f, renderer.paint)

                // Boss name — scale from large to normal
                val nameProgress = min(timer / 0.6f, 1f)
                val nameScale = 1f + (1f - nameProgress) * 0.8f
                val nameAlpha = (nameProgress * 255).toInt()

                canvas.save()
                canvas.scale(nameScale, nameScale, w / 2f, h * 0.42f)

                // Gold outline
                renderer.titlePaint.color = Color.argb(nameAlpha, 255, 215, 0)
                renderer.titlePaint.style = Paint.Style.FILL_AND_STROKE
                renderer.titlePaint.strokeWidth = 4f
                canvas.drawText(bossName, w / 2f, h * 0.42f, renderer.titlePaint)
                renderer.titlePaint.style = Paint.Style.FILL
                renderer.titlePaint.strokeWidth = 0f

                canvas.restore()

                // Title text (subtitle)
                if (timer > 0.4f) {
                    val titleProgress = min((timer - 0.4f) / 0.5f, 1f)
                    val titleAlpha = (titleProgress * 200).toInt()
                    renderer.subtitlePaint.color = Color.argb(titleAlpha, 170, 130, 200)
                    canvas.drawText(bossTitle, w / 2f, h * 0.52f, renderer.subtitlePaint)
                }

                // Decorative lines
                val lineAlpha = (nameProgress * 120).toInt()
                renderer.paint.color = Color.argb(lineAlpha, 170, 68, 255)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 2f
                val lineW = min(timer / 0.8f, 1f) * w * 0.35f
                canvas.drawLine(w / 2f - lineW, h * 0.47f, w / 2f + lineW, h * 0.47f, renderer.paint)
                renderer.paint.style = Paint.Style.FILL
                renderer.paint.strokeWidth = 0f
            }
            2 -> {
                // Phase 2: fade out
                val fadeOut = min(timer / 0.5f, 1f)
                val overlayAlpha = 180f * (1f - fadeOut)
                renderer.paint.color = Color.argb(overlayAlpha.toInt(), 5, 2, 15)
                renderer.paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

                // Fading name
                val nameAlpha = ((1f - fadeOut) * 255).toInt()
                if (nameAlpha > 0) {
                    renderer.titlePaint.color = Color.argb(nameAlpha, 255, 215, 0)
                    canvas.drawText(bossName, w / 2f, h * 0.42f, renderer.titlePaint)

                    renderer.subtitlePaint.color = Color.argb((nameAlpha * 0.78f).toInt(), 170, 130, 200)
                    canvas.drawText(bossTitle, w / 2f, h * 0.52f, renderer.subtitlePaint)
                }
            }
        }
    }
}
