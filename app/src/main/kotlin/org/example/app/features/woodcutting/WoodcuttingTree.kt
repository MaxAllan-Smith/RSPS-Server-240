package org.example.app.features.woodcutting

import org.rsmod.routefinder.loc.LocShapeConstants

/**
 * Logical category of Woodcutting resource.
 */
internal enum class WoodcuttingTreeType {
    REGULAR,
    OAK,
    WILLOW,
}

/**
 * Item/XP produced by a successful Woodcutting resource roll.
 *
 * The XP representation will be replaced by the global fixed-point
 * ExperienceService in the next architecture step so fractional rewards such
 * as 37.5 and 67.5 XP can be represented exactly.
 */
internal data class WoodcuttingReward(
    val itemId: Int,
    val itemName: String,
    val experience: Int,
)

/**
 * Static metadata for a supported Woodcutting tree.
 *
 * These definitions intentionally remain in Kotlin while the mechanics are
 * still being developed. Once the data shape stabilizes they will move behind
 * a SQLite-backed WoodcuttingTreeRepository and be preloaded into memory.
 */
internal data class WoodcuttingTree(
    val type: WoodcuttingTreeType,
    val name: String,
    val locIds: Set<Int>,
    val requiredLevel: Int,
    val reward: WoodcuttingReward?,

    /**
     * Runtime loc used while the resource is depleted.
     */
    val stumpId: Int?,

    /**
     * Inclusive server-game-tick respawn range.
     */
    val respawnTicks: IntRange?,

    /**
     * Placement information required when replacing the static cache loc with
     * a dynamic runtime loc.
     */
    val locShape: Int =
        LocShapeConstants
            .CENTREPIECE_STRAIGHT,

    val locRotation: Int =
        0,

    /**
     * Positional sound played when this tree falls.
     */
    val fallSoundId: Int? =
        null,
) {

    init {
        require(requiredLevel >= 1) {
            "Woodcutting tree level must be positive."
        }

        if (respawnTicks != null) {
            require(
                !respawnTicks.isEmpty() &&
                    respawnTicks.first > 0
            ) {
                "Tree respawn range must contain positive ticks."
            }
        }
    }

    fun matches(
        locId: Int,
    ): Boolean =
        locId in locIds

    companion object {

        /**
         * Revision-240 regular tree variants verified so far:
         *
         * tree  = 1276
         * tree2 = 1278
         *
         * Custom server tuning:
         *
         * The underlying reference data uses a longer randomized respawn.
         * Normal trees are intentionally shortened here to 50..80 game ticks
         * so early Woodcutting areas recover more quickly.
         */
        val REGULAR =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.REGULAR,

                name =
                    "Tree",

                locIds =
                    setOf(
                        1276,
                        1278,
                    ),

                requiredLevel =
                    1,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1511,

                        itemName =
                            "logs",

                        experience =
                            25,
                    ),

                stumpId =
                    1342,

                /*
                 * 50..80 × 600 ms
                 *
                 * = 30.0 .. 48.0 seconds.
                 */
                respawnTicks =
                    50..80,

                fallSoundId =
                    2734,
            )

        /**
         * Oak is already recognized and its level requirement is active.
         *
         * The complete reward/depletion loop is enabled after the generic XP
         * service can represent its fractional XP reward exactly.
         */
        val OAK =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.OAK,

                name =
                    "Oak tree",

                locIds =
                    setOf(
                        10820,
                    ),

                requiredLevel =
                    15,

                reward =
                    null,

                stumpId =
                    null,

                respawnTicks =
                    null,
            )

        /**
         * Revision-240 Willow variants verified so far:
         *
         * willowtree   = 10819
         * willow_tree2 = 10829
         * willow_tree3 = 10831
         * willow_tree4 = 10833
         */
        val WILLOW =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.WILLOW,

                name =
                    "Willow tree",

                locIds =
                    setOf(
                        10819,
                        10829,
                        10831,
                        10833,
                    ),

                requiredLevel =
                    30,

                reward =
                    null,

                stumpId =
                    null,

                respawnTicks =
                    null,
            )

        val entries:
            List<WoodcuttingTree> =
            listOf(
                REGULAR,
                OAK,
                WILLOW,
            )

        fun find(
            locId: Int,
        ): WoodcuttingTree? =
            entries.firstOrNull {
                it.matches(
                    locId
                )
            }
    }
}