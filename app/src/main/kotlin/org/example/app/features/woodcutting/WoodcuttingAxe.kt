package org.example.app.features.woodcutting

/**
 * Static Woodcutting metadata for an axe.
 *
 * Attack requirements remain an equipment concern. Woodcutting only needs to
 * know whether the tool is usable for the skill and which action sequence it
 * should play.
 */
internal data class WoodcuttingAxe(
    val id: Int,
    val name: String,
    val woodcuttingLevel: Int,
    val animationId: Int,
    val priority: Int,
) {

    init {
        require(id >= 0) {
            "Woodcutting axe id must be non-negative."
        }

        require(woodcuttingLevel >= 1) {
            "Woodcutting axe level must be positive."
        }

        require(animationId >= 0) {
            "Woodcutting animation id must be non-negative."
        }

        require(priority >= 0) {
            "Woodcutting axe priority must be non-negative."
        }
    }

    companion object {

        /**
         * Bronze axe.
         *
         * Item:      1351
         * WC level:  1
         * Sequence:  879 - human_woodcutting_bronze_axe
         */
        val BRONZE =
            WoodcuttingAxe(
                id = 1351,
                name = "Bronze axe",
                woodcuttingLevel = 1,
                animationId = 879,
                priority = 0,
            )

        val entries:
            List<WoodcuttingAxe> =
            listOf(
                BRONZE,
            )

        private val byId:
            Map<Int, WoodcuttingAxe> =
            entries.associateBy(
                WoodcuttingAxe::id,
            )

        fun find(
            itemId: Int,
        ): WoodcuttingAxe? =
            byId[itemId]
    }
}