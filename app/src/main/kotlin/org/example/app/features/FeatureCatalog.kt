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
import org.example.app.features.world.WorldLocService

/**
 * Application composition root for vertical gameplay features.
 *
 * Cross-feature runtime services are created once here and explicitly shared
 * by the features that consume them.
 */
object FeatureCatalog {

    fun create(
        dependencies:
            FeatureDependencies,
    ): List<Feature> {
        /*
         * One authoritative movement service means walking clicks and gameplay
         * interactions manipulate the same route queue.
         */
        val movement =
            MovementService(
                planner =
                    RoutePlanner(
                        collision =
                            dependencies.collision,
                    ),
            )

        /*
         * One world-loc runtime service means every feature sees the same
         * temporary world state.
         *
         * Mining, doors, Farming, etc. can reuse this later.
         */
        val worldLocs =
            WorldLocService()

        return listOf(
            LoginFeature(),

            WorldBootstrapFeature(
                worldLocs =
                    worldLocs,
            ),

            MovementFeature(
                movement =
                    movement,
            ),

            SkillsFeature(),

            InventoryFeature(),

            WoodcuttingFeature(
                movement =
                    movement,
                worldLocs =
                    worldLocs,
            ),

            CombatFeature(
                itemDefinitions =
                    dependencies
                        .itemDefinitions,
            ),

            InterfaceFeature(
                itemDefinitions =
                    dependencies
                        .itemDefinitions,
            ),

            ChatFeature(),
        )
    }
}