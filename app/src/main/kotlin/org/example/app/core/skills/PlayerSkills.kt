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

    fun addExperience(
        skill: Skill,
        amount: Int,
    ): SkillChange {
        require(amount >= 0) {
            "Experience amount cannot be negative."
        }

        return setExperience(
            skill = skill,
            experience = experience(skill) + amount,
        )
    }

    fun setExperience(
        skill: Skill,
        experience: Int,
    ): SkillChange {
        val previousExperience =
            this.experience(skill)

        val previousBaseLevel =
            baseLevel(skill)

        val previousCurrentLevel =
            currentLevel(skill)

        val cappedExperience =
            experience.coerceIn(
                minimumValue = 0,
                maximumValue = SkillExperience.MAX_EXPERIENCE,
            )

        val newBaseLevel =
            SkillExperience.levelForExperience(cappedExperience)

        this.experience[skill.id] =
            cappedExperience

        baseLevels[skill.id] =
            newBaseLevel

        if (previousCurrentLevel == previousBaseLevel) {
            currentLevels[skill.id] =
                newBaseLevel
        }

        return SkillChange(
            skill = skill,
            previousExperience = previousExperience,
            experience = cappedExperience,
            previousLevel = previousBaseLevel,
            level = newBaseLevel,
        )
    }
}