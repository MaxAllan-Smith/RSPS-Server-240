package org.example.app.features.combat

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.features.combat.command.CombatCommandHandler
import org.example.app.features.combat.ui.CombatInterfaceService
import org.example.app.features.combat.weapon.CombatEquipmentService

internal class CombatFeature(
    itemDefinitions:
        ItemDefinitionRepository,
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
    }
}