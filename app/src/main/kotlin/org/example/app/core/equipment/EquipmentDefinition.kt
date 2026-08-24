package org.example.app.core.equipment

data class EquipmentDefinition(
    val slot: EquipmentSlot,
    val skillRequirements: List<EquipmentSkillRequirement> =
        emptyList(),
)