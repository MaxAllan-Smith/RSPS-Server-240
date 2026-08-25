package org.example.app.features.combat.style

import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatStyleDefinition
import org.example.app.features.combat.state.combatState
import org.example.app.features.combat.weapon.CombatWeaponCategories

/** Resolves a player's currently-selected [CombatStyleDefinition] from their weapon category and chosen style slot. */
internal object CombatStyleResolver {

    fun resolve(
        player: Player,
    ): CombatStyleDefinition? {
        val category =
            CombatWeaponCategories.find(
                player.combatState.weaponCategory,
            )

        return category?.style(
            player.combatState.style,
        )
    }
}