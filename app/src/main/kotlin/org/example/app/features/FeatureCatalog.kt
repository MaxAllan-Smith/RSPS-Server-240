package org.example.app.features

import org.example.app.core.experience.ExperienceConfig
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureDependencies
import org.example.app.features.chat.ChatFeature
import org.example.app.features.combat.CombatFeature
import org.example.app.features.firemaking.FiremakingConfig
import org.example.app.features.firemaking.FiremakingFeature
import org.example.app.features.grounditems.GroundItemFeature
import org.example.app.features.interfaces.InterfaceFeature
import org.example.app.features.inventory.InventoryFeature
import org.example.app.features.inventory.InventorySyncService
import org.example.app.features.itemuse.ItemUseFeature
import org.example.app.features.login.LoginFeature
import org.example.app.features.movement.MovementConfig
import org.example.app.features.movement.MovementFeature
import org.example.app.features.movement.MovementService
import org.example.app.features.npcs.NpcFeature
import org.example.app.features.npcs.NpcService
import org.example.app.features.skills.SkillsFeature
import org.example.app.features.woodcutting.WoodcuttingFeature
import org.example.app.features.world.WorldBootstrapFeature

/**
 * Application composition root for gameplay features.
 *
 * This is the one place concrete feature classes are allowed to be imported
 * together and wired up. All cross-cutting core services (route planning,
 * dynamic world locs, ground items, item-on-item dispatch) are already built
 * by [org.example.app.core.server.ServerApplication] and arrive here through
 * [FeatureDependencies] -- this catalog only wires the feature-owned services
 * that sit on top of them and constructs the [Feature] list.
 */
object FeatureCatalog {

    fun create(
        dependencies: FeatureDependencies,
    ): List<Feature> {

        val movementConfig =
            MovementConfig(
                runEnergyDrainPerRunningCycle =
                    dependencies
                        .config
                        .runEnergyDrainPerRunningCycle,

                runEnergyRestorePerIdleCycle =
                    dependencies
                        .config
                        .runEnergyRestorePerIdleCycle,
            )

        val movement =
            MovementService(
                planner =
                    dependencies.routePlanner,

                config =
                    movementConfig,
            )

        val npcs =
            NpcService(
                planner =
                    dependencies.routePlanner,
            )

        val experience =
            ExperienceService(
                config =
                    ExperienceConfig(
                        globalRatePercent =
                            100,
                    ),
            )

        val inventorySync =
            InventorySyncService()

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
                    dependencies.worldLocs,
            ),

            NpcFeature(
                npcs =
                    npcs,
            ),

            MovementFeature(
                movement =
                    movement,
            ),

            SkillsFeature(),

            InventoryFeature(
                syncService =
                    inventorySync,
            ),

            ItemUseFeature(
                itemOnItem =
                    dependencies.itemOnItem,
            ),

            FiremakingFeature(
                itemOnItem =
                    dependencies.itemOnItem,

                worldLocs =
                    dependencies.worldLocs,

                groundItems =
                    dependencies.groundItems,

                movement =
                    movement,

                experience =
                    experience,

                config =
                    firemakingConfig,
            ),

            GroundItemFeature(
                groundItems =
                    dependencies.groundItems,

                movement =
                    movement,
            ),

            WoodcuttingFeature(
                movement =
                    movement,

                worldLocs =
                    dependencies.worldLocs,

                experience =
                    experience,
            ),

            CombatFeature(
                itemDefinitions =
                    dependencies
                        .itemDefinitions,

                inventorySync =
                    inventorySync,
            ),

            InterfaceFeature(),

            ChatFeature(),
        )
    }
}
