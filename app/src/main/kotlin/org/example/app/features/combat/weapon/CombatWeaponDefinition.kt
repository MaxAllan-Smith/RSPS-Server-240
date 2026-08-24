package org.example.app.features.combat.weapon

import org.example.app.features.combat.model.CombatWeaponCategory

internal data class CombatWeaponDefinition(
    val itemId: Int,
    val category: CombatWeaponCategory,
)