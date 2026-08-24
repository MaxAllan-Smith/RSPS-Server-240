package org.example.app.features.movement

import org.example.app.core.player.WorldPosition
import org.example.app.core.world.WorldCollision
import kotlin.math.abs

/** Small adapter around RSMod's RuneScape-style BFS routefinder. */
internal class RoutePlanner(
    private val collision: WorldCollision,
) {
    fun route(
        start: WorldPosition,
        destination: WorldPosition,
    ): List<WorldPosition> {
        if (start.level != destination.level) return emptyList()
        if (start == destination) return emptyList()

        val route =
            collision.routeFinding.findRoute(
                level = start.level,
                srcX = start.x,
                srcZ = start.z,
                destX = destination.x,
                destZ = destination.z,
                moveNear = true,
            )

        if (route.failed) return emptyList()

        return expandWaypoints(
            start = start,
            waypoints = route.waypoints.map {
                WorldPosition(it.x, it.z, it.level)
            },
        )
    }

    private fun expandWaypoints(
        start: WorldPosition,
        waypoints: List<WorldPosition>,
    ): List<WorldPosition> {
        val steps = ArrayList<WorldPosition>()
        var current = start

        for (waypoint in waypoints) {
            while (current.x != waypoint.x || current.z != waypoint.z) {
                val dx = (waypoint.x - current.x).coerceIn(-1, 1)
                val dz = (waypoint.z - current.z).coerceIn(-1, 1)

                // RSMod waypoints are straight/diagonal segments; this guard
                // catches malformed data rather than creating a non-unit step.
                check(abs(dx) <= 1 && abs(dz) <= 1)

                current =
                    WorldPosition(
                        x = current.x + dx,
                        z = current.z + dz,
                        level = current.level,
                    )
                steps += current
            }
        }

        return steps
    }
}
