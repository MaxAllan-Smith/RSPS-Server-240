package org.example.app.features.combat

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.features.combat.command.CombatCommandHandler
import org.example.app.features.combat.ui.CombatInterfaceService
import org.example.app.features.combat.weapon.CombatEquipmentService
import org.example.app.features.combat.weapon.CombatWeaponRepository

internal class CombatFeature : Feature {

    private val interfaceService =
        CombatInterfaceService()

    private val weaponRepository =
        CombatWeaponRepository()

    private val equipmentService =
        CombatEquipmentService(
            weaponRepository = weaponRepository,
            interfaceService = interfaceService,
        )

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
            commandHandler::handle,
        )

        registrar.beforeInfoUpdate { _, player ->
            equipmentService.synchronize(player)
        }
    }
}