package org.example.app.features.combat

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class CombatFeature : Feature {

    private val interfaceService =
        CombatInterfaceService()

    private val commandHandler =
        CombatCommandHandler(
            interfaceService = interfaceService,
        )

    override val id: String =
        "combat"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.command(
            commandHandler::handle
        )

        registrar.beforeInfoUpdate(
            priority = COMBAT_PRIORITY,
        ) { _, player ->
            val state =
                player.combatState

            if (!state.initialized) {
                interfaceService.setUnarmed(player)

                state.initialized = true
            }
        }
    }

    private companion object {
        const val COMBAT_PRIORITY: Int = 80
    }
}