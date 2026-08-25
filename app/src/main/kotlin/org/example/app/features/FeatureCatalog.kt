package org.example.app.features

import org.example.app.core.experience.ExperienceConfig
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureDependencies
import org.example.app.features.chat.ChatFeature
import org.example.app.features.combat.CombatFeature
import org.example.app.features.grounditems.GroundItemConfig
import org.example.app.features.grounditems.GroundItemFeature
import org.example.app.features.grounditems.GroundItemService
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
 * Shared services are constructed once here and injected into the vertical
 * slices that require them.
 */
object FeatureCatalog {

    fun create(
        dependencies:
            FeatureDependencies,
    ): List<Feature> {

        /*
         * Shared collision-aware movement.
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
         * Shared transient dynamic-location state.
         */
        val worldLocs =
            WorldLocService()

        /*
         * Shared gameplay experience pipeline.
         *
         * 100 = canonical / 1x XP.
         */
        val experience =
            ExperienceService(
                config =
                    ExperienceConfig(
                        globalRatePercent =
                            100,
                    ),
            )

        /*
         * Shared transient ground-item repository.
         *
         * 100 cycles x 600ms = approximately 60 seconds.
         */
        val groundItems =
            GroundItemService(
                config =
                    GroundItemConfig(
                        despawnTicks =
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

            GroundItemFeature(
                groundItems =
                    groundItems,

                movement =
                    movement,
            ),

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