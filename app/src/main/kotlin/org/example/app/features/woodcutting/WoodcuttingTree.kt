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
 * Item/XP produced by a successful resource roll.
 */
internal data class WoodcuttingReward(
    val itemId: Int,
    val itemName: String,
    val experience: Int,
)

/**
 * Static metadata for a supported Woodcutting tree.
 *
 * This remains in Kotlin for the current incremental implementation.
 *
 * Once the mechanics are stable, these fields are strong candidates for the
 * SQLite-backed Woodcutting definition repository discussed earlier.
 */
internal data class WoodcuttingTree(
    val type: WoodcuttingTreeType,
    val name: String,
    val locIds: Set<Int>,
    val requiredLevel: Int,
    val reward: WoodcuttingReward?,

    /**
     * Loc used while this tree is depleted.
     *
     * Null means the depletion stage has not yet been enabled for this tree.
     */
    val stumpId: Int?,

    /**
     * Inclusive server-tick respawn range.
     */
    val respawnTicks: IntRange?,

    /**
     * Static placement properties required by LocAddChangeV2.
     *
     * The currently-supported regular trees are ordinary centrepiece locs.
     */
    val locShape: Int =
        LocShapeConstants
            .CENTREPIECE_STRAIGHT,

    val locRotation: Int =
        0,

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
         * Revision-240 regular tree variants observed:
         *
         * tree  = 1276
         * tree2 = 1278
         *
         * Current RSMod Woodcutting definitions map both to regular stump 1342
         * and use a randomized 60..100 tick respawn.
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

                respawnTicks =
                    60..100,

                fallSoundId =
                    2734,
            )

        /**
         * Oak interaction and level requirement are already enabled.
         *
         * Its full resource/depletion loop remains disabled until the generic
         * fractional XP system is introduced.
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
         * Revision-240 Willow variants captured so far:
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