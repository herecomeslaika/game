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
    val optionsBtnRect = RectF()
    val exitBtnRect = RectF()
    val confirmOkBtnRect = RectF()
    val confirmCancelBtnRect = RectF()
    val optionsBackBtnRect = RectF()
    val optionsBgmSliderRect = RectF()
    val optionsSfxSliderRect = RectF()
    val optionsMuteBtnRect = RectF()
    val optionsMainBgmBtnRect = RectF()
    val optionsBattleBgmBtnRect = RectF()
    val optionsBossBgmBtnRect = RectF()
    val optionsStopBgmBtnRect = RectF()
    
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

        val titleX = w * 0.08f
        val titleY = h * 0.28f
        val menuStartY = h * 0.46f
        val titleFontSize = (h * 0.12f).coerceIn(96f, 160f)
        val menuFontSize = (h * 0.058f).coerceIn(48f, 82f)
        val btnSpacing = (h * 0.11f).coerceIn(78f, 135f)
        val btnWidth = w * 0.28f
        val btnTopPadding = menuFontSize * 0.72f
        val btnBottomPadding = menuFontSize * 0.24f
        val versionY = h * 0.955f

        renderer.titlePaint.textSize = titleFontSize
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.textAlign = Paint.Align.LEFT
        renderer.titlePaint.setShadowLayer(6f, 2f, 2f, Color.argb(160, 0, 0, 0))
        renderer.titlePaint.color = Color.parseColor("#990000")
        renderer.titlePaint.style = Paint.Style.FILL
        renderer.titlePaint.strokeWidth = 0f
        canvas.drawText("冥途", titleX, titleY, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()
        renderer.titlePaint.strokeWidth = 0f
        renderer.titlePaint.style = Paint.Style.FILL

        val btnX = titleX
        var btnY = menuStartY

        val alpha = ((sin(renderer.globalTime * 2f) + 1) * 127 + 128).toInt()
        renderer.subtitlePaint.color = Color.argb(alpha, 255, 215, 0)
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.textSize = menuFontSize
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        startBtnRect.set(btnX - 20f, btnY - btnTopPadding, btnX + btnWidth, btnY + btnBottomPadding)
        canvas.drawText("开始游戏", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        renderer.subtitlePaint.color = Color.parseColor("#88FF88")
        multiplayerBtnRect.set(btnX - 20f, btnY - btnTopPadding, btnX + btnWidth, btnY + btnBottomPadding)
        canvas.drawText("联机模式", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
        optionsBtnRect.set(btnX - 20f, btnY - btnTopPadding, btnX + btnWidth, btnY + btnBottomPadding)
        canvas.drawText("选项", btnX, btnY, renderer.subtitlePaint)
        btnY += btnSpacing

        exitBtnRect.set(btnX - 20f, btnY - btnTopPadding, btnX + btnWidth, btnY + btnBottomPadding)
        canvas.drawText("退出游戏", btnX, btnY, renderer.subtitlePaint)

        val bitmapAspect = menuPlayerBitmap.width.toFloat() / menuPlayerBitmap.height.toFloat()
        val maxBitmapHeight = h * 0.92f
        val maxBitmapWidth = w * 0.42f
        val bitmapHeight = min(maxBitmapHeight, maxBitmapWidth / bitmapAspect)
        val bitmapWidth = bitmapHeight * bitmapAspect
        val cx = w * 0.80f
        val cy = h * 0.55f
        val left = cx - bitmapWidth / 2f
        val top = cy - bitmapHeight / 2f
        val dstRect = RectF(left, top, left + bitmapWidth, top + bitmapHeight)
        canvas.drawBitmap(menuPlayerBitmap, null, dstRect, renderer.paint)

        renderer.subtitlePaint.textSize = (h * 0.022f).coerceIn(22f, 30f)
        renderer.subtitlePaint.color = Color.parseColor("#666666")
        canvas.drawText("冥途 v1.0", titleX, versionY, renderer.subtitlePaint)

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
        canvas.drawText("联机大厅", w / 2f, h * 0.15f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        val btnCenterX = w / 2f
        var btnY = h * 0.32f
        val btnWidth = 520f
        val btnHeight = 100f
        val btnSpacing = 160f

        // 创建房间按钮
        drawButtonWithGlow(canvas, btnCenterX, btnY, btnWidth, btnHeight, 
            "创建房间", Color.parseColor("#4CAF50"), createRoomBtnRect)
        btnY += btnSpacing

        // 加入房间按钮
        drawButtonWithGlow(canvas, btnCenterX, btnY, btnWidth, btnHeight,
            "加入房间", Color.parseColor("#2196F3"), joinRoomBtnRect)
        btnY += btnSpacing

        // 返回按钮
        drawButtonWithGlow(canvas, btnCenterX, btnY, btnWidth, btnHeight,
            "返回主菜单", Color.parseColor("#757575"), backToMenuBtnRect)

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
     * 绘制简约按钮 - 只显示文字，无底纹
     */
    private fun drawButtonWithGlow(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float,
                                   text: String, color: Int, rect: RectF) {
        // 更新点击区域
        rect.set(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2)
        
        // 只绘制文字 - 无底纹
        renderer.subtitlePaint.textSize = 65f
        renderer.subtitlePaint.color = color
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(text, cx, cy + 23f, renderer.subtitlePaint)
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
        canvas.drawText("可用房间", w / 2f, h * 0.1f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        val listY = h * 0.2f
        val itemHeight = 90f
        val itemSpacing = 10f
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

                // 房间背景 - 简约风格
                val bgPaint = Paint().apply {
                    color = Color.argb(50, 33, 150, 243)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, 10f, 10f, bgPaint)
                
                // 房间边框 - 简约
                renderer.paint.color = Color.argb(150, 33, 150, 243)
                renderer.paint.style = Paint.Style.STROKE
                renderer.paint.strokeWidth = 1.5f
                canvas.drawRoundRect(rect, 10f, 10f, renderer.paint)

                // 房间名称和房间号
                renderer.subtitlePaint.textSize = 42f
                renderer.subtitlePaint.textAlign = Paint.Align.LEFT
                renderer.subtitlePaint.color = Color.WHITE
                renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
                val displayName = room.roomName
                canvas.drawText(displayName, itemX + 20f, y + 32f, renderer.subtitlePaint)
                
                // 房间号
                renderer.subtitlePaint.textSize = 32f
                renderer.subtitlePaint.color = Color.parseColor("#FFD700")
                canvas.drawText("房间号: ${room.roomCode}", itemX + 20f, y + 62f, renderer.subtitlePaint)

                // 玩家数量和IP
                renderer.subtitlePaint.textSize = 34f
                renderer.subtitlePaint.textAlign = Paint.Align.RIGHT
                renderer.subtitlePaint.color = Color.parseColor("#4CAF50")
                canvas.drawText("${room.playerCount}/${room.maxPlayers}", 
                    itemX + itemWidth - 20f, y + 32f, renderer.subtitlePaint)
                
                renderer.subtitlePaint.textSize = 28f
                renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
                canvas.drawText("IP: ${room.hostIp}", itemX + itemWidth - 20f, y + 62f, renderer.subtitlePaint)
            }
        }

        // 返回按钮
        val returnBtnY = h * 0.9f
        drawButtonWithGlow(canvas, w / 2f, returnBtnY, 360f, 70f,
            "返回", Color.parseColor("#757575"), backToMenuBtnRect)

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
     * 渲染等待房间界面 — 优化的紧凑布局
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
        val titleBarHeight = h * 0.1f
        val titleBgPaint = Paint().apply {
            color = Color.argb(60, 153, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), titleBarHeight, titleBgPaint)
        
        renderer.paint.color = Color.parseColor("#FF6B35")
        renderer.paint.strokeWidth = 2f
        canvas.drawLine(0f, titleBarHeight - 3f, w.toFloat(), titleBarHeight - 3f, renderer.paint)

        // 标题
        renderer.titlePaint.textSize = 70f
        renderer.titlePaint.color = Color.parseColor("#FFD700")
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.setShadowLayer(6f, 0f, 3f, Color.BLACK)
        canvas.drawText("等待开始", w / 2f, h * 0.065f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        // ===== 房间码和玩家计数（仅房主显示，水平布局） =====
        if (isHost) {
            // 房间号左侧显示
            renderer.subtitlePaint.textSize = 32f
            renderer.subtitlePaint.color = Color.parseColor("#888888")
            renderer.subtitlePaint.textAlign = Paint.Align.LEFT
            canvas.drawText("房间号: ", w * 0.1f, h * 0.15f, renderer.subtitlePaint)
            
            renderer.titlePaint.textSize = 72f
            renderer.titlePaint.color = Color.parseColor("#FFD700")
            renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(roomCode, w * 0.28f, h * 0.15f, renderer.titlePaint)
            
            // 玩家计数右侧显示
            renderer.subtitlePaint.textSize = 32f
            renderer.subtitlePaint.color = Color.parseColor("#AAAAAA")
            renderer.subtitlePaint.textAlign = Paint.Align.RIGHT
            val playerCount = "${players.size}/${4}"  // 最多4人
            canvas.drawText("玩家: $playerCount", w * 0.9f, h * 0.15f, renderer.subtitlePaint)
        }

        // ===== 玩家列表区 =====
        drawPlayerListSection(canvas, w, h, players, isHost)

        // ===== 底部按钮区 =====
        val btnAreaY = h * 0.78f
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
     * 绘制玩家列表区
     */
    private fun drawPlayerListSection(canvas: Canvas, w: Int, h: Int, players: List<LobbyPlayer>, isHost: Boolean) {
        val sectionX = w * 0.08f
        val sectionY = h * 0.22f
        val sectionWidth = w * 0.84f
        val itemHeight = 70f
        val itemSpacing = 6f
        
        // 标题
        renderer.subtitlePaint.textSize = 36f
        renderer.subtitlePaint.color = Color.parseColor("#CCAA88")
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText("玩家列表", sectionX, sectionY - 5f, renderer.subtitlePaint)
        
        kickPlayerBtnRects.clear()
        
        val listY = sectionY + 25f
        for ((index, player) in players.withIndex()) {
            val y = listY + index * (itemHeight + itemSpacing)
            if (y + itemHeight > h * 0.70f) break  // 为按钮区域留出空间
            
            drawPlayerItemCard(canvas, sectionX, y, sectionWidth, itemHeight, player, isHost)
        }
    }
    
    /**
     * 绘制单个玩家卡片
     */
    private fun drawPlayerItemCard(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
                                   player: LobbyPlayer, isHost: Boolean) {
        // 背景 - 根据准备状态改变颜色
        val bgColor = if (player.isReady) Color.argb(70, 76, 175, 80) else Color.argb(50, 100, 100, 150)
        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, bgPaint)
        
        // 边框 - 简约风格
        val borderColor = if (player.isReady) Color.parseColor("#4CAF50") else Color.parseColor("#64B5F6")
        renderer.paint.color = borderColor
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 1.5f
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, renderer.paint)
        
        // 玩家名称 - 简化布局
        renderer.subtitlePaint.textSize = 38f
        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.color = Color.WHITE
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        val displayName = if (player.isHost) "${player.name}(房主)" else player.name
        canvas.drawText(displayName, x + 15f, y + 43f, renderer.subtitlePaint)
        
        // 准备状态 - 紧凑显示
        val statusColor = if (player.isReady) Color.parseColor("#4CAF50") else Color.parseColor("#FF5252")
        renderer.subtitlePaint.textSize = 28f
        renderer.subtitlePaint.textAlign = Paint.Align.RIGHT
        renderer.subtitlePaint.color = statusColor
        val statusText = if (player.isReady) "已准备" else "未准备"
        canvas.drawText(statusText, x + width - 80f, y + 43f, renderer.subtitlePaint)
        
        // 踢人按钮（仅房主看到，且不是自己）
        if (isHost && !player.isHost) {
            val kickBtnX = x + width - 60f
            val kickBtnY = y + 8f
            val kickBtnSize = height - 16f
            val kickRect = RectF(kickBtnX, kickBtnY, kickBtnX + kickBtnSize, kickBtnY + kickBtnSize)
            kickPlayerBtnRects.add(kickRect)
            
            // 按钮背景 - 简约
            renderer.paint.color = Color.argb(180, 255, 82, 82)
            renderer.paint.style = Paint.Style.FILL
            canvas.drawRoundRect(kickRect, 6f, 6f, renderer.paint)
            
            // 按钮文字
            renderer.subtitlePaint.textSize = 28f
            renderer.subtitlePaint.textAlign = Paint.Align.CENTER
            renderer.subtitlePaint.color = Color.WHITE
            canvas.drawText("×", kickBtnX + kickBtnSize / 2, kickBtnY + kickBtnSize / 2 + 10f, renderer.subtitlePaint)
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
            
            canvas.drawText("开始游戏", w / 2f, btnY + 18f, renderer.subtitlePaint)
        } else {
            renderer.subtitlePaint.color = Color.parseColor("#888888")
            val hint = if (players.size < 2) "等待玩家加入..." else "等待玩家准备..."
            canvas.drawText(hint, w / 2f, btnY + 18f, renderer.subtitlePaint)
            startGameBtnRect.set(0f, 0f, 0f, 0f)
        }
        
        // 离开房间按钮
        val leaveBtnY = btnY + 100f
        drawButtonWithGlow(canvas, w / 2f, leaveBtnY, 360f, 70f,
            "离开房间", Color.parseColor("#FF5252"), leaveRoomBtnRect)
    }
    
    /**
     * 绘制客户端的底部按钮（准备）
     */
    private fun drawClientButtons(canvas: Canvas, w: Int, h: Int, btnY: Float, players: List<LobbyPlayer>) {
        val myPlayer = players.firstOrNull { !it.isHost }
        val isReady = myPlayer?.isReady == true
        
        val readyColor = if (isReady) Color.parseColor("#4CAF50") else Color.parseColor("#2196F3")
        val readyText = if (isReady) "已准备" else "准备游戏"
        
        readyBtnRect.set(w / 2f - 240f, btnY - 45f, w / 2f + 240f, btnY + 45f)
        
        // 轻微闪烁效果（未准备时）
        val pulse = (sin(renderer.globalTime * 3f) + 1f) * 0.5f
        val alpha = if (isReady) 255 else (200 + pulse * 55).toInt()
        
        renderer.paint.color = Color.argb(alpha, (readyColor shr 16) and 0xFF, (readyColor shr 8) and 0xFF, readyColor and 0xFF)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRoundRect(readyBtnRect, 12f, 12f, renderer.paint)
        
        // 按钮边框
        renderer.paint.color = Color.argb(150, 255, 255, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 1.5f
        canvas.drawRoundRect(readyBtnRect, 12f, 12f, renderer.paint)
        
        renderer.subtitlePaint.textSize = 50f
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.color = Color.WHITE
        canvas.drawText(readyText, w / 2f, btnY + 16f, renderer.subtitlePaint)
        
        // 离开房间按钮
        val leaveBtnY = btnY + 100f
        drawButtonWithGlow(canvas, w / 2f, leaveBtnY, 360f, 70f,
            "离开房间", Color.parseColor("#FF5252"), leaveRoomBtnRect)
    }
    fun renderOptions(canvas: Canvas, w: Int, h: Int, bgmVolume: Float, sfxVolume: Float, muted: Boolean) {
        val panel = RectF(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.82f)
        renderer.paint.color = Color.argb(210, 10, 16, 28)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRoundRect(panel, 22f, 22f, renderer.paint)

        renderer.paint.color = Color.argb(180, 255, 255, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(panel, 22f, 22f, renderer.paint)

        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.color = Color.parseColor("#E5E7EB")
        renderer.titlePaint.textSize = 72f
        canvas.drawText("选项", panel.centerX(), panel.top + 80f, renderer.titlePaint)

        val left = panel.left + 60f
        val sliderLeft = left + 220f
        val sliderRight = panel.right - 80f

        renderVolumeRow(
            canvas = canvas,
            label = "背景音乐",
            value = bgmVolume,
            y = panel.top + 180f,
            labelX = left,
            sliderLeft = sliderLeft,
            sliderRight = sliderRight,
            rect = optionsBgmSliderRect,
            color = Color.parseColor("#60A5FA")
        )
        renderVolumeRow(
            canvas = canvas,
            label = "游戏音效",
            value = sfxVolume,
            y = panel.top + 270f,
            labelX = left,
            sliderLeft = sliderLeft,
            sliderRight = sliderRight,
            rect = optionsSfxSliderRect,
            color = Color.parseColor("#34D399")
        )

        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.color = Color.parseColor("#D1D5DB")
        renderer.subtitlePaint.textSize = 30f
        canvas.drawText("背景音乐试听", left, panel.top + 365f, renderer.subtitlePaint)

        val btnY = panel.top + 430f
        drawButtonWithGlow(canvas, left + 95f, btnY, 170f, 64f, "Main", Color.parseColor("#8B5CF6"), optionsMainBgmBtnRect)
        drawButtonWithGlow(canvas, left + 285f, btnY, 170f, 64f, "Battle", Color.parseColor("#2563EB"), optionsBattleBgmBtnRect)
        drawButtonWithGlow(canvas, left + 475f, btnY, 170f, 64f, "Boss", Color.parseColor("#DC2626"), optionsBossBgmBtnRect)
        drawButtonWithGlow(canvas, left + 665f, btnY, 170f, 64f, "停止", Color.parseColor("#6B7280"), optionsStopBgmBtnRect)

        val muteText = if (muted) "取消静音" else "一键静音"
        val muteColor = if (muted) Color.parseColor("#F59E0B") else Color.parseColor("#8B5CF6")
        drawButtonWithGlow(canvas, panel.centerX(), panel.top + 525f, 260f, 68f, muteText, muteColor, optionsMuteBtnRect)

        val backCx = panel.centerX()
        val backCy = panel.bottom - 70f
        val backWidth = 240f
        val backHeight = 72f
        optionsBackBtnRect.set(backCx - backWidth / 2, backCy - backHeight / 2, backCx + backWidth / 2, backCy + backHeight / 2)
        drawButtonWithGlow(canvas, backCx, backCy, backWidth, backHeight, "返回", Color.parseColor("#6B7280"), optionsBackBtnRect)
    }

    private fun renderVolumeRow(
        canvas: Canvas,
        label: String,
        value: Float,
        y: Float,
        labelX: Float,
        sliderLeft: Float,
        sliderRight: Float,
        rect: RectF,
        color: Int
    ) {
        val clamped = value.coerceIn(0f, 1f)
        val sliderHeight = 18f
        val hitHeight = 64f
        rect.set(sliderLeft, y - hitHeight / 2f, sliderRight, y + hitHeight / 2f)

        renderer.subtitlePaint.textAlign = Paint.Align.LEFT
        renderer.subtitlePaint.color = Color.parseColor("#E5E7EB")
        renderer.subtitlePaint.textSize = 32f
        canvas.drawText(label, labelX, y + 11f, renderer.subtitlePaint)

        renderer.paint.style = Paint.Style.FILL
        renderer.paint.color = Color.argb(160, 31, 41, 55)
        canvas.drawRoundRect(
            sliderLeft,
            y - sliderHeight / 2f,
            sliderRight,
            y + sliderHeight / 2f,
            9f,
            9f,
            renderer.paint
        )

        val fillRight = sliderLeft + (sliderRight - sliderLeft) * clamped
        renderer.paint.color = color
        canvas.drawRoundRect(
            sliderLeft,
            y - sliderHeight / 2f,
            fillRight,
            y + sliderHeight / 2f,
            9f,
            9f,
            renderer.paint
        )

        renderer.paint.color = Color.WHITE
        canvas.drawCircle(fillRight, y, 18f, renderer.paint)
        renderer.paint.color = color
        canvas.drawCircle(fillRight, y, 10f, renderer.paint)

        renderer.subtitlePaint.textAlign = Paint.Align.RIGHT
        renderer.subtitlePaint.color = Color.parseColor("#D1D5DB")
        renderer.subtitlePaint.textSize = 28f
        canvas.drawText("${(clamped * 100).toInt()}%", sliderRight, y - 26f, renderer.subtitlePaint)
    }

    fun renderExitConfirm(canvas: Canvas, w: Int, h: Int, fromPlaying: Boolean) {
        val panel = RectF(w * 0.28f, h * 0.30f, w * 0.72f, h * 0.68f)
        renderer.paint.color = Color.argb(225, 12, 18, 30)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRoundRect(panel, 20f, 20f, renderer.paint)

        renderer.paint.color = Color.argb(170, 255, 255, 255)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2f
        canvas.drawRoundRect(panel, 20f, 20f, renderer.paint)

        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.color = Color.parseColor("#F3F4F6")
        renderer.titlePaint.textSize = 58f
        canvas.drawText("确认退出？", panel.centerX(), panel.top + 80f, renderer.titlePaint)

        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.color = Color.parseColor("#D1D5DB")
        renderer.subtitlePaint.textSize = 30f
        val message = if (fromPlaying) "返回主菜单会结束当前战斗" else "确定要关闭游戏吗"
        canvas.drawText(message, panel.centerX(), panel.top + 150f, renderer.subtitlePaint)

        val btnCy = panel.bottom - 70f
        drawButtonWithGlow(canvas, panel.centerX() - 130f, btnCy, 180f, 68f, "取消", Color.parseColor("#6B7280"), confirmCancelBtnRect)
        drawButtonWithGlow(canvas, panel.centerX() + 130f, btnCy, 180f, 68f, "确认", Color.parseColor("#EF4444"), confirmOkBtnRect)
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
        canvas.drawText("游戏结束", w / 2f, h * 0.35f, renderer.titlePaint)
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
        canvas.drawText("通关成功", w / 2f, h * 0.35f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()

        // 提示文字
        renderer.subtitlePaint.textSize = 60f
        renderer.subtitlePaint.color = Color.parseColor("#DDDD88")
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("点击任意位置返回主菜单", w / 2f, h * 0.6f, renderer.subtitlePaint)
    }

    fun renderIntroStory(canvas: Canvas, w: Int, h: Int) {
        drawStoryBackdrop(canvas, w, h, Color.rgb(9, 8, 18), Color.rgb(38, 16, 44))
        drawStoryFrame(canvas, w, h)
        drawStoryTitle(canvas, w, h, "被夺走的黎明", Color.parseColor("#F7D56B"))

        val lines = listOf(
            "王国最后一束星火在冥界之门前熄灭。",
            "公主艾莉娅被三位守门者带入深渊，塔耳塔洛斯的骨墙、阿斯福德的烈焰、伊利西昂的冠军挡住了归途。",
            "你是王国最后的骑士。穿过三层冥途，击败守门者，把她带回黎明。"
        )
        drawStoryText(canvas, w, h, lines, h * 0.36f)
        drawStoryHint(canvas, w, h, "点击任意位置跳过")
    }

    fun renderEndingStory(canvas: Canvas, w: Int, h: Int) {
        drawStoryBackdrop(canvas, w, h, Color.rgb(8, 18, 14), Color.rgb(36, 45, 24))
        drawStoryFrame(canvas, w, h)
        drawStoryTitle(canvas, w, h, "黎明归来", Color.parseColor("#FFE68A"))

        val lines = listOf(
            "冥界王座崩塌，三位守门者的誓约化作尘光。",
            "你在破晓前找到了公主，长夜的诅咒从她身上褪去，王国的钟声越过山谷。",
            "骑士归来，黎明得救。"
        )
        drawStoryText(canvas, w, h, lines, h * 0.36f)
        drawStoryHint(canvas, w, h, "点击任意位置继续")
    }

    private fun drawStoryBackdrop(canvas: Canvas, w: Int, h: Int, topColor: Int, bottomColor: Int) {
        renderer.resetPaint()
        if (renderer.enableShaders) {
            renderer.paint.shader = renderer.makeLinearGradient(0f, 0f, 0f, h.toFloat(), topColor, bottomColor)
        } else {
            renderer.paint.color = bottomColor
        }
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
        renderer.paint.shader = null

        drawGridBackground(canvas, w, h)
        renderer.paint.color = Color.argb(120, 0, 0, 0)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), renderer.paint)
    }

    private fun drawStoryFrame(canvas: Canvas, w: Int, h: Int) {
        val panel = RectF(w * 0.16f, h * 0.18f, w * 0.84f, h * 0.82f)
        renderer.paint.color = Color.argb(170, 9, 10, 20)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRoundRect(panel, 18f, 18f, renderer.paint)

        renderer.paint.color = Color.argb(150, 245, 214, 135)
        renderer.paint.style = Paint.Style.STROKE
        renderer.paint.strokeWidth = 2.5f
        canvas.drawRoundRect(panel, 18f, 18f, renderer.paint)
    }

    private fun drawStoryTitle(canvas: Canvas, w: Int, h: Int, title: String, color: Int) {
        renderer.titlePaint.textAlign = Paint.Align.CENTER
        renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.titlePaint.textSize = (h * 0.085f).coerceIn(64f, 104f)
        renderer.titlePaint.color = color
        renderer.titlePaint.setShadowLayer(10f, 0f, 4f, Color.BLACK)
        canvas.drawText(title, w / 2f, h * 0.30f, renderer.titlePaint)
        renderer.titlePaint.clearShadowLayer()
    }

    private fun drawStoryText(canvas: Canvas, w: Int, h: Int, lines: List<String>, startY: Float) {
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT
        renderer.subtitlePaint.textSize = (h * 0.035f).coerceIn(30f, 46f)
        renderer.subtitlePaint.color = Color.parseColor("#E7DCC7")

        var y = startY
        val maxWidth = w * 0.58f
        val lineGap = renderer.subtitlePaint.textSize * 1.45f
        for (paragraph in lines) {
            val wrapped = wrapText(paragraph, maxWidth, renderer.subtitlePaint)
            for (line in wrapped) {
                canvas.drawText(line, w / 2f, y, renderer.subtitlePaint)
                y += lineGap
            }
            y += lineGap * 0.35f
        }
    }

    private fun drawStoryHint(canvas: Canvas, w: Int, h: Int, hint: String) {
        val alpha = ((sin(renderer.globalTime * 2f) + 1f) * 70f + 110f).toInt()
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
        renderer.subtitlePaint.textSize = (h * 0.032f).coerceIn(30f, 42f)
        renderer.subtitlePaint.color = Color.argb(alpha, 255, 229, 165)
        canvas.drawText(hint, w / 2f, h * 0.74f, renderer.subtitlePaint)
        renderer.subtitlePaint.typeface = Typeface.DEFAULT
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        for (char in text) {
            val next = current + char
            if (current.isNotEmpty() && paint.measureText(next) > maxWidth) {
                result.add(current)
                current = char.toString()
            } else {
                current = next
            }
        }
        if (current.isNotEmpty()) result.add(current)
        return result
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
                renderer.titlePaint.style = Paint.Style.FILL
                renderer.titlePaint.strokeWidth = 0f
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


