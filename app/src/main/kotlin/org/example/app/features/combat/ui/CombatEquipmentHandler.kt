package org.example.app.features.combat.ui

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.player.Player
import org.example.app.features.combat.weapon.CombatWeaponEquipService

internal class CombatEquipmentHandler(
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
            packet.interfaceId != EQUIPMENT_INTERFACE ||
            packet.componentId != WEAPON_COMPONENT ||
            packet.op != REMOVE_OP
        ) {
            return false
        }

        val equippedWeapon =
            player.equipment[
                EquipmentSlot.WEAPON
            ]
                ?: return false

        return equipService.unequip(
            player = player,
            itemId = equippedWeapon.id,
        )
    }

    private companion object {
        const val EQUIPMENT_INTERFACE: Int = 387
        const val WEAPON_COMPONENT: Int = 18
        const val REMOVE_OP: Int = 1
    }
}