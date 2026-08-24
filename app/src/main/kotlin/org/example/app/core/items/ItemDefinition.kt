package org.example.app.core.items

import org.example.app.core.equipment.EquipmentDefinition
import org.example.app.core.equipment.WeaponDefinition

data class ItemDefinition(
    val id: Int,
    val equipment: EquipmentDefinition? = null,
    val weapon: WeaponDefinition? = null,
) {
    init {
        require(id >= 0) {
            "Item id must be non-negative."
        }

        require(
            weapon == null ||
                equipment != null
        ) {
            "Weapon item $id must have equipment metadata."
        }
    }
}