package org.example.app.features.inventory

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class InventoryFeature : Feature {

    override val id: String =
        "inventory"

    private val syncService =
        InventorySyncService()

    private val interfaceService =
        InventoryInterfaceService()

    private val commandHandler =
        InventoryCommandHandler()

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.command(
            commandHandler::handle,
        )

        registrar.beforeInfoUpdate { _, player ->
            interfaceService.initialize(player)
            syncService.synchronize(player)
        }
    }
}