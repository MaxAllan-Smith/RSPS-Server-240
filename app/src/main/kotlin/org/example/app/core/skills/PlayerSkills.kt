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
    ) {
        require(amount >= 0) {
            "Experience amount cannot be negative."
        }

        setExperience(
            skill = skill,
            experience = experience(skill) + amount,
        )
    }

    fun setExperience(
        skill: Skill,
        experience: Int,
    ) {
        val cappedExperience =
            experience.coerceIn(
                minimumValue = 0,
                maximumValue = SkillExperience.MAX_EXPERIENCE,
            )

        val previousBaseLevel =
            baseLevel(skill)

        val newBaseLevel =
            SkillExperience.levelForExperience(cappedExperience)

        this.experience[skill.id] =
            cappedExperience

        baseLevels[skill.id] =
            newBaseLevel

        if (currentLevel(skill) == previousBaseLevel) {
            currentLevels[skill.id] =
                newBaseLevel
        }
    }
}