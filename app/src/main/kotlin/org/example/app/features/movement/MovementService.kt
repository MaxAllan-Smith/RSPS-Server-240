package org.example.app.features.movement

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.state.movementState

/** Owns player route requests and consumes one walking tile per game cycle. */
internal class MovementService(
    private val planner: RoutePlanner,
) {
    fun request(
        player: Player,
        x: Int,
        z: Int,
        keyCombination: Int,
    ) {
        val destination =
            WorldPosition(
                x = x,
                z = z,
                level = player.position.level,
            )

        val route = planner.route(player.position, destination)
        val state = player.movementState

        state.steps.clear()
        state.steps.addAll(route)
        state.requestedKeyCombination = keyCombination

        if (route.isEmpty() && destination != player.position) {
            println(
                "[Movement] '${player.username}' could not route " +
                    "to ${destination.x},${destination.z},${destination.level}."
            )
        }
    }

    fun cycle(player: Player) {
        val next = player.movementState.steps.removeFirstOrNull() ?: return
        player.position = next
    }

    fun clear(player: Player) {
        player.movementState.steps.clear()
    }
}
