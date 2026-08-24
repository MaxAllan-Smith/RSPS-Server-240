package org.example.app.features.combat

@JvmInline
internal value class CombatWeaponCategory(
    val id: Int,
) {
    init {
        require(id >= 0) {
            "Combat weapon category must be non-negative."
        }
    }

    companion object {
        val UNARMED =
            CombatWeaponCategory(0)
    }
}