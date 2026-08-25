package org.example.app.features.combat.model

/** One named attack style: its stance, attack type and the [CombatStyle] slot it occupies. */
internal data class CombatStyleDefinition(
    val style: CombatStyle,
    val name: String,
    val stance: CombatStance,
    val attackType: CombatAttackType,
)
