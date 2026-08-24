package org.example.app.features.combat.weapon

import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill

internal class CombatWeaponEquipService(
    private val itemDefinitions: ItemDefinitionRepository =
        CombatItemDefinitions.repository,
) {

    fun wield(
        player: Player,
        inventorySlot: Int,
        expectedItemId: Int,
    ): Boolean {
        val item =
            player.inventory[inventorySlot]
                ?: return false

        if (item.id != expectedItemId) {
            println(
                "[Combat] '${player.username}' rejected stale " +
                    "inventory interaction slot=$inventorySlot " +
                    "packetItem=$expectedItemId actualItem=${item.id}."
            )

            return false
        }

        val itemDefinition =
            itemDefinitions[item.id]

        if (
            itemDefinition == null ||
            itemDefinition.weapon == null
        ) {
            println(
                "[Combat] '${player.username}' cannot wield " +
                    "unsupported weapon item=${item.id}."
            )

            return false
        }

        val equipmentDefinition =
            itemDefinition.equipment
                ?: return false

        val unmetRequirement =
            equipmentDefinition
                .skillRequirements
                .firstOrNull { requirement ->
                    player.skills.baseLevel(
                        requirement.skill,
                    ) < requirement.level
                }

        if (unmetRequirement != null) {
            val playerLevel =
                player.skills.baseLevel(
                    unmetRequirement.skill,
                )

            println(
                "[Combat] '${player.username}' cannot wield " +
                    "item=${item.id}; requires " +
                    "${unmetRequirement.skill.displayName} " +
                    "${unmetRequirement.level}, " +
                    "player has $playerLevel."
            )

            return false
        }

        val previous =
            player.equipment.set(
                slot = equipmentDefinition.slot,
                item = item,
            )

        player.inventory.set(
            slot = inventorySlot,
            item = previous,
        )

        println(
            "[Combat] '${player.username}' wielded " +
                "item=${item.id} from inventory slot=$inventorySlot."
        )

        return true
    }

    fun unequip(
        player: Player,
        itemId: Int,
    ): Boolean {
        val itemDefinition =
            itemDefinitions[itemId]

        if (
            itemDefinition == null ||
            itemDefinition.weapon == null
        ) {
            return false
        }

        val equipmentDefinition =
            itemDefinition.equipment
                ?: return false

        val item =
            player.equipment[
                equipmentDefinition.slot
            ]
                ?: return false

        if (item.id != itemId) {
            return false
        }

        if (!player.inventory.add(item)) {
            println(
                "[Combat] '${player.username}' cannot unequip " +
                    "item=${item.id}; inventory is full."
            )

            return false
        }

        player.equipment.clear(
            equipmentDefinition.slot,
        )

        println(
            "[Combat] '${player.username}' unequipped " +
                "item=${item.id} to inventory."
        )

        return true
    }

    private val Skill.displayName: String
        get() =
            name
                .lowercase()
                .replaceFirstChar(Char::uppercase)
}