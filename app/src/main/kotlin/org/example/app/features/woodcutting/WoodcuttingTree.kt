package org.example.app.features.woodcutting

/**
 * Static metadata describing a tree supported by Woodcutting.
 *
 * IDs in this file have been verified directly against revision-240
 * location interaction packets.
 *
 * Resource item IDs, XP, requirements, depletion and respawn metadata
 * will be added incrementally as those mechanics are implemented.
 */
internal data class WoodcuttingTree(
    val name: String,
    val locIds: Set<Int>,
) {

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
         */
        val REGULAR =
            WoodcuttingTree(
                name = "Tree",
                locIds =
                    setOf(
                        1276,
                        1278,
                    ),
            )

        /**
         * Revision-240 oak tree observed:
         *
         * oaktree = 10820
         */
        val OAK =
            WoodcuttingTree(
                name = "Oak tree",
                locIds =
                    setOf(
                        10820,
                    ),
            )

        /**
         * Revision-240 willow variants observed:
         *
         * willowtree  = 10819
         * willow_tree3 = 10831
         */
        val WILLOW =
            WoodcuttingTree(
                name = "Willow tree",
                locIds =
                    setOf(
                        10819,
                        10831,
                    ),
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