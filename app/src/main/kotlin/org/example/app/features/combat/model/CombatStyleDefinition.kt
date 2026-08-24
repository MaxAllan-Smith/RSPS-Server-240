package org.example.app.features.combat.model

internal data class CombatStyleDefinition(
    val style: CombatStyle,
    val name: String,
    val stance: CombatStance,
    val attackType: CombatAttackType,
)
