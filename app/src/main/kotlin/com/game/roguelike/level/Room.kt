package com.game.roguelike.level

import com.game.roguelike.core.Game
import com.game.roguelike.core.RoomType
import com.game.roguelike.entity.*
import com.game.roguelike.util.Vector2
import kotlin.random.Random

class Room(
    val width: Int = 20,
    val height: Int = 14,
    val type: RoomType,
    val layerIndex: Int = 0
) {
    companion object {
        const val TILE_FLOOR = 0
        const val TILE_WALL = 1
        const val TILE_OBSTACLE = 2
        const val TILE_DOOR = 3
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

                // Random obstacles
                val obstacleCount = Random.nextInt(2, 5)
                for (i in 0 until obstacleCount) {
                    val ox = Random.nextInt(2, width - 2)
                    val oy = Random.nextInt(2, height - 2)
                    setTile(ox, oy, TILE_OBSTACLE)
                }
            }
            RoomType.REWARD -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
            }
            RoomType.SHOP -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = false))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = false))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
                merchantPosition = Vector2(width * 32f, height * 16f / 2f)
            }
            RoomType.BOSS -> {
                setTile(0, height / 2, TILE_DOOR)
                setTile(width - 1, height / 2, TILE_DOOR)
                doors.add(Door(Vector2(0f, (height / 2) * 32f), isLocked = true))
                doors.add(Door(Vector2((width - 1) * 64f, (height / 2) * 32f), isLocked = true))
                spawnPoint = Vector2(64f * 2f, (height / 2) * 32f)
            }
        }

        // Decorative obstacles for non-entry rooms
        if (type != RoomType.ENTRY) {
            val cornerObstacles = 2
            for (i in 0 until cornerObstacles) {
                setTile(1 + i, 1, TILE_OBSTACLE)
                setTile(width - 2 - i, 1, TILE_OBSTACLE)
                setTile(1 + i, height - 2, TILE_OBSTACLE)
                setTile(width - 2 - i, height - 2, TILE_OBSTACLE)
            }
        }
    }

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
            0 -> Random.nextInt(3, 6)
            1 -> Random.nextInt(3, 5)
            2 -> Random.nextInt(2, 4)
            else -> 3
        }

        for (i in 0 until count) {
            val enemyType = when (layerIndex) {
                0 -> if (Random.nextFloat() < 0.4f) EnemyType.WRAITH else EnemyType.SKELETON
                1 -> if (Random.nextFloat() < 0.4f) EnemyType.LAVA_CASTER else EnemyType.FLAME_DANCER
                2 -> if (Random.nextFloat() < 0.4f) EnemyType.SPEAR_THROWER else EnemyType.SHIELD_BEARER
                else -> EnemyType.SKELETON
            }
            val pos = Vector2(
                (Random.nextInt(3, width - 3)) * 64f,
                (Random.nextInt(2, height - 2)) * 32f
            )
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

    fun unlockDoors() {
        for (door in doors) {
            door.isLocked = false
        }
    }
}