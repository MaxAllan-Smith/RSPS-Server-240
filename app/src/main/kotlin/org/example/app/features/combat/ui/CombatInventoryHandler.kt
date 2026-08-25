package org.example.app.features.combat.ui

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.player.Player
import org.example.app.features.combat.weapon.CombatWeaponEquipService

/** Handles wielding a weapon from the inventory interface. */
internal class CombatInventoryHandler(
    itemDefinitions:
        ItemDefinitionRepository,
    private val equipService:
        CombatWeaponEquipService =
        CombatWeaponEquipService(
            itemDefinitions
        ),
) {

    fun handle(
        player: Player,
        packet: If3Button,
    ): Boolean {
        if (
            packet.interfaceId != INVENTORY_INTERFACE ||
            packet.componentId != INVENTORY_COMPONENT ||
            packet.op != WIELD_OP
        ) {
            return false
        }

        if (
            packet.sub !in
                0 until PlayerInventory.CAPACITY
        ) {
            return false
        }

        return equipService.wield(
            player = player,
            inventorySlot = packet.sub,
            expectedItemId = packet.obj,
        )
    }

    private companion object {
        const val INVENTORY_INTERFACE: Int = 149
        const val INVENTORY_COMPONENT: Int = 0
        const val WIELD_OP: Int = 3
    }
}