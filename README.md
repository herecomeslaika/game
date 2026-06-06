# 冥途 — Android 等距视角 Roguelike

受 Hades 启发的等距视角动作 Roguelike，运行于 Android 平台。Canvas 2D 渲染，Kotlin 原生开发，无游戏引擎依赖。

## 游戏玩法

- **等距视角实时战斗** — 三段连击（暴击/范围伤害可强化）+ 冲刺闪避 + 特殊技能（投掷飞刀），支持斗志/冻结/闪电等效果
- **七神祝福体系** — 宙斯/阿佛洛狄忒/雅典娜/阿瑞斯/赫尔墨斯/波塞冬/哈迪斯，各 4 个祝福 + 7 个 DUO 组合祝福（最多28+7种增强方式）
- **多楼层关卡** — 程序化生成，9-12 个房间/楼层，普通房→精英房→Boss 房，三层主题（冥界/炼狱/极乐），地图外围有对应主题场景装饰
- **6 种地形** — 地板、墙壁、障碍物、岩浆、水域、尖刺，每种有特殊交互
- **9 种敌人与 3 个主题 Boss** — 骷髅、幽灵、持盾兵、掷矛手、火焰舞者、熔岩法师等敌人；冥骨巨灵、炼狱泰坦、永恒冠军拥有主题技能、预警、入场动画和终结特效
- **剧情与音频** — 开场/通关/失败剧情可跳过，主菜单/战斗/Boss 背景音乐分离，命中、受伤、暴击、拾取、祝福等音效独立控制
- **📡 联机模式** — 支持本地网络最多 4 人房间大厅，实时显示人数与准备状态，房主默认准备，至少 1 名加入玩家准备后即可开始

## 项目结构

```
app/src/main/kotlin/com/game/roguelike/
├── core/                    # 核心游戏逻辑（游戏循环、状态管理）
│   ├── Game.kt              # 主循环(60fps fixed timestep)、状态机、碰撞、伤害计算
│   ├── GameState.kt         # 15个游戏状态枚举
│   ├── GameSurfaceView.kt   # Android SurfaceView 入口、触摸事件路由
│   ├── InputManager.kt      # 虚拟摇杆输入处理
│   ├── PlayerState.kt       # 玩家状态枚举
│   ├── EnemyState.kt        # 敌人状态枚举
│   ├── BlessingRarity.kt    # 祝福稀有度（普通/稀有/史诗/DUO）
│   ├── GodType.kt           # 七神枚举
│   └── RoomType.kt          # 房间类型（普通/精英/Boss等）
├── entity/                  # 游戏实体系统
│   ├── Entity.kt            # 实体基类（position、velocity等）
│   ├── Player.kt            # 玩家：三段连击、冲刺、特殊技能、祝福效果
│   ├── Enemy.kt             # 敌人：AI、护盾、Boss多阶段转换、死亡动画
│   ├── EnemyType.kt         # 敌人9种枚举（及3种Boss变体）
│   ├── EnemyConfig.kt       # 敌人属性配置表
│   ├── EnemyBehavior.kt     # 敌人AI逻辑（追踪、攻击、特殊行为）
│   ├── Particle.kt          # 粒子效果系统
│   ├── GhostSummon.kt       # 幽灵召唤物（祝福功能）
│   ├── Merchant.kt          # 商人NPC
│   └── BlessingEffects.kt   # 祝福效果代理类
├── blessing/                # 祝福系统（28+7种祝福）
│   ├── Blessing.kt          # 祝福定义（id、name、onApply callback）
│   └── BlessingSelector.kt  # 祝福选择界面逻辑
├── level/                   # 关卡生成系统
│   ├── DungeonGenerator.kt  # 程序化地牢生成（9-12房间/楼层）
│   ├── Room.kt              # 房间类：瓦片网格、敌人生成、门位置
│   ├── Layer.kt             # 楼层数据（3个主题楼层）
│   ├── Door.kt              # 房间门逻辑
│   └── FloorMap.kt          # 楼层地图（房间连接图）
├── rendering/               # 渲染系统（Canvas 2D）
│   ├── IsometricRenderer.kt # 核心：等距坐标变换、摄像机、light系统
│   ├── ScreenRenderer.kt    # UI渲染（菜单、剧情、联机界面、HUD、Boss血条）
│   ├── TileRenderer.kt      # 瓦片绘制（6种地形深度排序）
│   ├── EnemyRenderer.kt     # 敌人渲染（受伤闪白、死亡溶解、Boss特效）
│   ├── BossEntranceCinematicRenderer.kt # Boss出场动画
│   ├── EnvironmentRenderer.kt # 地图外围主题场景
│   ├── EnvironmentTheme.kt  # 楼层外围主题配置
│   ├── PlayerRenderer.kt    # 玩家渲染
│   └── EntityRenderer.kt    # 通用实体渲染
├── network/                 # 网络系统（本地UDP多人）
│   ├── RoomManager.kt       # 房间管理（房主/客户端模式）
│   ├── LobbyRules.kt        # 联机大厅规则（准备状态、开始条件、人数同步）
│   ├── UdpServer.kt         # UDP服务器（房主广播、接收join/ready）
│   ├── UdpClient.kt         # UDP客户端（发现/连接房间）
│   └── NetworkData.kt       # 网络数据类（PlayerInputFrame、RoomCommand等）
├── combat/                  # 战斗系统
│   └── Projectile.kt        # 投射物（飞刀）
├── shop/                    # 商店系统
│   └── Shop.kt              # 商店逻辑
├── audio/                   # 音频系统
│   └── AudioManager.kt      # 背景音乐、音效管理
├── event/                   # 事件系统
│   ├── GameEvent.kt         # 事件定义
│   ├── EventPool.kt         # 事件对象池
│   └── ...
├── ui/                      # UI组件
│   ├── HUD.kt               # 游戏内HUD（生命值、金币、祝福计数）
│   ├── VirtualJoystick.kt   # 虚拟摇杆控件
│   ├── ActionButtons.kt     # 攻击/技能/冲刺按钮
│   ├── BlessingSelectUI.kt  # 祝福选择界面
│   ├── ShopUI.kt            # 商店界面
│   └── ...
└── util/                    # 工具类
    ├── Vector2.kt           # 2D向量（运算、序列化）
    ├── StateMachine.kt      # 通用有限状态机（转换回调）
    ├── Animation.kt         # 动画播放器
    ├── Rect.kt              # 矩形工具
    └── ...

app/src/test/java/com/game/roguelike/
└── 单元测试（Vector2、StateMachine、Blessing、Enemy、Player等）
```

## 渲染管线

### 核心循环 (60fps Fixed Timestep)
```
GameSurfaceView
  ↓ 驱动
Game.gameLoop()
  ├─ update(TICK: 1/60秒) × N   [固定时间步 — 物理稳定]
  └─ render()                    [渲染当前帧]
```

### 渲染步骤
1. **清屏** — 背景色 (#0A0F19 深灰色)
2. **瓦片层** — `TileRenderer` 按 (row,col) 深度排序绘制 6 种地形等距菱形
3. **实体层** — 敌人、玩家按 (x+y) 排序绘制，支持受伤闪白、死亡溶解效果
4. **粒子层** — 伤害数字、特效粒子
5. **UI 层** — `ScreenRenderer` 叠加菜单、HUD、Boss血条、联机界面

### 等距坐标变换
```
World (wx, wy) ────→ Screen (sx, sy)
sx = (wx - wy) × tileWidth/2  + cameraX + w/2
sy = (wx + wy) × tileHeight/2 + cameraY + h/2
```

### 深度排序
- 按 `depth = x + y` 排序，确保等距视角下遮挡关系正确
- 摄像机跟随玩家（中心偏移）

## 游戏状态流转（15 个状态）

```
┌─ MENU (主菜单)
│   ├─ MULTIPLAYER_LOBBY (联机大厅)
│   │   ├─ ROOM_LIST (搜索房间)
│   │   │   └─ ROOM_WAITING (加入房间等待)
│   │   └─ ROOM_WAITING (创建房间等待)
│   │
│   ├─ PLAYING (游戏中) ⭐️ 核心状态
│   │   ├─ [清空敌人] → BLESSING_SELECT (选择祝福)
│   │   │            → LAYER_TRANSITION (过场动画)
│   │   │            → PLAYING (下一关)
│   │   │
│   │   ├─ [遇到 Boss] → BOSS_ENTRANCE (Boss 入场)
│   │   │               → PLAYING (Boss 战斗)
│   │   │
│   │   ├─ [进入商店] → SHOP (商店选择祝福)
│   │   │              → PLAYING
│   │   │
│   │   ├─ [事件房间] → EVENT (事件选择)
│   │   │              → PLAYING
│   │   │
│   │   └─ [玩家死亡] → PLAYER_DEATH (死亡动画)
│   │                   → GAME_OVER (游戏结束)
│   │
│   ├─ OPTIONS (设置)
│   └─ EXIT_CONFIRM (退出确认)
│
└─ VICTORY (胜利) / GAME_OVER (失败)
```

### 当前状态补充

- 新游戏会先进入 `INTRO_STORY`，点击任意位置跳过后开始正式关卡。
- Boss 房先进入 `BOSS_ENTRANCE`，播放主题出场动画和标题展示后生成 Boss；进入 Boss 房时不会触发祝福选择，只有击败 Boss 后才弹出祝福选择。
- 玩家死亡后进入 `PLAYER_DEATH` 死亡动画，随后播放 `FAILURE_STORY`，不再额外显示旧的“游戏结束”界面。
- 最终通关后进入 `ENDING_STORY`，剧情可跳过，结束后直接返回主菜单。

## 联机模式系统

### 网络架构
```
房主 (UdpServer, 端口12345-12346)
  ↕️ UDP 通信 (30Hz 同步)
客户端 (UdpClient, 搜索或输入房间码)
```

### 房间流程
1. **房主创建房间** → 生成 4 位房间码 → 房主进入玩家列表并默认“已准备” → 启动 UDP 服务器 → 广播房间到局域网
2. **客户端加入** → 搜索房间列表或输入房间码 → 连接房主 UDP 服务 → 房主分配玩家 ID 并广播完整玩家列表
3. **等待房间** → 实时显示房间号、玩家数量、每名玩家的房主/准备状态；客户端点击“准备游戏”后立即本地更新并同步给房主
4. **开始条件** → 房主默认准备；只要至少 1 名非房主玩家加入并准备，房主即可点击“开始游戏”
5. **实时同步** → 玩家位置、输入、敌人状态每帧同步 → 最多支持 4 人

### 等待房间 UI

- **联机大厅**：创建房间、加入房间、返回主菜单三个明确按钮，带半透明底纹和主题色描边。
- **房间列表**：显示房间名、4 位房间号、当前人数/最大人数、房主 IP。
- **等待房间**：顶部显示房间号和人数，中部显示玩家列表，底部显示准备/开始/离开按钮。
- **状态提示**：房主看到“等待玩家加入 / 等待玩家准备 / 已有玩家准备，房主可以开始”；客户端看到“点击准备后即可开始 / 已准备，等待房主开始”。
- **踢人入口**：房主可在玩家列表中踢出非房主玩家，房间人数和列表会同步刷新。

## 近期优化内容

### Boss 战斗

- **冥骨巨灵**：增加骨墙封锁、亡骸爆裂、王座震地三连等控场技能。
- **炼狱泰坦**：增加陨星标记雨、熔岩十字喷发、烈焰冲锋残迹等区域压迫技能。
- **永恒冠军**：增加盾反窗口、三连投矛扇形、英灵突刺等技巧型决斗技能。
- **通用表现**：Boss 体型为原始 1.5 倍，具备醒目血条、阶段文字提示、技能预警圈/线、释放前短暂停顿和主题粒子。
- **入场动画**：第一层从坟墓中爬出，第二层由岩浆球凝成 Boss，第三层从天而降并触发震屏；入场后展示 Boss 标题。
- **终结特效**：击败 Boss 后播放对应主题的终结视觉效果。

### 场景与界面

- 地图外围不再是纯黑背景，三层分别扩展为冥界墓地、岩浆炼狱、极乐城堡主题。
- 第一层外围包含坟墓、绿色幽灵、白骨；第二层包含流动岩浆池和岩浆怪物；第三层包含旗帜、城堡等装饰。
- 祝福选择框改为更紧凑的竖向布局，触摸区域与视觉尺寸同步。
- 攻击、技能、冲刺按钮使用剑、飞刀、冲刺箭头等图形化图标。

### 剧情与音频

- 新增骑士拯救公主故事线：开场剧情、通关剧情、死亡失败剧情均可跳过。
- 主菜单使用 `bgm_main.wav`，战斗和 Boss 分别使用 `bgm_battle` / `bgm_boss`。
- 音效系统支持 SFX/BGM 独立音量、命中、敌人死亡、Boss 阶段、拾取、受伤、暴击、脚步、祝福、升级等音效。
- 已去除 UI 点击音效和技能冷却完成提示音，避免频繁操作时过吵。


## 构建与运行

### 环境要求

- Android SDK 34+
- JDK 17+（推荐 JetBrains Runtime 或 Android Studio 内置 JBR）
- Android 设备（API 26+）或模拟器

### 设置 JAVA_HOME

```bash
# Windows CMD
set JAVA_HOME=C:\Users\杰拉德\.jdks\jbr-17.0.14

# Git Bash / Linux / macOS
export JAVA_HOME="/c/Users/杰拉德/.jdks/jbr-17.0.14"

# 也可使用 Android Studio 内置 JDK
# set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
```

### 构建与安装

```bash
# 构建 Debug APK
./gradlew.bat assembleDebug          # Windows
./gradlew assembleDebug              # Linux / macOS

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk

# 安装到设备（需连接 Android 设备或启动模拟器）
adb install app/build/outputs/apk/debug/app-debug.apk

# 运行单元测试
./gradlew.bat :app:testDebugUnitTest # Windows
./gradlew :app:testDebugUnitTest     # Linux / macOS
```

### 常见问题

| 问题 | 解决方案 |
|------|----------|
| `Unsupported class file major version 65` | JAVA_HOME 未指向 JDK 17+，参考上方设置 |
| `SDK location not found` | 项目根目录创建 `local.properties`，写入 `sdk.dir=C:\\Users\\<用户>\\AppData\\Local\\Android\\Sdk` |
| Boss 关闪退 | 已修复（vibrate 权限 SecurityException 已捕获） |

## 技术特点

### 架构特色
- **纯 Canvas 2D 渲染** — 无 Unity/Godot，完全自定义渲染管线
- **固定时间步** — 1/60 秒，确保物理稳定和网络同步精确
- **状态机驱动** — 15 个游戏状态 + 玩家/敌人状态机
- **程序化生成** — 每个楼层 9-12 个房间动态连接
- **本地网络多人** — UDP 30Hz 同步，支持 4 人实时对战

### 性能优化
- Path 对象池（减少 GC 压力）
- 梯度缓存（低端设备可禁用 shader）
- 粒子对象池
- 深度排序确保渲染正确性

### 技术栈
- **语言:** Kotlin (100% 原生)
- **UI:** Android Canvas 2D + SurfaceView
- **网络:** UDP 局域网通信
- **测试:** JUnit 5, Mockito
- **构建:** Gradle (Kotlin DSL)
- **最低支持:** Android API 26 (Oreo)
