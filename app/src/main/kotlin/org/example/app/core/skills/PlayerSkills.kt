package org.example.app.core.skills

class PlayerSkills {

    private val currentLevels =
        IntArray(Skill.entries.size) { index ->
            Skill.entries[index].defaultLevel
        }

    private val baseLevels =
        IntArray(Skill.entries.size) { index ->
            Skill.entries[index].defaultLevel
        }

    private val experience =
        IntArray(Skill.entries.size) { index ->
            SkillExperience.forLevel(
                Skill.entries[index].defaultLevel,
            )
        }

    fun currentLevel(skill: Skill): Int =
        currentLevels[skill.id]

    fun baseLevel(skill: Skill): Int =
        baseLevels[skill.id]

    fun experience(skill: Skill): Int =
        experience[skill.id]
}