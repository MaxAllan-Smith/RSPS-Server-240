package org.example.app.features.woodcutting

import kotlin.random.Random

/**
 * OSRS Woodcutting success markers.
 *
 * [low] is the level-1 success marker and [high] is the level-99 marker,
 * expressed on a 0..255 scale. Intermediate levels are linearly
 * interpolated.
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
            "High Woodcutting success marker must be >= low marker."
        }
    }

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

        if (chance >= MAX_ROLL) {
            return true
        }

        /*
         * nextInt(255) produces 0..254, giving an exact chance/MAX_ROLL
         * interpretation for these success markers.
         */
        return random.nextInt(
            MAX_ROLL
        ) < chance
    }

    private fun interpolatedChance(
        level: Int,
    ): Int {
        val effectiveLevel =
            level.coerceIn(
                minimumValue = MIN_LEVEL,
                maximumValue = MAX_LEVEL,
            )

        val levelOffset =
            effectiveLevel -
                MIN_LEVEL

        val range =
            high - low

        return (
            low +
                (
                    range *
                        levelOffset /
                        LEVEL_RANGE
                    )
            ).coerceIn(
            minimumValue = 0,
            maximumValue = MAX_ROLL,
        )
    }

    private companion object {
        const val MIN_LEVEL: Int =
            1

        const val MAX_LEVEL: Int =
            99

        const val LEVEL_RANGE: Int =
            MAX_LEVEL - MIN_LEVEL

        const val MAX_ROLL: Int =
            255
    }
}

/**
 * Success-rate catalogue.
 *
 * We add combinations incrementally as their tool/resource data is verified.
 */
internal object WoodcuttingSuccessRates {

    private val regularBronze =
        WoodcuttingSuccessRate(
            low = 64,
            high = 200,
        )

    fun find(
        tree: WoodcuttingTree,
        axe: WoodcuttingAxe,
    ): WoodcuttingSuccessRate? =
        when {
            tree.type ==
                WoodcuttingTreeType.REGULAR &&
                axe.id ==
                WoodcuttingAxe.BRONZE.id ->
                regularBronze

            else ->
                null
        }
}