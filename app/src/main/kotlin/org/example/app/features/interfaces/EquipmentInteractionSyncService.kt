package org.example.app.features.interfaces

import org.example.app.core.player.Player
import org.example.app.features.combat.ui.CombatInterfaceService
import org.example.app.features.combat.weapon.CombatEquipmentService
import org.example.app.features.combat.weapon.CombatWeaponRepository
import org.example.app.features.inventory.InventorySyncService

internal class EquipmentInteractionSyncService {

    private val inventorySyncService =
        InventorySyncService()

    private val equipmentSyncService =
        CombatEquipmentService(
            weaponRepository =
                CombatWeaponRepository(),
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