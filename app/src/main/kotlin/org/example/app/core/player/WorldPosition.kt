package org.example.app.core.player

data class WorldPosition(
    val x: Int,
    val z: Int,
    val level: Int = 0,
) {
    val zoneX: Int
        get() = x ushr 3

    val zoneZ: Int
        get() = z ushr 3

    companion object {
        val LUMBRIDGE =
            WorldPosition(
                x = 3222,
                z = 3218,
                level = 0,
            )
    }
}
