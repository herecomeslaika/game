# 联机UI优化 - 代码实现参考

## 核心改进点总览

### 1. 按钮发光效果实现

**关键代码**：
```kotlin
private fun drawButtonWithGlow(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float,
                               text: String, color: Int, rect: RectF) {
    // 第1层：发光背景（半透明）
    val glowPaint = Paint().apply {
        color = Color.argb(40, 
            (color shr 16) and 0xFF,    // R通道
            (color shr 8) and 0xFF,     // G通道
            color and 0xFF              // B通道
        )
        style = Paint.Style.FILL
    }
    val glowRadius = 20f
    canvas.drawRoundRect(cx - width / 2 - glowRadius, cy - height / 2 - glowRadius,
        cx + width / 2 + glowRadius, cy + height / 2 + glowRadius, 
        glowRadius, glowRadius, glowPaint)
    
    // 第2层：按钮主体
    renderer.paint.color = color
    renderer.paint.style = Paint.Style.FILL
    rect.set(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2)
    canvas.drawRoundRect(rect, 16f, 16f, renderer.paint)
    
    // 第3层：边框（白色半透明）
    renderer.paint.color = Color.argb(150, 255, 255, 255)
    renderer.paint.style = Paint.Style.STROKE
    renderer.paint.strokeWidth = 2f
    canvas.drawRoundRect(rect, 16f, 16f, renderer.paint)
    
    // 第4层：文字（加粗白色）
    renderer.subtitlePaint.textSize = 70f
    renderer.subtitlePaint.color = Color.WHITE
    renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    renderer.subtitlePaint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText(text, cx, cy + 25f, renderer.subtitlePaint)
}
```

**技术要点**：
- 使用argb()分离RGB通道，保持色相
- 四层叠加创建立体效果
- 透明度40%的发光不会过亮
- 圆角半径16f提供现代感

### 2. 加载动画实现

**关键代码**：
```kotlin
private fun drawLoadingAnimation(canvas: Canvas, cx: Float, cy: Float) {
    val radius = 30f
    val rotation = (renderer.globalTime * 360f) % 360f  // 每帧旋转
    
    val arcPaint = Paint().apply {
        color = Color.argb(200, 33, 150, 243)           // 蓝色
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND                      // 圆头线条
    }
    
    canvas.save()
    canvas.rotate(rotation, cx, cy)                      // 旋转变换
    canvas.drawArc(cx - radius, cy - radius, 
                   cx + radius, cy + radius, 
                   0f, 120f, false, arcPaint)             // 绘制120°弧
    canvas.restore()
    
    // 中心点
    renderer.paint.style = Paint.Style.FILL
    renderer.paint.color = Color.parseColor("#64B5F6")
    canvas.drawCircle(cx, cy, 8f, renderer.paint)
}
```

**技术要点**：
- `globalTime * 360f` 实现连续旋转
- `120f` 的弧度创建动态感
- `strokeCap = ROUND` 使端点光滑
- 中心点提供视觉重心

### 3. 玩家卡片设计

**关键代码**：
```kotlin
private fun drawPlayerItemCard(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
                               player: LobbyPlayer, isHost: Boolean) {
    // 背景 - 根据准备状态改变颜色
    val bgColor = if (player.isReady) 
        Color.argb(80, 76, 175, 80)   // 绿色：已准备
    else 
        Color.argb(60, 100, 100, 150) // 蓝色：未准备
    val bgPaint = Paint().apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(x, y, x + width, y + height, 10f, 10f, bgPaint)
    
    // 边框 - 动态颜色
    val borderColor = if (player.isReady) 
        Color.parseColor("#4CAF50")   // 绿边框
    else 
        Color.parseColor("#64B5F6")   // 蓝边框
    renderer.paint.color = borderColor
    renderer.paint.style = Paint.Style.STROKE
    renderer.paint.strokeWidth = 2f
    canvas.drawRoundRect(x, y, x + width, y + height, 10f, 10f, renderer.paint)
    
    // 房主标识 - 金色圆圈 + 👑emoji
    if (player.isHost) {
        val badgeX = x + 25f
        val badgeY = y + 25f
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
    
    // 准备状态 - 右侧大型展示
    val statusX = x + width - 120f
    val statusColor = if (player.isReady) 
        Color.parseColor("#4CAF50") // 绿色
    else 
        Color.parseColor("#FF5252")  // 红色
    renderer.subtitlePaint.textSize = 48f
    renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    renderer.subtitlePaint.color = statusColor
    val statusEmoji = if (player.isReady) "✓" else "○"
    canvas.drawText(statusEmoji, statusX, y + 55f, renderer.subtitlePaint)
    
    renderer.subtitlePaint.textSize = 32f
    renderer.subtitlePaint.color = statusColor
    val statusText = if (player.isReady) "已准备" else "未准备"
    canvas.drawText(statusText, statusX, y + 75f, renderer.subtitlePaint)
    
    // 踢人按钮（房主专用）
    if (isHost && !player.isHost) {
        val kickBtnX = x + width - 70f
        val kickBtnY = y + height / 2 - 35f
        val kickBtnSize = 70f
        val kickRect = RectF(kickBtnX, kickBtnY, 
                             kickBtnX + kickBtnSize, kickBtnY + kickBtnSize)
        kickPlayerBtnRects.add(kickRect)
        
        // 红色按钮
        renderer.paint.color = Color.argb(200, 255, 82, 82)
        renderer.paint.style = Paint.Style.FILL
        canvas.drawRoundRect(kickRect, 8f, 8f, renderer.paint)
        
        // 叉号符号
        renderer.subtitlePaint.textSize = 32f
        renderer.subtitlePaint.textAlign = Paint.Align.CENTER
        renderer.subtitlePaint.color = Color.WHITE
        canvas.drawText("✕", kickBtnX + kickBtnSize / 2, kickBtnY + 45f, renderer.subtitlePaint)
    }
}
```

**技术要点**：
- 条件判断创建动态配色
- 多层信息展示（名称、状态、按钮）
- emoji提升用户界面友好度
- 踢出按钮只在房主版本显示

### 4. 脉冲闪烁动画

**关键代码**：
```kotlin
// 在drawHostButtons中
if (allReady) {
    val pulse = (sin(renderer.globalTime * 3f) + 1f) * 0.5f  // 0.0 ~ 1.0
    val alpha = (120 + pulse * 135).toInt()                  // 120 ~ 255
    renderer.subtitlePaint.color = Color.argb(alpha, 76, 175, 80)
}
```

**数学原理**：
- `sin()` 范围：-1.0 ~ 1.0
- `(sin() + 1) * 0.5` 转换为 0.0 ~ 1.0
- `120 + 0.5 * 135 = 187.5`（中间值）
- `120 + 1.0 * 135 = 255`（最亮）

### 5. 房间码显示框

**关键代码**：
```kotlin
private fun drawRoomCodeDisplay(canvas: Canvas, w: Int, h: Int, roomCode: String) {
    val boxX = w * 0.1f
    val boxY = h * 0.15f
    val boxWidth = w * 0.35f
    val boxHeight = h * 0.12f
    
    // 背景框（半透明红）
    val bgPaint = Paint().apply {
        color = Color.argb(100, 153, 0, 0)  // 暗红
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 12f, 12f, bgPaint)
    
    // 边框（金色）
    renderer.paint.color = Color.parseColor("#FFD700")
    renderer.paint.style = Paint.Style.STROKE
    renderer.paint.strokeWidth = 2f
    canvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 12f, 12f, renderer.paint)
    
    // 标题"房间号"
    renderer.subtitlePaint.textSize = 32f
    renderer.subtitlePaint.color = Color.parseColor("#888888")
    renderer.subtitlePaint.textAlign = Paint.Align.CENTER
    canvas.drawText("房间号", boxX + boxWidth / 2, boxY + 28f, renderer.subtitlePaint)
    
    // 房间码（大号）
    renderer.titlePaint.textSize = 100f
    renderer.titlePaint.color = Color.parseColor("#FFD700")
    renderer.titlePaint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText(roomCode, boxX + boxWidth / 2, boxY + 88f, renderer.titlePaint)
}
```

### 6. 网格背景

**关键代码**：
```kotlin
private fun drawGridBackground(canvas: Canvas, w: Int, h: Int) {
    renderer.paint.color = Color.argb(15, 100, 100, 150)  // 非常淡的蓝
    renderer.paint.strokeWidth = 1f
    val gridSize = 80f
    
    // 竖线
    var x = 0f
    while (x < w) {
        canvas.drawLine(x, 0f, x, h.toFloat(), renderer.paint)
        x += gridSize
    }
    
    // 横线
    var y = 0f
    while (y < h) {
        canvas.drawLine(0f, y, w.toFloat(), y, renderer.paint)
        y += gridSize
    }
}
```

**视觉效果**：
- `Color.argb(15, ...)` 创建极淡网格（不会喧宾夺主）
- 80f网格大小适合1920x1080分辨率
- 提供深度感而不影响可读性

---

## 颜色配置表

### 联机大厅配色
```kotlin
背景：#0A0F19（深蓝黑）
标题：#FFD700（金色）
装饰线：#FF6B35（橙红色）
按钮1：#4CAF50（绿色）
按钮2：#2196F3（蓝色）
按钮3：#757575（灰色）
```

### 房间列表配色
```kotlin
背景：#0A0F19（深蓝黑）
标题：#64B5F6（浅蓝）
边框：#2196F3（蓝色）
装饰线：#2196F3（蓝色）
```

### 房间等待配色
```kotlin
背景：#0A0F19（深蓝黑）
标题：#FFD700（金色）
装饰线：#FF6B35（橙红色）
房间码框：Red + Gold
玩家卡片已准备：Green
玩家卡片未准备：Blue
踢出按钮：#FF5252（红色）
```

### 游戏结束配色
```kotlin
失败背景：#280A0A（暗红）
失败标题：#FF4444（红色）
通关背景：#0A280A（暗绿）
通关标题：#FFD700（金色）
```

---

## 常用Paint预设

```kotlin
// 标题文本（大、粗、金色）
renderer.titlePaint.apply {
    color = Color.parseColor("#FFD700")
    textSize = 120f
    typeface = Typeface.DEFAULT_BOLD
    textAlign = Paint.Align.CENTER
}

// 副标题文本（中等、正常、浅色）
renderer.subtitlePaint.apply {
    color = Color.parseColor("#CCAA88")
    textSize = 54f
    typeface = Typeface.DEFAULT
    textAlign = Paint.Align.CENTER
}

// 发光文本（带阴影）
paint.setShadowLayer(8f, 0f, 4f, Color.BLACK)
```

---

## 动画时间节奏

| 动画 | 速率 | 周期 | 用途 |
|-----|------|------|------|
| 脉冲闪烁 | 3f | ~2秒 | 开始游戏按钮 |
| 加载旋转 | 360f | ~1秒 | 房间搜索 |
| 背景脉冲 | 1.5f-2f | ~3-4秒 | 游戏结束 |
| 副手动画 | 4f | ~1.5秒 | Boss出场 |

**计算公式**：
```kotlin
周期 = 2π / 速率
例：2π / 3 ≈ 2.09秒
```

---

## 坐标系统

所有UI使用**屏幕相对坐标**：
```kotlin
w = 屏幕宽度（1920）
h = 屏幕高度（1080）

相对位置示例：
x_center = w / 2f           // 屏幕中心
y_top = h * 0.1f            // 距顶部10%处
```

---

## 性能优化建议

### 1. Paint对象缓存
```kotlin
// ❌ 不好 - 每帧创建新对象
fun drawButton(...) {
    val paint = Paint()
    paint.color = ...
}

// ✅ 好 - 复用Paint对象
private val buttonPaint = Paint()
```

### 2. String避免频繁创建
```kotlin
// ❌ 不好
canvas.drawText("准备: ${player.isReady}", ...)

// ✅ 好
val text = if (player.isReady) "✓ 已准备" else "○ 未准备"
canvas.drawText(text, ...)
```

### 3. 条件判断前置
```kotlin
// ❌ 不好 - 每帧计算
val pulse = (sin(renderer.globalTime * 3f) + 1f) * 0.5f
if (pulse > 0.5f) { ... }

// ✅ 好 - 计算一次
val shouldPulse = (sin(renderer.globalTime * 3f) + 1f) > 0.5f
if (shouldPulse) { ... }
```

---

## 文档版本
- **版本**: 1.0
- **日期**: 2026-06-03
- **作者**: 优化完成
- **状态**: 完成✓

---

