package org.example.app.core.items

import org.example.app.core.equipment.EquipmentDefinition

data class ItemDefinition(
    val id: Int,
    val equipment: EquipmentDefinition? = null,
) {
    init {
        require(id >= 0) {
            "Item id must be non-negative."
        }
    }
}