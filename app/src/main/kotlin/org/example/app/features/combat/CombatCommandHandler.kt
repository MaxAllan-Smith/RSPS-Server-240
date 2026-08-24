package org.example.app.features.combat

import org.example.app.core.player.Player

internal class CombatCommandHandler(
    private val interfaceService: CombatInterfaceService,
) {

    fun handle(
        player: Player,
        command: String,
        arguments: List<String>,
    ): Boolean {
        if (command != "weaponcat") {
            return false
        }

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
}