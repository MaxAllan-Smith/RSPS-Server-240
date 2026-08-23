package org.example.app.core.skills

data class SkillChange(
    val skill: Skill,
    val previousExperience: Int,
    val experience: Int,
    val previousLevel: Int,
    val level: Int,
) {
    val gainedExperience: Int
        get() = experience - previousExperience

    val gainedLevels: Int
        get() = level - previousLevel

    val levelledUp: Boolean
        get() = gainedLevels > 0
}