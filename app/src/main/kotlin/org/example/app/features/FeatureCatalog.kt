package org.example.app.features

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureDependencies
import org.example.app.features.chat.ChatFeature
import org.example.app.features.combat.CombatFeature
import org.example.app.features.interfaces.InterfaceFeature
import org.example.app.features.inventory.InventoryFeature
import org.example.app.features.login.LoginFeature
import org.example.app.features.movement.MovementFeature
import org.example.app.features.skills.SkillsFeature
import org.example.app.features.woodcutting.WoodcuttingFeature
import org.example.app.features.world.WorldBootstrapFeature

/**
 * Single composition list for all vertical game features.
 *
 * Core networking and the game engine remain independent of concrete
 * gameplay features.
 */
object FeatureCatalog {

    fun create(
        dependencies: FeatureDependencies,
    ): List<Feature> =
        listOf(
            LoginFeature(),
            WorldBootstrapFeature(),

            /*
             * Movement comes before interactive skills because future
             * object interactions will use the movement system to reach
             * their targets.
             */
            MovementFeature(
                collision =
                    dependencies.collision,
            ),

            SkillsFeature(),
            InventoryFeature(),

            WoodcuttingFeature(),

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