package org.example.app.features.combat

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.InventoryUiSync
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.features.combat.command.CombatCommandHandler
import org.example.app.features.combat.ui.CombatEquipmentHandler
import org.example.app.features.combat.ui.CombatInterfaceService
import org.example.app.features.combat.ui.CombatInventoryHandler
import org.example.app.features.combat.ui.CombatOptionsHandler
import org.example.app.features.combat.weapon.CombatEquipmentService

/**
 * Combat vertical slice: attack style selection, weapon categorization and
 * weapon equip/unequip through the equipment and inventory interfaces.
 */
internal class CombatFeature(
    itemDefinitions: ItemDefinitionRepository,
    inventorySync: InventoryUiSync,
) : Feature {

    private val interfaceService =
        CombatInterfaceService()

    private val equipmentService =
        CombatEquipmentService(
            itemDefinitions =
                itemDefinitions,
            interfaceService =
                interfaceService,
        )

    private val commandHandler =
        CombatCommandHandler(
            interfaceService =
                interfaceService,
        )

    private val optionsHandler =
        CombatOptionsHandler()

    private val inventoryHandler =
        CombatInventoryHandler(
            itemDefinitions =
                itemDefinitions,
        )

    private val equipmentHandler =
        CombatEquipmentHandler(
            itemDefinitions =
                itemDefinitions,
        )

    private val uiSyncService =
        CombatUiSyncService(
            equipmentSyncService =
                equipmentService,
            inventorySync =
                inventorySync,
        )

    override val id: String =
        "combat"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.command(
            commandHandler::handle,
        )

        registrar.beforeInfoUpdate { _, player ->
            equipmentService.synchronize(
                player
            )
        }

        // Attack-style selection, weapon wielding and unequipping all arrive
        // on the shared If3Button game message; this feature owns every
        // combat-related component id on it.
        registrar.onInterfaceButton { player, packet ->
            optionsHandler.handle(
                player = player,
                packet = packet,
            )

            val inventoryChanged =
                inventoryHandler.handle(
                    player = player,
                    packet = packet,
                )

            val equipmentChanged =
                equipmentHandler.handle(
                    player = player,
                    packet = packet,
                )

            if (inventoryChanged || equipmentChanged) {
                uiSyncService.synchronize(player)
            }
        }
    }
}
