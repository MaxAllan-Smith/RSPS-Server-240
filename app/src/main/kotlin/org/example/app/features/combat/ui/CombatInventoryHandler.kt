package org.example.app.features.combat.ui

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.player.Player
import org.example.app.features.combat.weapon.CombatWeaponRepository

internal class CombatInventoryHandler(
    private val weaponRepository: CombatWeaponRepository =
        CombatWeaponRepository(),
) {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (
            packet.interfaceId != INVENTORY_INTERFACE ||
            packet.componentId != INVENTORY_COMPONENT ||
            packet.op != WIELD_OP
        ) {
            return
        }

        val slot =
            packet.sub

        if (slot !in INVENTORY_SLOT_RANGE) {
            return
        }

        val item =
            player.inventory[slot]
                ?: return

        if (item.id != packet.obj) {
            println(
                "[Combat] '${player.username}' rejected stale " +
                    "inventory interaction slot=$slot " +
                    "packetItem=${packet.obj} actualItem=${item.id}."
            )

            return
        }

        val weapon =
            weaponRepository.find(
                item.id,
            )

        if (weapon == null) {
            println(
                "[Combat] '${player.username}' cannot wield " +
                    "unsupported weapon item=${item.id}."
            )

            return
        }

        val previouslyEquipped =
            player.equipment.set(
                slot = EquipmentSlot.WEAPON,
                item = item,
            )

        player.inventory.set(
            slot = slot,
            item = previouslyEquipped,
        )

        println(
            "[Combat] '${player.username}' wielded " +
                "item=${item.id} from inventory slot=$slot."
        )
    }

    private companion object {
        const val INVENTORY_INTERFACE: Int = 149
        const val INVENTORY_COMPONENT: Int = 0

        const val WIELD_OP: Int = 3

        val INVENTORY_SLOT_RANGE: IntRange =
            0..27
    }
}