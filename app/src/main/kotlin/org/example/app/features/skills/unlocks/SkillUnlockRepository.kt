package org.example.app.features.skills.unlocks

import org.example.app.core.skills.Skill

internal class SkillUnlockRepository(
    private val levelsBySkill: Map<Int, Set<Int>>,
) {

    val skillCount: Int
        get() =
            levelsBySkill.size

    val unlockLevelCount: Int
        get() =
            levelsBySkill.values
                .sumOf(Set<Int>::size)

    fun hasUnlocks(
        skill: Skill,
        firstLevel: Int,
        lastLevel: Int,
    ): Boolean {
        if (firstLevel > lastLevel) {
            return false
        }

        val levels =
            levelsBySkill[skill.id]
                ?: return false

        return levels.any {
            it in firstLevel..lastLevel
        }
    }
}