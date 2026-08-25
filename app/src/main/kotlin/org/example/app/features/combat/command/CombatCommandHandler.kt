package org.example.app.features.combat.command

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.style.CombatStyleResolver
import org.example.app.features.combat.ui.CombatInterfaceService

/** Development commands for forcing a weapon category/combat style, useful for testing without real equipment. */
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

            EQUIP_WEAPON_COMMAND ->
                handleEquipWeapon(
                    player = player,
                    arguments = arguments,
                )

            UNEQUIP_WEAPON_COMMAND ->
                handleUnequipWeapon(player)

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

    private fun handleEquipWeapon(
        player: Player,
        arguments: List<String>,
    ): Boolean {
        val itemId =
            arguments.firstOrNull()
                ?.toIntOrNull()

        if (itemId == null || itemId < 0) {
            println(
                "[Combat] Usage: ::equipweapon <itemId>"
            )
            return true
        }

        player.equipment.set(
            slot = EquipmentSlot.WEAPON,
            item =
                ItemStack(
                    id = itemId,
                ),
        )

        println(
            "[Combat] '${player.username}' equipped test " +
                "weapon item=$itemId."
        )

        return true
    }

    private fun handleUnequipWeapon(
        player: Player,
    ): Boolean {
        val removed =
            player.equipment.clear(
                EquipmentSlot.WEAPON,
            )

        if (removed == null) {
            println(
                "[Combat] '${player.username}' has no test " +
                    "weapon equipped."
            )

            return true
        }

        println(
            "[Combat] '${player.username}' unequipped test " +
                "weapon item=${removed.id}."
        )

        return true
    }

    private companion object {
        const val WEAPON_CATEGORY_COMMAND: String =
            "weaponcat"

        const val COMBAT_STYLE_COMMAND: String =
            "combatstyle"

        const val EQUIP_WEAPON_COMMAND: String =
            "equipweapon"

        const val UNEQUIP_WEAPON_COMMAND: String =
            "unequipweapon"
    }
}