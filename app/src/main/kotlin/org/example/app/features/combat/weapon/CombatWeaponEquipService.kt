package org.example.app.features.combat.weapon

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.player.Player

internal class CombatWeaponEquipService(
    private val weaponRepository: CombatWeaponRepository =
        CombatWeaponRepository(),
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

        if (weaponRepository.find(item.id) == null) {
            println(
                "[Combat] '${player.username}' cannot wield " +
                    "unsupported weapon item=${item.id}."
            )

            return false
        }

        val previous =
            player.equipment.set(
                slot = EquipmentSlot.WEAPON,
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
    ): Boolean {
        val item =
            player.equipment[
                EquipmentSlot.WEAPON
            ]
                ?: return false

        if (!player.inventory.add(item)) {
            println(
                "[Combat] '${player.username}' cannot unequip " +
                    "item=${item.id}; inventory is full."
            )

            return false
        }

        player.equipment.clear(
            EquipmentSlot.WEAPON,
        )

        println(
            "[Combat] '${player.username}' unequipped " +
                "item=${item.id} to inventory."
        )

        return true
    }
}