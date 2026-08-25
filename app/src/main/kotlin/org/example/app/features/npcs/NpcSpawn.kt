package org.example.app.features.npcs

import org.example.app.core.player.WorldPosition

/**
 * Immutable definition of a world NPC spawn.
 *
 * Dynamic state such as hitpoints, movement and respawn timers will live on
 * the runtime NPC rather than on this definition.
 */
internal data class NpcSpawn(
    val id: Int,
    val name: String,
    val position: WorldPosition,
    val direction: Int = SOUTH,
) {

    init {
        require(
            direction in
                0..7
        ) {
            "NPC direction must be in the range 0..7."
        }
    }

    companion object {

        const val NORTH_WEST: Int =
            0

        const val NORTH: Int =
            1

        const val NORTH_EAST: Int =
            2

        const val WEST: Int =
            3

        const val EAST: Int =
            4

        const val SOUTH_WEST: Int =
            5

        const val SOUTH: Int =
            6

        const val SOUTH_EAST: Int =
            7
    }
}