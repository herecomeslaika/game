package com.game.roguelike.rendering

import android.content.Context
import android.graphics.*
import com.game.roguelike.R
import com.game.roguelike.network.LobbyPlayer
import com.game.roguelike.network.RoomInfo
import com.game.roguelike.network.RoomManager
import kotlin.math.sin
import kotlin.math.cos
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
    
    // UI状态
    private var networkStatus = "已连接"  // 已连接、连接中、已断开
    private var lastPlayerCount = 0

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
        // 背景渐变效果
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
        
        // 添加网格背景效果
        drawGridBackground(canvas, w, h)

        // 标题背景 - 带渐变边框
        val titleBarHeight = h * 0.2f
        val titleBgPaint = Paint().apply {
            color = Color.argb(80, 153, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), titleBarHeight, titleBgPaint)
        
        // 顶部装饰线
        renderer.paint.color = Color.parseColor("#FF6B35")
        renderer.paint.strokeWidth = 3f
        canvas.drawLine(0f, titleBarHeight - 5f, w.toFloat(), titleBarHeight - 5f, renderer.paint)

        // 标题
        renderer.titlePaint.textSize = 130f
        renderer.titlePaint.color = Color.parseColor("#FFD700")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.setShadowLayer(8f, 0f, 4f, Color.BLACK)
        canvas.drawText("🌐 联机大厅", w / 2f, h * 0.15f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        val btnCenterX = w / 2f
        var btnY = h * 0.32f
        val btnWidth = 520f
        val btnHeight = 100f
        val btnSpacing = 160f

        // 创建房间按钮 - 改进设计
        drawButtonWithGlow(canvas, btnCenterX, btnY, btnWidth, btnHeight, 
            "✚ 创建房间", Color.parseColor("#4CAF50"), createRoomBtnRect)
        btnY += btnSpacing

        // 加入房间按钮
        drawButtonWithGlow(canvas, btnCenterX, btnY, btnWidth, btnHeight,
            "🔍 加入房间", Color.parseColor("#2196F3"), joinRoomBtnRect)
        btnY += btnSpacing

        // 返回按钮
        drawButtonWithGlow(canvas, btnCenterX, btnY, btnWidth, btnHeight,
            "← 返回主菜单", Color.parseColor("#757575"), backToMenuBtnRect)

        // 底部信息提示
        renderer.subtitlePaint.textSize = 40f
        renderer.subtitlePaint.color = Color.parseColor("#888888")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("2-4人联机对战", w / 2f, h * 0.92f, renderer.subtitlePaint)

        // 恢复设置
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    }
    
    /**
     * 绘制带有发光效果和悬停反馈的按钮
     */
    private fun drawButtonWithGlow(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float,
                                   text: String, color: Int, rect: RectF) {
        // 发光背景
        val glowPaint = Paint()
        glowPaint.color = Color.argb(40, (color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
        glowPaint.style = Paint.Style.FILL
        val glowRadius = 20f
        canvas.drawRoundRect(cx - width / 2 - glowRadius, cy - height / 2 - glowRadius,
            cx + width / 2 + glowRadius, cy + height / 2 + glowRadius, 
            glowRadius, glowRadius, glowPaint)
        
        // 按钮主体
        renderer.paint.color = color
        renderer.paint.style = Paint.Style.FILL
        rect.set(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2)
        canvas.drawRoundRect(rect, 16f, 16f, renderer.paint)
        
        // 按钮边框
        renderer.paint.color = Color.argb(150, 255, 255, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 16f, 16f, renderer.paint)
        
        // 按钮文字
        renderer.subtitlePaint.textSize = 70f
        renderer.subtitlePaint.color = Color.WHITE
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(text, cx, cy + 25f, renderer.subtitlePaint)
    }
    
    /**
     * 绘制网格背景效果
     */
    private fun drawGridBackground(canvas: Canvas, w: Int, h: Int) {
        renderer.paint.color = Color.argb(15, 100, 100, 150)
        renderer.paint.strokeWidth = 1f
        val gridSize = 80f
        
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), renderer.paint)
            x += gridSize
        }
        
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w.toFloat(), y, renderer.paint)
            y += gridSize
        }
    }

    fun renderRoomList(canvas: Canvas, w: Int, h: Int, rooms: List<RoomInfo>) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
        
        // 背景网格
        drawGridBackground(canvas, w, h)

        // 顶部标题区
        val titleBarHeight = h * 0.15f
        val titleBgPaint = Paint().apply {
            color = Color.argb(80, 33, 150, 243)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), titleBarHeight, titleBgPaint)
        
        renderer.paint.color = Color.parseColor("#2196F3")
        renderer.paint.strokeWidth = 3f
        canvas.drawLine(0f, titleBarHeight - 5f, w.toFloat(), titleBarHeight - 5f, renderer.paint)

        // 标题
        renderer.titlePaint.textSize = 110f
        renderer.titlePaint.color = Color.parseColor("#64B5F6")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.setShadowLayer(8f, 0f, 4f, Color.BLACK)
        canvas.drawText("🔍 可用房间", w / 2f, h * 0.1f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        val listY = h * 0.2f
        val itemHeight = 110f
        val itemSpacing = 15f
        val itemWidth = w * 0.8f
        val itemX = w * 0.1f

        roomListRects.clear()

        if (rooms.isEmpty()) {
            // 无房间提示 - 改进设计
            renderer.subtitlePaint.textSize = 50f
            renderer.subtitlePaint.color = Color.parseColor("#888888")
            renderer.subtitlePaint.textAlign = Paint.Align.CENTER
            canvas.drawText("正在搜索房间...", w / 2f, h * 0.45f, renderer.subtitlePaint)
            
            renderer.subtitlePaint.textSize = 40f
            canvas.drawText("请确保设备在同一局域网下", w / 2f, h * 0.52f, renderer.subtitlePaint)
            
            // 添加加载动画
            drawLoadingAnimation(canvas, w / 2f, h * 0.65f)
        } else {
            // 显示房间列表信息
            renderer.subtitlePaint.textSize = 38f
            renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
            renderer.subtitlePaint.textAlign = Paint.Align.LEFT
            canvas.drawText("找到 ${rooms.size} 个房间:", itemX, listY - 20f, renderer.subtitlePaint)
            
            for ((index, room) in rooms.withIndex()) {
                val y = listY + index * (itemHeight + itemSpacing)
                if (y > h * 0.85f) break // 只显示屏幕内的房间
                
                val rect = RectF(itemX, y, itemX + itemWidth, y + itemHeight)
                roomListRects.add(rect)

                // 房间背景 - 带渐变效果
                val bgPaint = Paint().apply {
                    color = Color.argb(60, 33, 150, 243)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
                
                // 房间边框
                renderer.paint.color = Color.argb(200, 33, 150, 243)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 2f
                canvas.drawRoundRect(rect, 12f, 12f, renderer.paint)

                // 房间名称和房间号
                renderer.subtitlePaint.textSize = 48f
                renderer.subtitlePaint.textAlign = Paint.Align.LEFT
                renderer.subtitlePaint.color = Color.WHITE
                renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
                val displayName = room.roomName
                canvas.drawText(displayName, itemX + 25f, y + 40f, renderer.subtitlePaint)
                
                // 房间号
                renderer.subtitlePaint.textSize = 36f
                renderer.subtitlePaint.color = Color.parseColor("#FFD700")
                canvas.drawText("房间号: ${room.roomCode}", itemX + 25f, y + 75f, renderer.subtitlePaint)

                // 玩家数量和IP
                renderer.subtitlePaint.textSize = 38f
                renderer.subtitlePaint.textAlign = Paint.Align.RIGHT
                renderer.subtitlePaint.color = Color.parseColor("#4CAF50")
                canvas.drawText("${room.playerCount}/${room.maxPlayers}", 
                    itemX + itemWidth - 25f, y + 45f, renderer.subtitlePaint)
                
                renderer.subtitlePaint.textSize = 32f
                renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
                canvas.drawText("IP: ${room.hostIp}", itemX + itemWidth - 25f, y + 80f, renderer.subtitlePaint)
            }
        }

        // 返回按钮 - 改进设计
        val returnBtnY = h * 0.9f
        drawButtonWithGlow(canvas, w / 2f, returnBtnY, 400f, 80f,
            "← 返回", Color.parseColor("#757575"), backToMenuBtnRect)

        // 恢复设置
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    }
    
    /**
     * 绘制加载动画
     */
    private fun drawLoadingAnimation(canvas: Canvas, cx: Float, cy: Float) {
        val radius = 30f
        val rotation = (renderer.globalTime * 360f) % 360f
        
        // 外圆旋转
        renderer.paint.color = Color.argb(200, 33, 150, 243)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 4f
        
        val arcPaint = Paint().apply {
            color = Color.argb(200, 33, 150, 243)
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        
        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 0f, 120f, false, arcPaint)
        canvas.restore()
        
        // 中心点
        renderer.paint.style = Paint.Style.FILL
        renderer.paint.color = Color.parseColor("#64B5F6")
        canvas.drawCircle(cx, cy, 8f, renderer.paint)
    }

    /**
     * 渲染等待房间界面 — 房主看到房间码+玩家列表+开始/踢人按钮
     * 客户端看到玩家列表+准备按钮
     */
    fun renderRoomWaiting(canvas: Canvas, w: Int, h: Int, roomManager: RoomManager) {
        renderer.paint.color = Color.parseColor("#0A0F19")
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
        
        // 背景网格
        drawGridBackground(canvas, w, h)

        val isHost = roomManager.isHost
        val roomCode = roomManager.roomCode
        val players = roomManager.lobbyPlayers

        // ===== 顶部标题区 =====
        val titleBarHeight = h * 0.12f
        val titleBgPaint = Paint().apply {
            color = Color.argb(80, 153, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), titleBarHeight, titleBgPaint)
        
        renderer.paint.color = Color.parseColor("#FF6B35")
        renderer.paint.strokeWidth = 3f
        canvas.drawLine(0f, titleBarHeight - 5f, w.toFloat(), titleBarHeight - 5f, renderer.paint)

        // 标题
        renderer.titlePaint.textSize = 80f
        renderer.titlePaint.color = Color.parseColor("#FFD700")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.setShadowLayer(8f, 0f, 4f, Color.BLACK)
        canvas.drawText("⏳ 等待开始", w / 2f, h * 0.08f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        // ===== 房间码区（仅房主显示） =====
        if (isHost) {
            drawRoomCodeDisplay(canvas, w, h, roomCode)
        }

        // ===== 玩家列表区 =====
        drawPlayerListSection(canvas, w, h, players, isHost)

        // ===== 底部按钮区 =====
        val btnAreaY = h * 0.82f
        if (isHost) {
            drawHostButtons(canvas, w, h, btnAreaY, players)
        } else {
            drawClientButtons(canvas, w, h, btnAreaY, players)
        }

        // 恢复设置
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    }
    
    /**
     * 绘制房间码显示区域
     */
    private fun drawRoomCodeDisplay(canvas: Canvas, w: Int, h: Int, roomCode: String) {
        val boxX = w * 0.1f
        val boxY = h * 0.15f
        val boxWidth = w * 0.35f
        val boxHeight = h * 0.12f
        
        // 背景框
        val bgPaint = Paint().apply {
            color = Color.argb(100, 153, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 12f, 12f, bgPaint)
        
        // 边框
        renderer.paint.color = Color.parseColor("#FFD700")
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 12f, 12f, renderer.paint)
        
        // 标题
        renderer.subtitlePaint.textSize = 32f
        renderer.subtitlePaint.color = Color.parseColor("#888888")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("房间号", boxX + boxWidth / 2, boxY + 28f, renderer.subtitlePaint)
        
        // 房间码（大号）
        renderer.titlePaint.textSize = 100f
        renderer.titlePaint.color = Color.parseColor("#FFD700")
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(roomCode, boxX + boxWidth / 2, boxY + 88f, renderer.titlePaint)
        
        // 右侧玩家计数
        val countX = w * 0.55f
        drawPlayerCountBox(canvas, countX, boxY, boxHeight)
    }
    
    /**
     * 绘制玩家计数框
     */
    private fun drawPlayerCountBox(canvas: Canvas, x: Float, y: Float, height: Float) {
        val size = height
        val bgPaint = Paint().apply {
            color = Color.argb(100, 76, 175, 80)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(x, y, x + size, y + size, 12f, 12f, bgPaint)
        
        renderer.paint.color = Color.parseColor("#4CAF50")
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(x, y, x + size, y + size, 12f, 12f, renderer.paint)
        
        renderer.subtitlePaint.textSize = 32f
        renderer.subtitlePaint.color = Color.parseColor("#4CAF50")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("👥", x + size / 2, y + 40f, renderer.subtitlePaint)
    }
    
    /**
     * 绘制玩家列表区
     */
    private fun drawPlayerListSection(canvas: Canvas, w: Int, h: Int, players: List<LobbyPlayer>, isHost: Boolean) {
        val sectionX = w * 0.1f
        val sectionY = h * 0.28f
        val sectionWidth = w * 0.8f
        val itemHeight = 90f
        val itemSpacing = 12f
        
        // 标题
        renderer.subtitlePaint.textSize = 40f
        renderer.subtitlePaint.color = Color.parseColor("#CCAA88")
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText("👥 玩家列表", sectionX, sectionY, renderer.subtitlePaint)
        
        kickPlayerBtnRects.clear()
        
        val listY = sectionY + 50f
        for ((index, player) in players.withIndex()) {
            val y = listY + index * (itemHeight + itemSpacing)
            if (y > h * 0.75f) break
            
            drawPlayerItemCard(canvas, sectionX, y, sectionWidth, itemHeight, player, isHost)
        }
    }
    
    /**
     * 绘制单个玩家卡片
     */
    private fun drawPlayerItemCard(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
                                   player: LobbyPlayer, isHost: Boolean) {
        // 背景 - 根据准备状态改变颜色
        val bgColor = if (player.isReady) Color.argb(80, 76, 175, 80) else Color.argb(60, 100, 100, 150)
        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(x, y, x + width, y + height, 10f, 10f, bgPaint)
        
        // 边框
        val borderColor = if (player.isReady) Color.parseColor("#4CAF50") else Color.parseColor("#64B5F6")
        renderer.paint.color = borderColor
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(x, y, x + width, y + height, 10f, 10f, renderer.paint)
        
        // 玩家角标（房主/准备状态）
        val badgeX = x + 25f
        val badgeY = y + 25f
        if (player.isHost) {
            renderer.paint.color = Color.parseColor("#FFD700")
            renderer.paint.style = Paint.Style.FILL
            canvas.drawCircle(badgeX, badgeY, 10f, renderer.paint)
            
            renderer.subtitlePaint.textSize = 32f
            renderer.subtitlePaint.color = Color.BLACK
            renderer.subtitlePaint.textAlign = Paint.Align.CENTER
            canvas.drawText("👑", badgeX, badgeY + 12f, renderer.subtitlePaint)
        }
        
        // 玩家名称
        renderer.subtitlePaint.textSize = 45f
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.color = Color.WHITE
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        val displayName = if (player.isHost) "${player.name} (房主)" else player.name
        canvas.drawText(displayName, x + 60f, y + 50f, renderer.subtitlePaint)
        
        // 准备状态 - 大型展示
        val statusX = x + width - 120f
        val statusColor = if (player.isReady) Color.parseColor("#4CAF50") else Color.parseColor("#FF5252")
        renderer.subtitlePaint.textSize = 48f
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.color = statusColor
        val statusEmoji = if (player.isReady) "✓" else "○"
        canvas.drawText(statusEmoji, statusX, y + 55f, renderer.subtitlePaint)
        
        renderer.subtitlePaint.textSize = 32f
        renderer.subtitlePaint.color = statusColor
        val statusText = if (player.isReady) "已准备" else "未准备"
        canvas.drawText(statusText, statusX, y + 75f, renderer.subtitlePaint)
        
        // 踢人按钮（仅房主看到，且不是自己）
        if (isHost && !player.isHost) {
            val kickBtnX = x + width - 70f
            val kickBtnY = y + height / 2 - 35f
            val kickBtnSize = 70f
            val kickRect = RectF(kickBtnX, kickBtnY, kickBtnX + kickBtnSize, kickBtnY + kickBtnSize)
            kickPlayerBtnRects.add(kickRect)
            
            // 按钮背景
            renderer.paint.color = Color.argb(200, 255, 82, 82)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRoundRect(kickRect, 8f, 8f, renderer.paint)
            
            // 按钮文字
            renderer.subtitlePaint.textSize = 32f
            renderer.subtitlePaint.textAlign = Paint.Align.CENTER
            renderer.subtitlePaint.color = Color.WHITE
            canvas.drawText("✕", kickBtnX + kickBtnSize / 2, kickBtnY + 45f, renderer.subtitlePaint)
        }
    }
    
    /**
     * 绘制房主的底部按钮（开始游戏）
     */
    private fun drawHostButtons(canvas: Canvas, w: Int, h: Int, btnY: Float, players: List<LobbyPlayer>) {
        val allReady = players.all { it.isReady } && players.size >= 2
        
        renderer.subtitlePaint.textSize = 55f
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        
        if (allReady) {
            // 闪烁效果
            val pulse = (sin(renderer.globalTime * 3f) + 1f) * 0.5f
            val alpha = (120 + pulse * 135).toInt()
            renderer.subtitlePaint.color = Color.argb(alpha, 76, 175, 80)
            
            startGameBtnRect.set(w / 2f - 250f, btnY - 50f, w / 2f + 250f, btnY + 50f)
            
            // 发光背景
            val glowPaint = Paint().apply {
                color = Color.argb(40, 76, 175, 80)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(startGameBtnRect.left - 15f, startGameBtnRect.top - 15f,
                startGameBtnRect.right + 15f, startGameBtnRect.bottom + 15f,
                16f, 16f, glowPaint)
            
            // 按钮
            renderer.paint.color = Color.parseColor("#4CAF50")
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRoundRect(startGameBtnRect, 16f, 16f, renderer.paint)
            
            // 按钮边框
            renderer.paint.color = Color.argb(200, 255, 255, 255)
            renderer.paint.style = Paint.Style.STROKE
            renderer.paint.strokeWidth = 2f
            canvas.drawRoundRect(startGameBtnRect, 16f, 16f, renderer.paint)
            
            canvas.drawText("🚀 开始游戏", w / 2f, btnY + 18f, renderer.subtitlePaint)
        } else {
            renderer.subtitlePaint.color = Color.parseColor("#888888")
            val hint = if (players.size < 2) "等待玩家加入..." else "等待玩家准备..."
            canvas.drawText(hint, w / 2f, btnY + 18f, renderer.subtitlePaint)
            startGameBtnRect.set(0f, 0f, 0f, 0f)
        }
        
        // 离开房间按钮
        val leaveBtnY = btnY + 100f
        drawButtonWithGlow(canvas, w / 2f, leaveBtnY, 380f, 75f,
            "← 离开房间", Color.parseColor("#FF5252"), leaveRoomBtnRect)
    }
    
    /**
     * 绘制客户端的底部按钮（准备）
     */
    private fun drawClientButtons(canvas: Canvas, w: Int, h: Int, btnY: Float, players: List<LobbyPlayer>) {
        val myPlayer = players.firstOrNull { !it.isHost }
        val isReady = myPlayer?.isReady == true
        
        val readyColor = if (isReady) Color.parseColor("#4CAF50") else Color.parseColor("#2196F3")
        val readyText = if (isReady) "✓ 已准备" else "⏳ 准备"
        
        if (!isReady) {
            // 未准备状态：闪烁
            val pulse = (sin(renderer.globalTime * 3f) + 1f) * 0.5f
            val alpha = (120 + pulse * 135).toInt()
            renderer.paint.color = Color.argb(alpha / 2, 33, 150, 243)
        }
        
        readyBtnRect.set(w / 2f - 250f, btnY - 50f, w / 2f + 250f, btnY + 50f)
        
        renderer.paint.color = readyColor
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRoundRect(readyBtnRect, 16f, 16f, renderer.paint)
        
        renderer.paint.color = Color.argb(200, 255, 255, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(readyBtnRect, 16f, 16f, renderer.paint)
        
        renderer.subtitlePaint.textSize = 55f
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.color = Color.WHITE
        canvas.drawText(readyText, w / 2f, btnY + 18f, renderer.subtitlePaint)
        
        // 离开房间按钮
        val leaveBtnY = btnY + 100f
        drawButtonWithGlow(canvas, w / 2f, leaveBtnY, 380f, 75f,
            "← 离开房间", Color.parseColor("#FF5252"), leaveRoomBtnRect)
    }

    fun renderGameOver(canvas: Canvas, w: Int, h: Int) {
        // 背景渐变 - 暗红色
        renderer.paint.color = Color.argb(220, 40, 10, 10)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
        
        // 网格背景
        drawGridBackground(canvas, w, h)
        
        // 颜色闪烁效果
        val pulse = (sin(renderer.globalTime * 2f) + 1f) * 0.5f
        val overlayAlpha = (100 + pulse * 80).toInt()
        renderer.paint.color = Color.argb(overlayAlpha, 255, 100, 100)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // 标题 - 带阴影
        renderer.titlePaint.textSize = 180f
        renderer.titlePaint.color = Color.parseColor("#FF4444")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.setShadowLayer(15f, 0f, 5f, Color.BLACK)
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("💀 游戏结束", w / 2f, h * 0.35f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        // 提示文字
        renderer.subtitlePaint.textSize = 60f
        renderer.subtitlePaint.color = Color.parseColor("#FFAA88")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("点击任意位置返回主菜单", w / 2f, h * 0.6f, renderer.subtitlePaint)
    }

    fun renderVictory(canvas: Canvas, w: Int, h: Int) {
        // 背景渐变 - 暗绿色
        renderer.paint.color = Color.argb(220, 10, 40, 10)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
        
        // 网格背景
        drawGridBackground(canvas, w, h)
        
        // 颜色闪烁效果
        val pulse = (sin(renderer.globalTime * 1.5f) + 1f) * 0.5f
        val overlayAlpha = (100 + pulse * 80).toInt()
        renderer.paint.color = Color.argb(overlayAlpha, 100, 255, 100)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)

        // 标题 - 带阴影
        renderer.titlePaint.textSize = 200f
        renderer.titlePaint.color = Color.parseColor("#FFD700")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.setShadowLayer(15f, 0f, 5f, Color.BLACK)
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("🎉 通关成功!", w / 2f, h * 0.35f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        // 提示文字
        renderer.subtitlePaint.textSize = 60f
        renderer.subtitlePaint.color = Color.parseColor("#DDDD88")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("点击任意位置返回主菜单", w / 2f, h * 0.6f, renderer.subtitlePaint)
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
