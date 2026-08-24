package org.example.app.features.interfaces

import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.player.Player
import org.example.app.features.combat.ui.CombatInterfaceService
import org.example.app.features.combat.weapon.CombatEquipmentService
import org.example.app.features.inventory.InventorySyncService

internal class EquipmentInteractionSyncService(
    itemDefinitions:
        ItemDefinitionRepository,
) {

    private val inventorySyncService =
        InventorySyncService()

    private val equipmentSyncService =
        CombatEquipmentService(
            itemDefinitions =
                itemDefinitions,
            interfaceService =
                CombatInterfaceService(),
        )

    fun synchronize(
        player: Player,
    ) {
        inventorySyncService.synchronize(
            player,
        )

        equipmentSyncService.synchronize(
            player,
        )
    }
}