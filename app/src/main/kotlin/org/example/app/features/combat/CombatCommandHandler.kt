package org.example.app.features.combat

import org.example.app.core.player.Player

internal class CombatCommandHandler(
    private val interfaceService: CombatInterfaceService,
) {

    fun handle(
        player: Player,
        command: String,
        arguments: List<String>,
    ): Boolean =
        when (command) {
            WEAPON_CATEGORY_COMMAND ->
                handleWeaponCategory(
                    player = player,
                    arguments = arguments,
                )

            COMBAT_STYLE_COMMAND ->
                handleCombatStyle(player)

            else ->
                false
        }

    private fun handleWeaponCategory(
        player: Player,
        arguments: List<String>,
    ): Boolean {
        val category =
            arguments.firstOrNull()
                ?.toIntOrNull()

        if (category == null || category < 0) {
            println(
                "[Combat] Usage: ::weaponcat <category>"
            )
            return true
        }

        interfaceService.setWeaponCategory(
            player = player,
            category =
                CombatWeaponCategory(category),
        )

        println(
            "[Combat] '${player.username}' set weapon category " +
                "to $category."
        )

        return true
    }

    private fun handleCombatStyle(
        player: Player,
    ): Boolean {
        val definition =
            CombatStyleResolver.resolve(player)

        if (definition == null) {
            println(
                "[Combat] '${player.username}' has no resolved " +
                    "combat style."
            )

            return true
        }

        println(
            "[Combat] '${player.username}' resolved style: " +
                "${definition.name}, " +
                "${definition.stance}, " +
                "${definition.attackType}."
        )

        return true
    }

    private companion object {
        const val WEAPON_CATEGORY_COMMAND: String =
            "weaponcat"

        const val COMBAT_STYLE_COMMAND: String =
            "combatstyle"
    }
}