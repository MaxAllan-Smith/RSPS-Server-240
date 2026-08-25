package org.example.app.core.experience

import org.example.app.core.player.Player
import org.example.app.core.skills.Skill

/**
 * Global gameplay experience configuration.
 *
 * 100 = normal experience
 * 200 = double experience
 * 50  = half experience
 */
data class ExperienceConfig(
    val globalRatePercent: Int = 100,
) {

    init {
        require(globalRatePercent >= 0) {
            "Global experience rate cannot be negative."
        }
    }
}

/**
 * Result of one gameplay XP award.
 */
data class ExperienceAward(
    val baseExperienceMilli: Int,
    val ratePercent: Int,
    val awardedExperience: Int,
    val carriedMilliExperience: Int,
)

/**
 * Global entry point for gameplay-generated experience.
 *
 * Gameplay features should use this service instead of applying their own
 * multipliers or writing directly to PlayerSkills.
 *
 * Experience is represented internally in thousandths:
 *
 *     25 XP   = 25_000
 *     37.5 XP = 37_500
 *     67.5 XP = 67_500
 *
 * The client/server's existing persistent XP representation is whole XP, so
 * fractional portions are carried forward until enough exists to award another
 * whole point.
 *
 * Example:
 *
 *     first Oak log  = 37 XP + 0.5 carried
 *     second Oak log = 38 XP
 *
 * Total = exactly 75 XP.
 */
class ExperienceService(
    private val config: ExperienceConfig,
) {

    fun award(
        player: Player,
        skill: Skill,
        baseExperienceMilli: Int,
    ): ExperienceAward {
        require(baseExperienceMilli >= 0) {
            "Base experience cannot be negative."
        }

        val state =
            player.experienceRateState

        /*
         * Using Long here prevents intermediate multiplication overflow.
         */
        val scaledMilli =
            (
                baseExperienceMilli.toLong() *
                    config.globalRatePercent
                ) / PERCENT_SCALE

        val totalMilli =
            scaledMilli +
                state.remainderMilli[skill.id]

        val wholeExperience =
            (
                totalMilli /
                    MILLI_PER_EXPERIENCE
                ).toInt()

        val remainder =
            (
                totalMilli %
                    MILLI_PER_EXPERIENCE
                ).toInt()

        state.remainderMilli[skill.id] =
            remainder

        if (wholeExperience > 0) {
            player.skills.addExperience(
                skill = skill,
                amount = wholeExperience,
            )
        }

        return ExperienceAward(
            baseExperienceMilli =
                baseExperienceMilli,

            ratePercent =
                config.globalRatePercent,

            awardedExperience =
                wholeExperience,

            carriedMilliExperience =
                remainder,
        )
    }

    private companion object {
        const val MILLI_PER_EXPERIENCE: Long =
            1_000L

        const val PERCENT_SCALE: Long =
            100L
    }
}

/**
 * Fractional gameplay XP that has not yet become a whole experience point.
 *
 * This is separate from the displayed whole-XP value.
 *
 * When we do the planned persistence/database pass, this small remainder is
 * the only additional field that needs persisting per skill.
 */
private class ExperienceRateState {

    val remainderMilli =
        IntArray(
            Skill.entries.size
        )
}

private val Player.experienceRateState:
    ExperienceRateState
    get() =
        featureState.getOrPut(
            ExperienceRateState::class,
            ::ExperienceRateState,
        )