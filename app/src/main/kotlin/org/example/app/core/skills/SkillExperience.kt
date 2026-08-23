package org.example.app.core.skills

import kotlin.math.floor
import kotlin.math.pow

object SkillExperience {

    const val MAX_LEVEL: Int = 99
    const val MAX_EXPERIENCE: Int = 200_000_000

    private val levelThresholds =
        IntArray(MAX_LEVEL + 1).apply {
            var points = 0

            for (level in 1 until MAX_LEVEL) {
                points +=
                    floor(
                        level + 300.0 * 2.0.pow(level / 7.0)
                    ).toInt()

                this[level + 1] = points / 4
            }
        }

    fun forLevel(level: Int): Int {
        require(level in 1..MAX_LEVEL) {
            "Level must be between 1 and $MAX_LEVEL."
        }

        return levelThresholds[level]
    }

    fun levelForExperience(experience: Int): Int {
        val capped =
            experience.coerceIn(
                minimumValue = 0,
                maximumValue = MAX_EXPERIENCE,
            )

        for (level in MAX_LEVEL downTo 1) {
            if (capped >= levelThresholds[level]) {
                return level
            }
        }

        return 1
    }
}