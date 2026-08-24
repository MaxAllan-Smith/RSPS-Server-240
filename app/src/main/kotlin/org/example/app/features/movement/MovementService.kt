package org.example.app.features.movement

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.state.movementState

/**
 * Shared authoritative player movement service.
 *
 * Ground clicks and gameplay interactions both use the same player route
 * queue and RSMod collision map.
 */
class MovementService internal constructor(
    private val planner: RoutePlanner,
) {

    /**
     * Standard ground/minimap movement request.
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
                level =
                    player.position.level,
            )

        val route =
            planner.route(
                start =
                    player.position,
                destination =
                    destination,
            )

        val state =
            player.movementState

        state.steps.clear()
        state.steps.addAll(
            route
        )

        state.requestedKeyCombination =
            keyCombination

        if (
            route.isEmpty() &&
            destination !=
            player.position
        ) {
            println(
                "[Movement] '${player.username}' could not route " +
                    "to ${destination.x}," +
                    "${destination.z}," +
                    "${destination.level}."
            )

            return false
        }

        return true
    }

    /**
     * Routes a player to the nearest reachable tile around a location.
     *
     * The selected endpoint is returned to the caller so the gameplay
     * interaction can wait for that exact tile before starting.
     */
    fun requestNear(
        player: Player,
        x: Int,
        z: Int,
        maximumRadius: Int,
        keyCombination: Int = 0,
    ): WorldPosition? {
        val target =
            WorldPosition(
                x = x,
                z = z,
                level =
                    player.position.level,
            )

        val route =
            planner.routeNear(
                start =
                    player.position,
                target =
                    target,
                maximumRadius =
                    maximumRadius,
            )
                ?: run {
                    clear(
                        player = player,
                    )

                    println(
                        "[Movement] '${player.username}' could not route " +
                            "near ${target.x}," +
                            "${target.z}," +
                            "${target.level}."
                    )

                    return null
                }

        val state =
            player.movementState

        state.steps.clear()
        state.steps.addAll(
            route.steps
        )

        state.requestedKeyCombination =
            keyCombination

        return route.destination
    }

    /**
     * Advances the player by one authoritative walking tile.
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
     * Cancels the current queued route.
     */
    fun clear(
        player: Player,
    ) {
        player.movementState
            .steps
            .clear()
    }
}