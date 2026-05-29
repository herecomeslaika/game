package com.game.roguelike.level

import com.game.roguelike.core.Game
import com.game.roguelike.core.RoomType
import com.game.roguelike.entity.*
import com.game.roguelike.util.Vector2
import kotlin.random.Random

class Room(
    val width: Int = 28,
    val height: Int = 20,
    val type: RoomType,
    val layerIndex: Int = 0,
    var roomId: Int = -1
) {
    companion object {
        const val TILE_FLOOR = 0
        const val TILE_WALL = 1
        const val TILE_OBSTACLE = 2
        const val TILE_DOOR = 3
        const val TILE_PILLAR = 4
        const val TILE_CHEST = 5
        const val TILE_LAVA = 6
        const val TILE_WATER = 7
        const val TILE_SPIKE = 8
    }

    var tiles = IntArray(width * height)
    var cleared = false
    val doors = mutableListOf<Door>()
    var spawnPoint = Vector2(width * 32f / 2f, height * 16f)
    var merchantPosition = Vector2(width * 32f / 2f, height * 16f / 2f)

    init {
        generateLayout()
    }

    private fun generateLayout() {
        // Fill with floor
        for (i in tiles.indices) {
            tiles[i] = TILE_FLOOR
        }

        // Walls around edges
        for (x in 0 until width) {
            setTile(x, 0, TILE_WALL)
            setTile(x, height - 1, TILE_WALL)
        }
        for (y in 0 until height) {
            setTile(0, y, TILE_WALL)
            setTile(width - 1, y, TILE_WALL)
        }

        // Doors
        when (type) {
            RoomType.ENTRY -> {
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(
                    position = Vector2((width - 1) * 64f, (height / 2) * 32f),
                    isLocked = false
                ))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateEntryLayout()
            }
            RoomType.COMBAT -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(
                    position = Vector2(0f, (height / 2) * 32f),
                    isLocked = true
                ))
                doors.add(Door(
                    position = Vector2((width - 1) * 64f, (height / 2) * 32f),
                    isLocked = true
                ))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateCombatLayout()
            }
            RoomType.REWARD -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateRewardLayout()
            }
            RoomType.SHOP -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                merchantPosition = Vector2(width * 32f, height * 16f / 2f)
                generateShopLayout()
            }
            RoomType.BOSS -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = true))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = true))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateBossLayout()
            }
            RoomType.ELITE -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = true))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = true))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateCombatLayout()
            }
            RoomType.TREASURE -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateRewardLayout()
            }
            RoomType.EVENT -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateEntryLayout()
            }
            RoomType.REST -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateEntryLayout()
            }
            RoomType.HIDDEN -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                generateRewardLayout()
            }
        }
    }

    private fun generateEntryLayout() {
        // Decorative pillars near entrance
        val cx = width / 2
        val cy = height / 2
        setTile(cx - 3, cy - 2, TILE_PILLAR)
        setTile(cx + 3, cy - 2, TILE_PILLAR)
        setTile(cx - 3, cy + 2, TILE_PILLAR)
        setTile(cx + 3, cy + 2, TILE_PILLAR)

        // Layer-specific floor decoration
        addLayerTerrain(2, 2, width - 3, height - 3)
    }

    private fun generateCombatLayout() {
        val variant = Random.nextInt(8)
        val cx = width / 2
        val cy = height / 2

        when (variant) {
            0 -> {
                // Center cross obstacle group
                setTile(cx, cy - 1, TILE_OBSTACLE)
                setTile(cx, cy + 1, TILE_OBSTACLE)
                setTile(cx - 1, cy, TILE_OBSTACLE)
                setTile(cx + 1, cy, TILE_OBSTACLE)
                setTile(cx - 2, cy - 2, TILE_PILLAR)
                setTile(cx + 2, cy - 2, TILE_PILLAR)
                setTile(cx - 2, cy + 2, TILE_PILLAR)
                setTile(cx + 2, cy + 2, TILE_PILLAR)
            }
            1 -> {
                // Symmetric two columns
                for (i in -1..1) {
                    setTile(cx - 3, cy + i, TILE_OBSTACLE)
                    setTile(cx + 3, cy + i, TILE_OBSTACLE)
                }
                setTile(cx - 3, cy - 2, TILE_PILLAR)
                setTile(cx + 3, cy - 2, TILE_PILLAR)
                setTile(cx - 3, cy + 2, TILE_PILLAR)
                setTile(cx + 3, cy + 2, TILE_PILLAR)
            }
            2 -> {
                // Ring of obstacles around center
                for (dx in -2..2) {
                    for (dy in -2..2) {
                        if (abs(dx) + abs(dy) == 2) {
                            setTile(cx + dx, cy + dy, TILE_OBSTACLE)
                        }
                    }
                }
            }
            3 -> {
                // Corridor style - two rows forming a channel
                for (x in cx - 3..cx + 3) {
                    setTile(x, cy - 2, TILE_OBSTACLE)
                    setTile(x, cy + 2, TILE_OBSTACLE)
                }
                // Pillars at corridor ends
                setTile(cx - 4, cy - 1, TILE_PILLAR)
                setTile(cx - 4, cy + 1, TILE_PILLAR)
                setTile(cx + 4, cy - 1, TILE_PILLAR)
                setTile(cx + 4, cy + 1, TILE_PILLAR)
            }
            4 -> {
                // Four corner fortresses
                for (dx in 0..2) for (dy in 0..2) {
                    setTile(2 + dx, 2 + dy, TILE_OBSTACLE)
                    setTile(width - 3 - dx, 2 + dy, TILE_OBSTACLE)
                    setTile(2 + dx, height - 3 - dy, TILE_OBSTACLE)
                    setTile(width - 3 - dx, height - 3 - dy, TILE_OBSTACLE)
                }
            }
            5 -> {
                // Central block + corner pillars
                for (dx in -1..1) for (dy in -1..1) {
                    setTile(cx + dx, cy + dy, TILE_OBSTACLE)
                }
                setTile(3, 3, TILE_PILLAR)
                setTile(width - 4, 3, TILE_PILLAR)
                setTile(3, height - 4, TILE_PILLAR)
                setTile(width - 4, height - 4, TILE_PILLAR)
            }
            6 -> {
                // Maze walls with gaps
                for (y in 4..height - 5 step 4) {
                    for (x in 3..width - 4) {
                        if (x != cx - 1 && x != cx + 1) setTile(x, y, TILE_OBSTACLE)
                    }
                }
            }
            7 -> {
                // Arena: obstacle ring around edges with 4 entrances
                for (x in 4..width - 5) {
                    setTile(x, 3, TILE_OBSTACLE)
                    setTile(x, height - 4, TILE_OBSTACLE)
                }
                for (y in 4..height - 5) {
                    setTile(3, y, TILE_OBSTACLE)
                    setTile(width - 4, y, TILE_OBSTACLE)
                }
                // Entrance gaps
                for (dy in -1..1) {
                    setTile(3, cy + dy, TILE_FLOOR)
                    setTile(width - 4, cy + dy, TILE_FLOOR)
                }
                for (dx in -1..1) {
                    setTile(cx + dx, 3, TILE_FLOOR)
                    setTile(cx + dx, height - 4, TILE_FLOOR)
                }
            }
        }

        // Add spikes in combat rooms
        if (Random.nextFloat() < 0.8f) {
            val spikeCount = Random.nextInt(4, 9)
            for (i in 0 until spikeCount) {
                val sx = Random.nextInt(3, width - 3)
                val sy = Random.nextInt(3, height - 3)
                if (getTile(sx, sy) == TILE_FLOOR) {
                    setTile(sx, sy, TILE_SPIKE)
                }
            }
        }

        // Layer-specific terrain
        addLayerTerrain(2, 2, width - 3, height - 3)

        // Corner obstacles
        val cornerObstacles = 2
        for (i in 0 until cornerObstacles) {
            setTile(1 + i, 1, TILE_OBSTACLE)
            setTile(width - 2 - i, 1, TILE_OBSTACLE)
            setTile(1 + i, height - 2, TILE_OBSTACLE)
            setTile(width - 2 - i, height - 2, TILE_OBSTACLE)
        }
    }

    private fun generateRewardLayout() {
        val cx = width / 2
        val cy = height / 2

        // Central chest decoration
        setTile(cx, cy, TILE_CHEST)

        // Decorative pillars around chest
        setTile(cx - 2, cy - 2, TILE_PILLAR)
        setTile(cx + 2, cy - 2, TILE_PILLAR)
        setTile(cx - 2, cy + 2, TILE_PILLAR)
        setTile(cx + 2, cy + 2, TILE_PILLAR)

        addLayerTerrain(2, 2, width - 3, height - 3)
    }

    private fun generateShopLayout() {
        val cy = height / 2

        // Shelf-like obstacles along back wall
        for (x in 3..(width - 4) step 2) {
            setTile(x, 2, TILE_OBSTACLE)
        }

        // Pillars flanking merchant
        setTile(width / 2 - 2, cy - 1, TILE_PILLAR)
        setTile(width / 2 + 2, cy - 1, TILE_PILLAR)

        addLayerTerrain(2, 2, width - 3, height - 3)
    }

    private fun generateBossLayout() {
        val cx = width / 2
        val cy = height / 2

        // Four corner pillars
        setTile(3, 3, TILE_PILLAR)
        setTile(width - 4, 3, TILE_PILLAR)
        setTile(3, height - 4, TILE_PILLAR)
        setTile(width - 4, height - 4, TILE_PILLAR)

        // Layer-specific terrain along edges
        addLayerTerrain(2, 2, width - 3, height - 3, edgeOnly = true)
    }

    /** Add layer-specific terrain (lava for layer 1, water for layer 2) */
    private fun addLayerTerrain(minX: Int, minY: Int, maxX: Int, maxY: Int, edgeOnly: Boolean = false) {
        when (layerIndex) {
            1 -> {
                // Asphodel: lava pools
                val lavaCount = if (edgeOnly) Random.nextInt(3, 6) else Random.nextInt(2, 4)
                for (i in 0 until lavaCount) {
                    val lx = if (edgeOnly) {
                        if (Random.nextBoolean()) Random.nextInt(minX, minX + 3) else Random.nextInt(maxX - 2, maxX + 1)
                    } else {
                        Random.nextInt(minX + 1, maxX - 1)
                    }
                    val ly = if (edgeOnly) {
                        if (Random.nextBoolean()) Random.nextInt(minY, minY + 3) else Random.nextInt(maxY - 2, maxY + 1)
                    } else {
                        Random.nextInt(minY + 1, maxY - 1)
                    }
                    if (getTile(lx, ly) == TILE_FLOOR) {
                        setTile(lx, ly, TILE_LAVA)
                        // Lava pools are 2-3 tiles
                        if (lx + 1 <= maxX && getTile(lx + 1, ly) == TILE_FLOOR && Random.nextBoolean()) {
                            setTile(lx + 1, ly, TILE_LAVA)
                        }
                        if (ly + 1 <= maxY && getTile(lx, ly + 1) == TILE_FLOOR && Random.nextBoolean()) {
                            setTile(lx, ly + 1, TILE_LAVA)
                        }
                    }
                }
            }
            2 -> {
                // Elysium: water pools
                val waterCount = if (edgeOnly) Random.nextInt(3, 6) else Random.nextInt(2, 4)
                for (i in 0 until waterCount) {
                    val wx = if (edgeOnly) {
                        if (Random.nextBoolean()) Random.nextInt(minX, minX + 3) else Random.nextInt(maxX - 2, maxX + 1)
                    } else {
                        Random.nextInt(minX + 1, maxX - 1)
                    }
                    val wy = if (edgeOnly) {
                        if (Random.nextBoolean()) Random.nextInt(minY, minY + 3) else Random.nextInt(maxY - 2, maxY + 1)
                    } else {
                        Random.nextInt(minY + 1, maxY - 1)
                    }
                    if (getTile(wx, wy) == TILE_FLOOR) {
                        setTile(wx, wy, TILE_WATER)
                        if (wx + 1 <= maxX && getTile(wx + 1, wy) == TILE_FLOOR && Random.nextBoolean()) {
                            setTile(wx + 1, wy, TILE_WATER)
                        }
                        if (wy + 1 <= maxY && getTile(wx, wy + 1) == TILE_FLOOR && Random.nextBoolean()) {
                            setTile(wx, wy + 1, TILE_WATER)
                        }
                    }
                }
            }
        }
    }

    private fun abs(v: Int) = if (v < 0) -v else v

    fun getTile(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return TILE_WALL
        return tiles[y * width + x]
    }

    private fun setTile(x: Int, y: Int, tile: Int) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            tiles[y * width + x] = tile
        }
    }

    fun spawnEnemies(game: Game) {
        val count = when (layerIndex) {
            0 -> Random.nextInt(5, 8)
            1 -> Random.nextInt(6, 10)
            2 -> Random.nextInt(8, 12)
            else -> 6
        }

        for (i in 0 until count) {
            val enemyType = when (layerIndex) {
                0 -> if (Random.nextFloat() < 0.4f) EnemyType.WRAITH else EnemyType.SKELETON
                1 -> if (Random.nextFloat() < 0.4f) EnemyType.LAVA_CASTER else EnemyType.FLAME_DANCER
                2 -> if (Random.nextFloat() < 0.4f) EnemyType.SPEAR_THROWER else EnemyType.SHIELD_BEARER
                else -> EnemyType.SKELETON
            }
            val pos = randomFloorPosition()
            game.enemies.add(Enemy(enemyType, pos, layerIndex))
        }
    }

    fun spawnElite(game: Game) {
        val count = when (layerIndex) {
            0 -> Random.nextInt(3, 5)
            1 -> Random.nextInt(4, 6)
            2 -> Random.nextInt(5, 7)
            else -> 4
        }

        for (i in 0 until count) {
            val enemyType = when (layerIndex) {
                0 -> if (Random.nextFloat() < 0.6f) EnemyType.WRAITH else EnemyType.SKELETON
                1 -> if (Random.nextFloat() < 0.5f) EnemyType.LAVA_CASTER else EnemyType.FLAME_DANCER
                2 -> if (Random.nextFloat() < 0.5f) EnemyType.SPEAR_THROWER else EnemyType.SHIELD_BEARER
                else -> EnemyType.SKELETON
            }
            val pos = randomFloorPosition()
            game.enemies.add(Enemy(enemyType, pos, layerIndex))
        }
    }

    fun spawnBoss(game: Game) {
        val bossType = when (layerIndex) {
            0 -> EnemyType.MEGA_SKELETON
            1 -> EnemyType.INFERNO_TITAN
            2 -> EnemyType.CHAMPION
            else -> EnemyType.MEGA_SKELETON
        }
        val bossPos = Vector2(width * 32f, height * 16f)
        game.enemies.add(Enemy(bossType, bossPos, layerIndex, isBoss = true))
    }

    /** Find a random floor tile position for spawning */
    private fun randomFloorPosition(): Vector2 {
        var attempts = 0
        while (attempts < 50) {
            val x = Random.nextInt(3, width - 3)
            val y = Random.nextInt(3, height - 3)
            val tile = getTile(x, y)
            if (tile == TILE_FLOOR) {
                return Vector2(x * 64f, y * 32f)
            }
            attempts++
        }
        return Vector2((width / 2) * 64f, (height / 2) * 32f)
    }

    fun unlockDoors() {
        for (door in doors) {
            door.isLocked = false
        }
    }
}
