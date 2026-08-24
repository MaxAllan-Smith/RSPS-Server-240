package org.example.app.features.combat

internal data class CombatStyleDefinition(
    val style: CombatStyle,
    val name: String,
    val stance: CombatStance,
    val attackType: CombatAttackType,
)