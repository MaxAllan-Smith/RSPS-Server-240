package org.example.app.features.combat.ui

import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.state.combatState

internal class CombatInterfaceService {

    fun setWeaponCategory(
        player: Player,
        category: CombatWeaponCategory,
    ) {
        player.combatState.weaponCategory =
            category

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
