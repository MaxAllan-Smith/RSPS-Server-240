package org.example.app.features.combat.style

import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatStyleDefinition
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.model.CombatWeaponCategoryDefinition
import org.example.app.features.combat.state.combatState
import org.example.app.features.combat.weapon.CombatWeaponCategories

internal object CombatStyleResolver {

    fun resolve(
        player: Player,
    ): CombatStyleDefinition? {
        val category =
            resolveCategory(
                player.combatState.weaponCategory,
            )

        return category?.style(
            player.combatState.style,
        )
    }

    private fun resolveCategory(
        category: CombatWeaponCategory,
    ): CombatWeaponCategoryDefinition? =
        when (category) {
            CombatWeaponCategory.UNARMED ->
                CombatWeaponCategories.UNARMED

            CombatWeaponCategory.AXE ->
                CombatWeaponCategories.AXE

            else ->
                null
        }
}
