package org.example.app.features.combat

import org.example.app.core.player.Player

internal class CombatInterfaceService {

    fun setWeaponCategory(
        player: Player,
        category: CombatWeaponCategory,
    ) {
        player.vars.setVarbit(
            id = COMBAT_WEAPON_CATEGORY_VARBIT,
            value = category.id,
        )
    }

    fun setUnarmed(
        player: Player,
    ) {
        setWeaponCategory(
            player = player,
            category = CombatWeaponCategory.UNARMED,
        )
    }

    private companion object {
        const val COMBAT_WEAPON_CATEGORY_VARBIT: Int = 357
    }
}