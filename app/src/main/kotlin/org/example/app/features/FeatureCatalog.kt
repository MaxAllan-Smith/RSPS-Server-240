package org.example.app.features

import org.example.app.core.experience.ExperienceConfig
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureDependencies
import org.example.app.features.chat.ChatFeature
import org.example.app.features.combat.CombatFeature
import org.example.app.features.firemaking.FiremakingConfig
import org.example.app.features.firemaking.FiremakingFeature
import org.example.app.features.grounditems.GroundItemConfig
import org.example.app.features.grounditems.GroundItemFeature
import org.example.app.features.grounditems.GroundItemService
import org.example.app.features.interfaces.InterfaceFeature
import org.example.app.features.inventory.InventoryFeature
import org.example.app.features.itemuse.ItemOnItemDispatcher
import org.example.app.features.itemuse.ItemUseFeature
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

        val experience =
            ExperienceService(
                config =
                    ExperienceConfig(
                        globalRatePercent =
                            100,
                    ),
            )

        /*
         * Ordinary drops AND Firemaking ashes inherit this global lifetime.
         */
        val groundItems =
            GroundItemService(
                config =
                    GroundItemConfig(
                        despawnTicks =
                            dependencies
                                .config
                                .groundItemDespawnTicks,
                    ),
            )

        val itemOnItem =
            ItemOnItemDispatcher()

        val firemakingConfig =
            FiremakingConfig(
                rollIntervalTicks =
                    dependencies
                        .config
                        .firemakingRollIntervalTicks,

                fireLifetimeMinTicks =
                    dependencies
                        .config
                        .fireLifetimeMinTicks,

                fireLifetimeMaxTicks =
                    dependencies
                        .config
                        .fireLifetimeMaxTicks,
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

            ItemUseFeature(
                itemOnItem =
                    itemOnItem,
            ),

            FiremakingFeature(
                itemOnItem =
                    itemOnItem,

                worldLocs =
                    worldLocs,

                groundItems =
                    groundItems,

                movement =
                    movement,

                experience =
                    experience,

                config =
                    firemakingConfig,
            ),

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