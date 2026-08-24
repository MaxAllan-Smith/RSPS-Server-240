package org.example.app.features.combat.ui

import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.state.combatState
import org.example.app.features.combat.weapon.CombatWeaponCategories

internal class CombatInterfaceService {

    fun setUnarmed(
        player: Player,
    ) {
        setWeaponCategory(
            player = player,
            category = CombatWeaponCategory.UNARMED,
        )
    }

    fun setWeaponCategory(
        player: Player,
        category: CombatWeaponCategory,
    ) {
        normalizeCombatStyle(
            player = player,
            category = category,
        )

        player.combatState.weaponCategory =
            category

        player.vars.setVarbit(
            id = COMBAT_WEAPON_CATEGORY_VARBIT,
            value = category.id,
        )
    }

    private fun normalizeCombatStyle(
        player: Player,
        category: CombatWeaponCategory,
    ) {
        val definition =
            CombatWeaponCategories.find(
                category,
            )
                ?: return

        val currentStyle =
            player.combatState.style

        if (definition.style(currentStyle) != null) {
            return
        }

        val fallback =
            definition.styles.firstOrNull()
                ?: return

        player.combatState.style =
            fallback.style

        player.vars.setVarp(
            id = COMBAT_MODE_VARP,
            value = fallback.style.id,
        )

        println(
            "[Combat] '${player.username}' normalized combat " +
                "style ${currentStyle.id} -> ${fallback.style.id} " +
                "for weapon category ${category.id}."
        )
    }

    private companion object {
        const val COMBAT_WEAPON_CATEGORY_VARBIT: Int =
            357

        const val COMBAT_MODE_VARP: Int =
            43
    }
}