package org.example.app.features.woodcutting

/**
 * Static metadata describing a tree that can be used by the
 * woodcutting feature.
 *
 * At this stage we only store identifiers that have been verified
 * against revision 240 packet captures.
 *
 * Skill requirements, logs, experience, depletion and respawn
 * metadata will be added once their revision-240 values are
 * introduced and verified.
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
         * Verified from the revision-240 client:
         *
         * oploc1_v2 id=tree (1276)
         */
        val REGULAR =
            WoodcuttingTree(
                name = "Tree",
                locIds =
                    setOf(
                        1276,
                    ),
            )

        /**
         * Verified from the revision-240 client:
         *
         * oploc1_v2 id=willow_tree3 (10831)
         */
        val WILLOW =
            WoodcuttingTree(
                name = "Willow tree",
                locIds =
                    setOf(
                        10831,
                    ),
            )

        val entries: List<WoodcuttingTree> =
            listOf(
                REGULAR,
                WILLOW,
            )

        fun find(
            locId: Int,
        ): WoodcuttingTree? =
            entries.firstOrNull {
                it.matches(locId)
            }
    }
}