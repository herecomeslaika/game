# 冥途 — Android 等距视角 Roguelike

受 Hades 启发的等距视角动作 Roguelike，运行于 Android 平台。Canvas 2D 渲染，Kotlin 原生开发，无游戏引擎依赖。

## 游戏玩法

- **等距视角实时战斗** — 三段连击 + 闪避 + 特殊技能
- **七神祝福体系** — 宙斯/阿佛洛狄忒/雅典娜/阿瑞斯/赫尔墨斯/波塞冬/哈迪斯，各 4 个祝福 + 7 个 DUO 组合祝福
- **多楼层关卡** — 普通房→精英房→Boss 房，三层主题（冥界/炼狱/极乐）
- **6 种地形** — 地板、墙壁、障碍物、岩浆、水域、尖刺
- **8 种敌人** — 骷髅、幽灵、持盾兵、掷矛手、火焰舞者、熔岩法师 + 3 种 Boss

## 项目结构

```
app/src/main/kotlin/com/game/roguelike/
├── core/                    # 核心游戏逻辑
│   ├── Game.kt              # 主循环、状态机、战斗、碰撞、输入处理
│   ├── GameState.kt         # 游戏状态枚举（菜单/战斗/Boss入场/祝福选择/暂停/死亡/胜利）
│   ├── GameSurfaceView.kt   # Android SurfaceView 入口
│   ├── BlessingRarity.kt    # 祝福稀有度（普通/稀有/史诗/DUO）
│   └── GodType.kt           # 七神枚举及扩展属性（颜色、名称）
├── entity/                  # 实体
│   ├── Player.kt            # 玩家：属性、状态机、祝福效果、重置
│   ├── Enemy.kt             # 敌人：AI、护盾、阶段转换、死亡动画
│   ├── EnemyType.kt         # 敌人类型枚举（属性、Boss 名称/称号）
│   └── Particle.kt          # 粒子效果
├── blessing/                # 祝福系统
│   └── Blessing.kt          # 28 祝福 + 7 DUO 祝福定义及查询方法
├── level/                   # 关卡生成
│   └── Room.kt              # 房间生成：布局变体、地形放置、敌人/Boss 生成
├── rendering/               # 渲染
│   ├── IsometricRenderer.kt # 等距渲染核心：坐标变换、摄像机、主题色
│   ├── TileRenderer.kt      # 瓦片绘制：地板/墙壁/岩浆/水域/尖刺/宝箱/柱子
│   ├── EnemyRenderer.kt     # 敌人绘制：类型外观、Boss、受伤闪白、死亡溶解
│   └── ScreenRenderer.kt    # UI 层：HUD、祝福面板、Boss 血条、伤害数字
└── util/                    # 工具
    ├── Vector2.kt           # 2D 向量运算
    └── StateMachine.kt      # 通用状态机（转换回调、状态计时）

app/src/test/java/com/game/roguelike/
├── util/
│   ├── Vector2Test.kt       # 向量运算测试
│   └── StateMachineTest.kt  # 状态机测试
├── blessing/
│   └── BlessingTest.kt      # 祝福系统测试
└── entity/
    ├── EnemyDamageTest.kt   # 敌人属性/状态测试
    └── PlayerDamageTest.kt  # 玩家属性/重置测试
```

## 渲染管线

1. `GameSurfaceView` 驱动游戏循环（~60fps）
2. `Game.gameLoop()` → `update()` + `render()`
3. `IsometricRenderer` 管理等距坐标变换和摄像机跟随
4. `TileRenderer` 按行列遍历瓦片，逐类型绘制等距菱形
5. `EnemyRenderer` 深度排序后绘制敌人（含受伤闪白、死亡溶解动画）
6. `ScreenRenderer` 叠加 HUD 层

## 游戏状态流转

```
MENU → PLAYING → (通关/全灭) → VICTORY / DEAD
                → BOSS_ENTRANCE → PLAYING(Boss战)
                → BLESSING_SELECT → PLAYING
                → PAUSE
```

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
./gradlew test
```

### 常见问题

| 问题 | 解决方案 |
|------|----------|
| `Unsupported class file major version 65` | JAVA_HOME 未指向 JDK 17+，参考上方设置 |
| `SDK location not found` | 项目根目录创建 `local.properties`，写入 `sdk.dir=C:\\Users\\<用户>\\AppData\\Local\\Android\\Sdk` |
| Boss 关闪退 | 已修复（vibrate 权限 SecurityException 已捕获） |

## 技术栈

- Kotlin, Android Canvas 2D, SurfaceView
- JUnit 5, Mockito（单元测试）
- 无第三方游戏引擎或渲染库
