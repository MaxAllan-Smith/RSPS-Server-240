package org.example.app.features

import org.example.app.core.experience.ExperienceConfig
import org.example.app.core.experience.ExperienceService
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
 * Application composition root for gameplay features.
 *
 * Cross-feature services are created once here and explicitly shared with the
 * vertical slices that consume them.
 */
object FeatureCatalog {

    fun create(
        dependencies:
            FeatureDependencies,
    ): List<Feature> {
        val movement =
            MovementService(
                planner =
                    RoutePlanner(
                        collision =
                            dependencies.collision,
                    ),
            )

        val worldLocs =
            WorldLocService()

        /*
         * Global gameplay XP configuration.
         *
         * Change this one value later, or replace it with a DB/config-backed
         * setting, to modify gameplay XP across every feature using this
         * service.
         */
        val experience =
            ExperienceService(
                config =
                    ExperienceConfig(
                        globalRatePercent =
                            100,
                    ),
            )

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

                experience =
                    experience,
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