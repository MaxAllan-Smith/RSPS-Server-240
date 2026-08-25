package org.example.app.features.npcs

import org.example.app.core.player.WorldPosition

/**
 * Initial Lumbridge NPC population.
 *
 * This is intentionally a small first vertical slice so NPC synchronization
 * can be verified before interaction, movement, combat, death and respawning
 * are layered on top.
 *
 * These coordinates are server-owned initial placements in the appropriate
 * Lumbridge areas; they are not intended to represent a complete official
 * Jagex NPC spawn dump.
 */
internal object LumbridgeNpcSpawns {

    val all:
        List<NpcSpawn> =
        listOf(
            /*
             * Lumbridge Castle / courtyard.
             */
            NpcSpawn(
                id =
                    HANS,

                name =
                    "Hans",

                position =
                    WorldPosition(
                        x =
                            3221,

                        z =
                            3218,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.SOUTH,
            ),

            NpcSpawn(
                id =
                    MAN,

                name =
                    "Man",

                position =
                    WorldPosition(
                        x =
                            3223,

                        z =
                            3219,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.WEST,
            ),

            NpcSpawn(
                id =
                    WOMAN,

                name =
                    "Woman",

                position =
                    WorldPosition(
                        x =
                            3220,

                        z =
                            3220,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.EAST,
            ),

            /*
             * Goblins east/north-east of Lumbridge Castle.
             */
            NpcSpawn(
                id =
                    GOBLIN_LEVEL_2,

                name =
                    "Goblin",

                position =
                    WorldPosition(
                        x =
                            3244,

                        z =
                            3244,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.WEST,
            ),

            NpcSpawn(
                id =
                    GOBLIN_LEVEL_2,

                name =
                    "Goblin",

                position =
                    WorldPosition(
                        x =
                            3246,

                        z =
                            3244,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.EAST,
            ),

            NpcSpawn(
                id =
                    GOBLIN_LEVEL_5,

                name =
                    "Goblin",

                position =
                    WorldPosition(
                        x =
                            3244,

                        z =
                            3247,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.SOUTH,
            ),

            NpcSpawn(
                id =
                    GOBLIN_LEVEL_2,

                name =
                    "Goblin",

                position =
                    WorldPosition(
                        x =
                            3246,

                        z =
                            3247,

                        level =
                            0,
                    ),

                direction =
                    NpcSpawn.SOUTH_WEST,
            ),
        )

    private const val HANS: Int =
        3105

    private const val MAN: Int =
        3106

    private const val WOMAN: Int =
        3111

    private const val GOBLIN_LEVEL_2: Int =
        3028

    private const val GOBLIN_LEVEL_5: Int =
        3045
}