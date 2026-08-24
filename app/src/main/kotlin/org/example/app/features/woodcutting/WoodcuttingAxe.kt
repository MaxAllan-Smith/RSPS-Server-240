package org.example.app.features.woodcutting

/**
 * Static metadata for an axe usable by Woodcutting.
 *
 * Axe IDs and requirements are added only after being verified for
 * the target game revision/server data.
 */
internal data class WoodcuttingAxe(
    val id: Int,
    val name: String,
    val woodcuttingLevel: Int,
    val priority: Int,
) {

    init {
        require(id >= 0) {
            "Woodcutting axe id must be non-negative."
        }

        require(woodcuttingLevel >= 1) {
            "Woodcutting axe level must be positive."
        }

        require(priority >= 0) {
            "Woodcutting axe priority must be non-negative."
        }
    }

    companion object {

        /**
         * Bronze axe.
         *
         * Item id 1351 is already verified by the existing equipment
         * implementation and revision-240 client captures.
         */
        val BRONZE =
            WoodcuttingAxe(
                id = 1351,
                name = "Bronze axe",
                woodcuttingLevel = 1,
                priority = 0,
            )

        val entries: List<WoodcuttingAxe> =
            listOf(
                BRONZE,
            )

        private val byId: Map<Int, WoodcuttingAxe> =
            entries.associateBy(
                WoodcuttingAxe::id,
            )

        fun find(
            itemId: Int,
        ): WoodcuttingAxe? =
            byId[itemId]
    }
}