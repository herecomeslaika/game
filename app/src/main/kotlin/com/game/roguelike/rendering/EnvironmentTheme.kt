package com.game.roguelike.rendering

data class EnvironmentTheme(
    val name: String,
    val skyTop: Int,
    val skyBottom: Int,
    val distantGround: Int,
    val distantGroundAlt: Int,
    val accent: Int,
    val glow: Int,
    val landmarks: List<EnvironmentLandmarkKind>
) {
    companion object {
        fun forLayer(layerIndex: Int): EnvironmentTheme {
            return when (layerIndex.coerceIn(0, 2)) {
                0 -> EnvironmentTheme(
                    name = "Tartarus Abyss",
                    skyTop = 0xFF040207.toInt(),
                    skyBottom = 0xFF130A20.toInt(),
                    distantGround = 0xFF151024.toInt(),
                    distantGroundAlt = 0xFF211737.toInt(),
                    accent = 0xFF7B5EA7.toInt(),
                    glow = 0xFF55FFAA.toInt(),
                    landmarks = listOf(
                        EnvironmentLandmarkKind.GRAVE,
                        EnvironmentLandmarkKind.GHOST,
                        EnvironmentLandmarkKind.BONE
                    )
                )
                1 -> EnvironmentTheme(
                    name = "Asphodel Inferno",
                    skyTop = 0xFF100400.toInt(),
                    skyBottom = 0xFF2A0800.toInt(),
                    distantGround = 0xFF241008.toInt(),
                    distantGroundAlt = 0xFF3A1608.toInt(),
                    accent = 0xFFFF6633.toInt(),
                    glow = 0xFFFFB13B.toInt(),
                    landmarks = listOf(
                        EnvironmentLandmarkKind.LAVA_POOL,
                        EnvironmentLandmarkKind.LAVA_BEAST
                    )
                )
                else -> EnvironmentTheme(
                    name = "Elysium Gardens",
                    skyTop = 0xFF03100A.toInt(),
                    skyBottom = 0xFF0B2414.toInt(),
                    distantGround = 0xFF102716.toInt(),
                    distantGroundAlt = 0xFF17391F.toInt(),
                    accent = 0xFF44BB66.toInt(),
                    glow = 0xFFA5FFC7.toInt(),
                    landmarks = listOf(
                        EnvironmentLandmarkKind.BANNER,
                        EnvironmentLandmarkKind.CASTLE
                    )
                )
            }
        }
    }
}

enum class EnvironmentLandmarkKind {
    GRAVE,
    GHOST,
    BONE,
    LAVA_POOL,
    LAVA_BEAST,
    BANNER,
    CASTLE
}
