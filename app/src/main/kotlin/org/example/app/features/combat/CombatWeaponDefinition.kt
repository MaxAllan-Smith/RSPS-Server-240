package org.example.app.features.combat

internal data class CombatWeaponDefinition(
    val itemId: Int,
    val category: CombatWeaponCategory,
) {
    init {
        require(itemId >= 0) {
            "Weapon item id must be non-negative."
        }
    }
}