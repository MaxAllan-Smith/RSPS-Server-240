package org.example.app.features.combat

import org.example.app.core.player.Player

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