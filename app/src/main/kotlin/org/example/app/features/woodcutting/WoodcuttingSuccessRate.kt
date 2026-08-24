package org.example.app.features.woodcutting

import kotlin.random.Random

/**
 * Level-scaled Woodcutting success markers.
 *
 * [low] represents the low-level marker and [high] the high-level marker.
 * Intermediate Woodcutting levels are linearly interpolated.
 */
internal data class WoodcuttingSuccessRate(
    val low: Int,
    val high: Int,
) {

    init {
        require(low >= 0) {
            "Low Woodcutting success marker cannot be negative."
        }

        require(high >= low) {
            "High Woodcutting success marker cannot be below low marker."
        }
    }

    /**
     * Performs one independent random resource roll.
     */
    fun succeeds(
        level: Int,
        random: Random,
    ): Boolean {
        val chance =
            interpolatedChance(
                level
            )

        if (chance <= 0) {
            return false
        }

        if (chance >= ROLL_SCALE) {
            return true
        }

        return random.nextInt(
            ROLL_SCALE
        ) < chance
    }

    private fun interpolatedChance(
        level: Int,
    ): Int {
        val effectiveLevel =
            level.coerceIn(
                minimumValue =
                    MINIMUM_LEVEL,

                maximumValue =
                    MAXIMUM_LEVEL,
            )

        val offset =
            effectiveLevel -
                MINIMUM_LEVEL

        val range =
            high - low

        return (
            low +
                (
                    range *
                        offset /
                        LEVEL_RANGE
                    )
            ).coerceIn(
            minimumValue = 0,
            maximumValue = ROLL_SCALE,
        )
    }

    private companion object {
        const val MINIMUM_LEVEL: Int =
            1

        const val MAXIMUM_LEVEL: Int =
            99

        const val LEVEL_RANGE: Int =
            MAXIMUM_LEVEL -
                MINIMUM_LEVEL

        const val ROLL_SCALE: Int =
            255
    }
}

/**
 * Resource success markers for the Bronze axe.
 *
 * More axe tiers can be added to this catalogue without changing the
 * Woodcutting action engine.
 */
internal object WoodcuttingSuccessRates {

    private val bronzeRates:
        Map<
            WoodcuttingTreeType,
            WoodcuttingSuccessRate
        > =
        mapOf(
            WoodcuttingTreeType.REGULAR to
                rate(
                    low = 64,
                    high = 200,
                ),

            /*
             * Achey uses ordinary-tree chopping rates.
             */
            WoodcuttingTreeType.ACHEY to
                rate(
                    low = 64,
                    high = 200,
                ),

            WoodcuttingTreeType.OAK to
                rate(
                    low = 32,
                    high = 100,
                ),

            WoodcuttingTreeType.WILLOW to
                rate(
                    low = 16,
                    high = 50,
                ),

            WoodcuttingTreeType.TEAK to
                rate(
                    low = 15,
                    high = 46,
                ),

            /*
             * Mature juniper uses ordinary-tree axe success rates.
             */
            WoodcuttingTreeType.JUNIPER to
                rate(
                    low = 64,
                    high = 200,
                ),

            WoodcuttingTreeType.MAPLE to
                rate(
                    low = 8,
                    high = 25,
                ),

            WoodcuttingTreeType.HOLLOW to
                rate(
                    low = 18,
                    high = 26,
                ),

            WoodcuttingTreeType.MAHOGANY to
                rate(
                    low = 8,
                    high = 25,
                ),

            WoodcuttingTreeType.ARCTIC_PINE to
                rate(
                    low = 6,
                    high = 30,
                ),

            WoodcuttingTreeType.YEW to
                rate(
                    low = 4,
                    high = 12,
                ),

            WoodcuttingTreeType.MAGIC to
                rate(
                    low = 2,
                    high = 6,
                ),

            WoodcuttingTreeType.REDWOOD to
                rate(
                    low = 2,
                    high = 6,
                ),
        )

    fun find(
        tree: WoodcuttingTree,
        axe: WoodcuttingAxe,
    ): WoodcuttingSuccessRate? {
        /*
         * Bronze is currently the only fully-verified axe in this server's
         * Woodcutting catalogue.
         */
        if (
            axe.id !=
            WoodcuttingAxe.BRONZE.id
        ) {
            return null
        }

        return bronzeRates[
            tree.type
        ]
    }

    private fun rate(
        low: Int,
        high: Int,
    ): WoodcuttingSuccessRate =
        WoodcuttingSuccessRate(
            low = low,
            high = high,
        )
}