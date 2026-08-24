package org.example.app.features.combat

import org.example.app.core.player.Player

internal class CombatInterfaceService {

    fun setWeaponCategory(
        player: Player,
        category: Int,
    ) {
        require(category >= 0) {
            "Combat weapon category must be non-negative."
        }

        player.vars.setVarbit(
            id = COMBAT_WEAPON_CATEGORY_VARBIT,
            value = category,
        )
    }

    fun setUnarmed(
        player: Player,
    ) {
        setWeaponCategory(
            player = player,
            category = UNARMED_CATEGORY,
        )
    }

    private companion object {
        const val COMBAT_WEAPON_CATEGORY_VARBIT: Int = 357
        const val UNARMED_CATEGORY: Int = 0
    }
}