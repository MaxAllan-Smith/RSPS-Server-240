package org.example.app.features.skills.levelup

import org.example.app.core.player.Player
import org.example.app.core.skills.Skill
import org.example.app.core.skills.SkillChange

internal data class SkillLevelUpUnlocks(
    val skill: Skill,
    val firstLevel: Int,
    val lastLevel: Int,
)

internal class SkillLevelUpState {

    private val pendingUnlocks =
        mutableMapOf<Skill, SkillLevelUpUnlocks>()

    fun record(change: SkillChange) {
        if (!change.levelledUp) return

        val skill =
            change.skill

        val existing =
            pendingUnlocks[skill]

        pendingUnlocks[skill] =
            SkillLevelUpUnlocks(
                skill = skill,
                firstLevel =
                    existing?.firstLevel
                        ?: (change.previousLevel + 1),
                lastLevel = change.level,
            )
    }

    fun consume(skill: Skill): SkillLevelUpUnlocks? =
        pendingUnlocks.remove(skill)
}

internal val Player.skillLevelUpState: SkillLevelUpState
    get() =
        featureState.getOrPut(
            SkillLevelUpState::class,
            ::SkillLevelUpState,
        )