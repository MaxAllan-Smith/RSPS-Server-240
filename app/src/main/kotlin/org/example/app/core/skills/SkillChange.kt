package org.example.app.core.skills

/** One skill's experience/level delta produced by an experience award, used to drive level-up detection and client sync. */
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