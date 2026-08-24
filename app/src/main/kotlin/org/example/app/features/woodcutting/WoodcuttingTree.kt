package org.example.app.features.woodcutting

import org.rsmod.routefinder.loc.LocShapeConstants

/**
 * Ordinary axe-based Woodcutting resource families.
 *
 * Special activities with materially different mechanics should use their own
 * implementations rather than being forced through this generic tree loop.
 */
internal enum class WoodcuttingTreeType {
    REGULAR,
    ACHEY,
    OAK,
    WILLOW,
    TEAK,
    JUNIPER,
    MAPLE,
    HOLLOW,
    MAHOGANY,
    ARCTIC_PINE,
    YEW,
    MAGIC,
    REDWOOD,
}

/**
 * Product and canonical XP from one successful Woodcutting resource roll.
 *
 * Experience is stored in thousandths so fractional XP remains exact:
 *
 * 25.0 XP = 25_000
 * 37.5 XP = 37_500
 * 67.5 XP = 67_500
 */
internal data class WoodcuttingReward(
    val itemId: Int,
    val itemName: String,
    val experienceMilli: Int,
) {

    init {
        require(itemId >= 0) {
            "Woodcutting reward item id must be non-negative."
        }

        require(experienceMilli >= 0) {
            "Woodcutting reward XP must be non-negative."
        }
    }

    /**
     * Human-readable canonical XP value for logging.
     */
    fun formattedExperience(): String {
        val whole =
            experienceMilli /
                MILLI_PER_EXPERIENCE

        val remainder =
            experienceMilli %
                MILLI_PER_EXPERIENCE

        if (remainder == 0) {
            return whole.toString()
        }

        return buildString {
            append(whole)

            append('.')

            append(
                remainder
                    .toString()
                    .padStart(
                        length = 3,
                        padChar = '0',
                    )
                    .trimEnd('0')
            )
        }
    }

    private companion object {
        const val MILLI_PER_EXPERIENCE: Int =
            1_000
    }
}

/**
 * Defines how a live Woodcutting resource becomes depleted.
 */
internal sealed interface WoodcuttingDepletion {

    /**
     * A single successful product immediately depletes the resource.
     *
     * Used by ordinary trees.
     */
    data object Immediate :
        WoodcuttingDepletion

    /**
     * The resource survives for a number of actively-cut game ticks.
     *
     * Successful products may be obtained during that lifetime. Once the
     * active-cut duration has been reached, a later successful resource roll
     * depletes it.
     */
    data class ActiveCutDuration(
        val ticks: Int,
    ) : WoodcuttingDepletion {

        init {
            require(ticks > 0) {
                "Active-cut duration must be positive."
            }
        }
    }

    /**
     * The resource independently rolls for depletion after each successful
     * product.
     */
    data class ChancePerSuccess(
        val denominator: Int,
    ) : WoodcuttingDepletion {

        init {
            require(denominator > 0) {
                "Depletion denominator must be positive."
            }
        }
    }
}

/**
 * Static metadata for one logical Woodcutting resource family.
 *
 * [locStumps] maps live tree location ids to their corresponding depleted
 * location id.
 *
 * A logical resource may have several client loc variants and, importantly,
 * those variants do not always share the same stump model.
 */
internal data class WoodcuttingTree(
    val type: WoodcuttingTreeType,
    val name: String,

    /**
     * live loc id -> depleted/stump loc id
     */
    val locStumps: Map<Int, Int>,

    val requiredLevel: Int,

    val reward: WoodcuttingReward,

    val depletion: WoodcuttingDepletion,

    /**
     * Inclusive respawn range measured in 600ms server game ticks.
     */
    val respawnTicks: IntRange,

    /**
     * Current supported definitions are centrepiece scenery unless explicitly
     * expanded later with cache-derived placement information.
     */
    val locShape: Int =
        LocShapeConstants
            .CENTREPIECE_STRAIGHT,

    val locRotation: Int =
        0,

    /**
     * Positional falling-tree sound.
     */
    val fallSoundId: Int? =
        TREE_FALL_SOUND,
) {

    init {
        require(locStumps.isNotEmpty()) {
            "A Woodcutting tree needs at least one loc."
        }

        require(requiredLevel >= 1) {
            "Woodcutting requirement must be positive."
        }

        require(
            !respawnTicks.isEmpty() &&
                respawnTicks.first > 0
        ) {
            "Woodcutting respawn ticks must be positive."
        }
    }

    fun matches(
        locId: Int,
    ): Boolean =
        locId in
            locStumps

    fun stumpFor(
        locId: Int,
    ): Int? =
        locStumps[
            locId
        ]

    companion object {

        /**
         * Regular trees.
         *
         * The 50..80 tick respawn is custom server tuning from the earlier
         * implementation rather than the longer reference range.
         */
        val REGULAR =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.REGULAR,

                name =
                    "Tree",

                locStumps =
                    mapOf(
                        1276 to 1342,
                        1277 to 1343,
                        1278 to 1342,
                        1279 to 1342,
                        1280 to 1343,
                    ),

                requiredLevel =
                    1,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1511,

                        itemName =
                            "logs",

                        experienceMilli =
                            25_000,
                    ),

                depletion =
                    WoodcuttingDepletion.Immediate,

                /*
                 * 30..48 seconds.
                 */
                respawnTicks =
                    50..80,
            )

        /**
         * Achey tree.
         */
        val ACHEY =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.ACHEY,

                name =
                    "Achey tree",

                locStumps =
                    mapOf(
                        2023 to 3371,
                    ),

                requiredLevel =
                    1,

                reward =
                    WoodcuttingReward(
                        itemId =
                            2862,

                        itemName =
                            "achey tree logs",

                        experienceMilli =
                            25_000,
                    ),

                depletion =
                    WoodcuttingDepletion.Immediate,

                respawnTicks =
                    60..100,
            )

        /**
         * Oak.
         *
         * Oak is a multi-log resource. It remains available while its shared
         * active-cut lifetime is below 45 game ticks.
         */
        val OAK =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.OAK,

                name =
                    "Oak tree",

                locStumps =
                    mapOf(
                        10820 to 1356,
                    ),

                requiredLevel =
                    15,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1521,

                        itemName =
                            "oak logs",

                        experienceMilli =
                            37_500,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            45,
                    ),

                respawnTicks =
                    15..15,
            )

        /**
         * Willow variants currently supported by the revision-240 server.
         */
        val WILLOW =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.WILLOW,

                name =
                    "Willow tree",

                locStumps =
                    mapOf(
                        10819 to 9711,
                        10829 to 9471,
                        10831 to 9471,
                        10833 to 9471,
                    ),

                requiredLevel =
                    30,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1519,

                        itemName =
                            "willow logs",

                        experienceMilli =
                            67_500,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            50,
                    ),

                respawnTicks =
                    15..15,
            )

        /**
         * Teak.
         */
        val TEAK =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.TEAK,

                name =
                    "Teak tree",

                locStumps =
                    mapOf(
                        9036 to 9037,
                    ),

                requiredLevel =
                    35,

                reward =
                    WoodcuttingReward(
                        itemId =
                            6333,

                        itemName =
                            "teak logs",

                        experienceMilli =
                            85_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            50,
                    ),

                respawnTicks =
                    16..16,
            )

        /**
         * Mature juniper.
         *
         * This uses probability-based depletion rather than an active duration.
         */
        val JUNIPER =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.JUNIPER,

                name =
                    "Mature juniper tree",

                locStumps =
                    mapOf(
                        27499 to 27500,
                    ),

                requiredLevel =
                    42,

                reward =
                    WoodcuttingReward(
                        itemId =
                            13355,

                        itemName =
                            "juniper logs",

                        experienceMilli =
                            35_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ChancePerSuccess(
                        denominator =
                            16,
                    ),

                respawnTicks =
                    15..15,
            )

        /**
         * Maple.
         */
        val MAPLE =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.MAPLE,

                name =
                    "Maple tree",

                locStumps =
                    mapOf(
                        10832 to 9712,
                    ),

                requiredLevel =
                    45,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1517,

                        itemName =
                            "maple logs",

                        experienceMilli =
                            100_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            100,
                    ),

                respawnTicks =
                    60..60,
            )

        /**
         * Hollow trees.
         */
        val HOLLOW =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.HOLLOW,

                name =
                    "Hollow tree",

                locStumps =
                    mapOf(
                        10821 to 2310,
                        10830 to 4061,
                    ),

                requiredLevel =
                    45,

                reward =
                    WoodcuttingReward(
                        itemId =
                            3239,

                        itemName =
                            "bark",

                        experienceMilli =
                            82_500,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            60,
                    ),

                respawnTicks =
                    43..43,
            )

        /**
         * Mahogany.
         */
        val MAHOGANY =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.MAHOGANY,

                name =
                    "Mahogany tree",

                locStumps =
                    mapOf(
                        9034 to 9035,
                    ),

                requiredLevel =
                    50,

                reward =
                    WoodcuttingReward(
                        itemId =
                            6332,

                        itemName =
                            "mahogany logs",

                        experienceMilli =
                            125_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            100,
                    ),

                respawnTicks =
                    15..15,
            )

        /**
         * Arctic pine.
         */
        val ARCTIC_PINE =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.ARCTIC_PINE,

                name =
                    "Arctic pine",

                locStumps =
                    mapOf(
                        3037 to 21274,
                    ),

                requiredLevel =
                    54,

                reward =
                    WoodcuttingReward(
                        itemId =
                            10810,

                        itemName =
                            "arctic pine logs",

                        experienceMilli =
                            40_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            140,
                    ),

                respawnTicks =
                    15..15,
            )

        /**
         * Yew.
         */
        val YEW =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.YEW,

                name =
                    "Yew tree",

                locStumps =
                    mapOf(
                        10822 to 9714,
                    ),

                requiredLevel =
                    60,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1515,

                        itemName =
                            "yew logs",

                        experienceMilli =
                            175_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            190,
                    ),

                respawnTicks =
                    100..100,
            )

        /**
         * Magic.
         */
        val MAGIC =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.MAGIC,

                name =
                    "Magic tree",

                locStumps =
                    mapOf(
                        10834 to 9713,
                    ),

                requiredLevel =
                    75,

                reward =
                    WoodcuttingReward(
                        itemId =
                            1513,

                        itemName =
                            "magic logs",

                        experienceMilli =
                            250_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            390,
                    ),

                respawnTicks =
                    200..200,
            )

        /**
         * Redwood has two independently-interactable faces, each with its own
         * depleted loc.
         */
        val REDWOOD =
            WoodcuttingTree(
                type =
                    WoodcuttingTreeType.REDWOOD,

                name =
                    "Redwood",

                locStumps =
                    mapOf(
                        29668 to 29669,
                        29670 to 29671,
                    ),

                requiredLevel =
                    90,

                reward =
                    WoodcuttingReward(
                        itemId =
                            19669,

                        itemName =
                            "redwood logs",

                        experienceMilli =
                            380_000,
                    ),

                depletion =
                    WoodcuttingDepletion.ActiveCutDuration(
                        ticks =
                            440,
                    ),

                respawnTicks =
                    200..200,
            )

        /**
         * Complete generic-tree catalogue.
         */
        val entries:
            List<WoodcuttingTree> =
            listOf(
                REGULAR,
                ACHEY,
                OAK,
                WILLOW,
                TEAK,
                JUNIPER,
                MAPLE,
                HOLLOW,
                MAHOGANY,
                ARCTIC_PINE,
                YEW,
                MAGIC,
                REDWOOD,
            )

        /**
         * Hot-path lookup from the concrete loc id received from the client to
         * its logical Woodcutting definition.
         *
         * Build this once at class initialization instead of scanning every
         * tree definition whenever the player clicks scenery.
         */
        private val byLocId:
            Map<Int, WoodcuttingTree> =
            buildLocIndex(
                definitions =
                    entries,
            )

        fun find(
            locId: Int,
        ): WoodcuttingTree? =
            byLocId[
                locId
            ]

        /**
         * Kept outside buildMap's lambda so the name "entries" cannot resolve
         * to MutableMap.entries through the builder receiver.
         */
        private fun buildLocIndex(
            definitions:
                List<WoodcuttingTree>,
        ): Map<Int, WoodcuttingTree> =
            buildMap {
                for (
                    tree in
                    definitions
                ) {
                    for (
                        locId in
                        tree.locStumps.keys
                    ) {
                        val previous =
                            put(
                                key =
                                    locId,

                                value =
                                    tree,
                            )

                        check(
                            previous == null
                        ) {
                            "Duplicate Woodcutting loc id $locId: " +
                                "${previous?.name} and ${tree.name}."
                        }
                    }
                }
            }

        private const val TREE_FALL_SOUND: Int =
            2734
    }
}