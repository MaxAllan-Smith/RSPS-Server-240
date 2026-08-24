package org.example.app.features.movement

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.state.movementState

/**
 * Shared player movement service.
 *
 * Gameplay features may request routes through this service instead of
 * manipulating player coordinates or movement queues directly.
 */
class MovementService internal constructor(
    private val planner: RoutePlanner,
) {

    /**
     * Replaces the player's current route with a route toward the supplied
     * absolute world coordinate.
     *
     * RSMod is configured to move-near, so destinations occupying blocked
     * tiles - such as trees and other world objects - resolve to the nearest
     * reachable tile instead of requiring the player to stand on the object.
     */
    fun request(
        player: Player,
        x: Int,
        z: Int,
        keyCombination: Int = 0,
    ): Boolean {
        val destination =
            WorldPosition(
                x = x,
                z = z,
                level = player.position.level,
            )

        val route =
            planner.route(
                start = player.position,
                destination = destination,
            )

        val state =
            player.movementState

        state.steps.clear()
        state.steps.addAll(route)
        state.requestedKeyCombination =
            keyCombination

        if (
            route.isEmpty() &&
            destination != player.position
        ) {
            println(
                "[Movement] '${player.username}' could not route " +
                    "to ${destination.x},${destination.z}," +
                    "${destination.level}."
            )

            return false
        }

        return true
    }

    /**
     * Advances the player by one server-authoritative walking tile.
     */
    fun cycle(
        player: Player,
    ) {
        val next =
            player.movementState
                .steps
                .removeFirstOrNull()
                ?: return

        player.position =
            next
    }

    /**
     * Cancels any currently queued route.
     */
    fun clear(
        player: Player,
    ) {
        player.movementState
            .steps
            .clear()
    }
}