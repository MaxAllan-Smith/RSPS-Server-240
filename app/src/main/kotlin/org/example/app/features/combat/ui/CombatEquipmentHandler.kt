package org.example.app.features.combat.ui

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.player.Player

internal class CombatEquipmentHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (
            packet.interfaceId != EQUIPMENT_INTERFACE ||
            packet.componentId != WEAPON_COMPONENT ||
            packet.op != REMOVE_OP
        ) {
            return
        }

        val equippedWeapon =
            player.equipment[
                EquipmentSlot.WEAPON
            ]
                ?: return

        val added =
            player.inventory.add(
                equippedWeapon,
            )

        if (!added) {
            println(
                "[Combat] '${player.username}' cannot unequip " +
                    "item=${equippedWeapon.id}; inventory is full."
            )

            return
        }

        player.equipment.clear(
            EquipmentSlot.WEAPON,
        )

        println(
            "[Combat] '${player.username}' unequipped " +
                "item=${equippedWeapon.id} to inventory."
        )
    }

    private companion object {
        const val EQUIPMENT_INTERFACE: Int =
            387

        const val WEAPON_COMPONENT: Int =
            18

        const val REMOVE_OP: Int =
            1
    }
}