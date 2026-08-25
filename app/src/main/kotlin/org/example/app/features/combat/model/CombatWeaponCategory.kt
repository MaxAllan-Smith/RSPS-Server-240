package org.example.app.features.combat.model

/** Type-safe wrapper around the cache's numeric weapon category id. */
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

        val AXE =
            CombatWeaponCategory(1)

        val WHIP =
            CombatWeaponCategory(20)
    }
}