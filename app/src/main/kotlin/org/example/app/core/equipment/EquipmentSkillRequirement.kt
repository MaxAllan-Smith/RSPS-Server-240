package org.example.app.core.equipment

import org.example.app.core.skills.Skill

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