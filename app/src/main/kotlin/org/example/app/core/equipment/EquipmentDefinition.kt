package org.example.app.core.equipment

/** The equipment-specific portion of an item definition: its slot and any skill requirements to wear it. */
data class EquipmentDefinition(
    val slot: EquipmentSlot,
    val skillRequirements: List<EquipmentSkillRequirement> =
        emptyList(),
)