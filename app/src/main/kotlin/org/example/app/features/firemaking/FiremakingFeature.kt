package org.example.app.features.firemaking

import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.items.ItemOnItemDispatcher
import org.example.app.core.movement.MovementCoordinator
import org.example.app.core.world.GroundItemService
import org.example.app.core.world.WorldLocService
import kotlin.random.Random

/**
 * Standard line-Firemaking gameplay entry point.
 *
 * This class only wires packets and cycle hooks; [FiremakingService] owns
 * the ignition state machine, chance curves and fire-burnout lifecycle
 * described in its own KDoc.
 */
internal class FiremakingFeature(
    itemOnItem: ItemOnItemDispatcher,
    worldLocs: WorldLocService,
    groundItems: GroundItemService,
    movement: MovementCoordinator,
    experience: ExperienceService,
    config: FiremakingConfig,
    random: Random = Random.Default,
) : Feature {

    private val service =
        FiremakingService(
            itemOnItem = itemOnItem,
            worldLocs = worldLocs,
            groundItems = groundItems,
            movement = movement,
            experience = experience,
            config = config,
            random = random,
        )

    override val id: String = "firemaking"

    override fun install(registrar: FeatureRegistrar) {
        service.registerLogInteractions()

        /*
         * Runs before normal movement. When ignition succeeds, the one-tile
         * step-away route is installed here. MovementFeature runs at
         * priority 10, so the route advances in this SAME game cycle instead
         * of waiting another 600 ms.
         */
        registrar.onCycleStart(priority = BEFORE_MOVEMENT_PRIORITY) { context ->
            service.processActiveFires()
            service.processAttempts(context)
        }

        /*
         * Runs after movement. If the player manually walked away while an
         * ignition attempt was in progress, cancel the attempt and leave
         * their log on the ground.
         */
        registrar.onCycleStart(priority = AFTER_MOVEMENT_PRIORITY) { context ->
            service.cancelMovedAttempts(context)
        }
    }

    private companion object {
        /** Completes before MovementFeature (priority 10) so step-away can advance immediately. */
        const val BEFORE_MOVEMENT_PRIORITY: Int = 5

        /** Manual movement interruption is checked after MovementFeature. */
        const val AFTER_MOVEMENT_PRIORITY: Int = 20
    }
}
