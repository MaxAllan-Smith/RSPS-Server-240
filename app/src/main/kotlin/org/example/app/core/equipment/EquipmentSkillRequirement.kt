package org.example.app.core.equipment

import org.example.app.core.skills.Skill

/** One skill-level requirement that must be met to equip an item. */
data class EquipmentSkillRequirement(
    val skill: Skill,
    val level: Int,
) {
    init {
        require(level >= 1) {
            "Equipment skill requirement must be positive."
        }
    }
}