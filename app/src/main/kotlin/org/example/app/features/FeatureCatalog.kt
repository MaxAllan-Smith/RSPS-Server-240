package org.example.app.features

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureDependencies
import org.example.app.features.chat.ChatFeature
import org.example.app.features.combat.CombatFeature
import org.example.app.features.interfaces.InterfaceFeature
import org.example.app.features.inventory.InventoryFeature
import org.example.app.features.login.LoginFeature
import org.example.app.features.movement.MovementFeature
import org.example.app.features.movement.MovementService
import org.example.app.features.movement.RoutePlanner
import org.example.app.features.skills.SkillsFeature
import org.example.app.features.woodcutting.WoodcuttingFeature
import org.example.app.features.world.WorldBootstrapFeature

/**
 * Single application composition point for vertical gameplay features.
 *
 * Shared cross-feature services are constructed here and explicitly
 * injected into the features that consume them.
 */
object FeatureCatalog {

    fun create(
        dependencies: FeatureDependencies,
    ): List<Feature> {
        /*
         * There must only be one authoritative movement service per
         * application runtime. Both direct movement packets and gameplay
         * interactions operate on the same route queue.
         */
        val movement =
            MovementService(
                planner =
                    RoutePlanner(
                        collision =
                            dependencies.collision,
                    ),
            )

        return listOf(
            LoginFeature(),

            WorldBootstrapFeature(),

            MovementFeature(
                movement =
                    movement,
            ),

            SkillsFeature(),

            InventoryFeature(),

            WoodcuttingFeature(
                movement =
                    movement,
            ),

            CombatFeature(
                itemDefinitions =
                    dependencies.itemDefinitions,
            ),

            InterfaceFeature(
                itemDefinitions =
                    dependencies.itemDefinitions,
            ),

            ChatFeature(),
        )
    }
}