package org.example.app.features.combat.weapon

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.features.combat.model.CombatWeaponCategory

internal data class CombatWeaponDefinition(
    val itemId: Int,
    val category: CombatWeaponCategory,
    val equipmentSlot: EquipmentSlot,
    val attackLevelRequirement: Int,
) {
    init {
        require(attackLevelRequirement >= 1) {
            "Attack level requirement must be positive."
        }
    }
}