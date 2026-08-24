package org.example.app.features.combat.weapon

import net.rsprot.protocol.common.game.outgoing.inv.InventoryObject
import net.rsprot.protocol.game.outgoing.inv.UpdateInvFull
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.state.combatState
import org.example.app.features.combat.ui.CombatInterfaceService

internal class CombatEquipmentService(
    private val weaponRepository: CombatWeaponRepository,
    private val interfaceService: CombatInterfaceService,
) {

    private val synchronizedRevisions =
        mutableMapOf<Int, Int>()

    fun synchronize(
        player: Player,
    ) {
        val currentRevision =
            player.equipment.revision

        val previousRevision =
            synchronizedRevisions[player.index]

        if (
            previousRevision != null &&
            previousRevision == currentRevision
        ) {
            return
        }

        synchronizedRevisions[player.index] =
            currentRevision

        val equippedWeapon =
            player.equipment[
                EquipmentSlot.WEAPON
            ]

        synchronizeAppearance(
            player = player,
            itemId = equippedWeapon?.id,
        )

        synchronizeEquipmentInventory(
            player = player,
        )

        synchronizeCombatCategory(
            player = player,
            itemId = equippedWeapon?.id,
        )
    }

    private fun synchronizeAppearance(
        player: Player,
        itemId: Int?,
    ) {
        player.infos
            .playerInfo
            .avatar
            .extendedInfo
            .setWornObj(
                wearpos = EquipmentSlot.WEAPON.id,
                id = itemId ?: EMPTY_ITEM,
                wearpos2 = NO_SECONDARY_WEARPOS,
                wearpos3 = NO_TERTIARY_WEARPOS,
            )
    }

    private fun synchronizeEquipmentInventory(
        player: Player,
    ) {
        player.session.queue(
            UpdateInvFull(
                inventoryId = WORN_INVENTORY,
                capacity = EQUIPMENT_CAPACITY,
            ) { slot ->
                val equipmentSlot =
                    EquipmentSlot.entries
                        .firstOrNull {
                            it.id == slot
                        }

                val item =
                    equipmentSlot
                        ?.let {
                            player.equipment[it]
                        }

                if (item == null) {
                    InventoryObject.NULL
                } else {
                    InventoryObject(
                        id = item.id,
                        count = item.amount,
                    )
                }
            },
        )
    }

    private fun synchronizeCombatCategory(
        player: Player,
        itemId: Int?,
    ) {
        val definition =
            itemId
                ?.let {
                    weaponRepository.find(it)
                }

        val category =
            definition?.category
                ?: CombatWeaponCategory.UNARMED

        if (
            player.combatState.weaponCategory ==
                category
        ) {
            return
        }

        interfaceService.setWeaponCategory(
            player = player,
            category = category,
        )

        if (itemId == null) {
            println(
                "[Combat] '${player.username}' detected no " +
                    "equipped weapon; using unarmed."
            )

            return
        }

        if (definition == null) {
            println(
                "[Combat] '${player.username}' equipped " +
                    "unsupported weapon item=$itemId; " +
                    "using unarmed."
            )

            return
        }

        println(
            "[Combat] '${player.username}' equipped " +
                "weapon item=$itemId, " +
                "category=${definition.category.id}."
        )
    }

    private companion object {
        const val WORN_INVENTORY: Int = 94

        const val EQUIPMENT_CAPACITY: Int = 14

        const val EMPTY_ITEM: Int = -1

        const val NO_SECONDARY_WEARPOS: Int =
            -1

        const val NO_TERTIARY_WEARPOS: Int =
            -1
    }
}