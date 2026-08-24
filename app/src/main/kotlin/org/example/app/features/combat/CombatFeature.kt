package org.example.app.features.combat

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.features.combat.command.CombatCommandHandler
import org.example.app.features.combat.ui.CombatInterfaceService

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
    }
}
