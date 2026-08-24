package org.example.app.features.woodcutting

/**
 * Logical category of Woodcutting resource.
 */
internal enum class WoodcuttingTreeType {
    REGULAR,
    OAK,
    WILLOW,
}

/**
 * Item/XP produced by a successful Woodcutting roll.
 *
 * Only rewards that can currently be represented exactly by the server skill
 * model are enabled here.
 */
internal data class WoodcuttingReward(
    val itemId: Int,
    val itemName: String,
    val experience: Int,
)

/**
 * Static metadata for a supported Woodcutting tree.
 *
 * Location IDs remain explicitly revision-verified rather than relying on
 * broad historical RSPS ID lists.
 */
internal data class WoodcuttingTree(
    val type: WoodcuttingTreeType,
    val name: String,
    val locIds: Set<Int>,
    val requiredLevel: Int,
    val reward: WoodcuttingReward?,
) {

    init {
        require(requiredLevel >= 1) {
            "Woodcutting tree level must be positive."
        }
    }

    fun matches(
        locId: Int,
    ): Boolean =
        locId in locIds

    companion object {

        /**
         * Verified revision-240 variants:
         *
         * tree  = 1276
         * tree2 = 1278
         *
         * Regular trees require level 1 and produce normal logs.
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
                        itemId = 1511,
                        itemName = "logs",
                        experience = 25,
                    ),
            )

        /**
         * Verified revision-240 oak:
         *
         * oaktree = 10820
         *
         * Oak chopping itself is enabled in a later step once fractional XP
         * handling is defined cleanly for the generic skill model.
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
            )

        /**
         * Verified revision-240 Willow variants captured so far:
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