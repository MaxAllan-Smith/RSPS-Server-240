package org.example.app.core.world.collision

import org.example.app.core.player.WorldPosition
import org.example.app.core.world.WorldCollision
import kotlin.math.abs

/**
 * Small adapter around RSMod's RuneScape-style BFS routefinder.
 *
 * Core movement routes directly to a tile, while interaction systems can
 * request the nearest reachable tile around a blocked world location.
 */
class RoutePlanner(
    private val collision: WorldCollision,
) {

    /**
     * Routes directly toward a world tile.
     *
     * Normal ground movement keeps move-near enabled to match client walking
     * behaviour when the exact requested destination cannot be occupied.
     */
    fun route(
        start: WorldPosition,
        destination: WorldPosition,
    ): List<WorldPosition> =
        route(
            start = start,
            destination = destination,
            moveNear = true,
        )

    /**
     * Finds a route to the closest usable interaction tile around [target].
     *
     * Locations may occupy more than their south-west packet coordinate.
     * Without loc dimensions in the precomputed collision dataset, routing
     * directly to that anchor is not reliable for larger objects.
     *
     * We therefore search outward one tile at a time and use the first radius
     * containing at least one reachable destination. Within that radius the
     * shortest route is preferred.
     */
    fun routeNear(
        start: WorldPosition,
        target: WorldPosition,
        maximumRadius: Int,
    ): InteractionRoute? {
        require(maximumRadius >= 1) {
            "Interaction radius must be at least one tile."
        }

        if (start.level != target.level) {
            return null
        }

        for (radius in 1..maximumRadius) {
            val candidates =
                interactionRing(
                    target = target,
                    radius = radius,
                )

            val routes =
                candidates.mapNotNull { destination ->
                    val steps =
                        route(
                            start = start,
                            destination = destination,
                            moveNear = false,
                        )

                    when {
                        destination == start ->
                            InteractionRoute(
                                destination = destination,
                                steps = emptyList(),
                            )

                        steps.isNotEmpty() ->
                            InteractionRoute(
                                destination = destination,
                                steps = steps,
                            )

                        else ->
                            null
                    }
                }

            val best =
                routes.minByOrNull {
                    it.steps.size
                }

            if (best != null) {
                return best
            }
        }

        return null
    }

    private fun route(
        start: WorldPosition,
        destination: WorldPosition,
        moveNear: Boolean,
    ): List<WorldPosition> {
        if (
            start.level !=
            destination.level
        ) {
            return emptyList()
        }

        if (start == destination) {
            return emptyList()
        }

        val route =
            collision.routeFinding.findRoute(
                level = start.level,
                srcX = start.x,
                srcZ = start.z,
                destX = destination.x,
                destZ = destination.z,
                moveNear = moveNear,
            )

        if (route.failed) {
            return emptyList()
        }

        return expandWaypoints(
            start = start,
            waypoints =
                route.waypoints.map {
                    WorldPosition(
                        x = it.x,
                        z = it.z,
                        level = it.level,
                    )
                },
        )
    }

    /**
     * Produces the perimeter of a square centered on the target.
     *
     * radius=1:
     *
     * XXX
     * XTX
     * XXX
     *
     * where T itself is intentionally excluded.
     */
    private fun interactionRing(
        target: WorldPosition,
        radius: Int,
    ): List<WorldPosition> =
        buildList {
            for (
                offsetX in
                -radius..radius
            ) {
                for (
                    offsetZ in
                    -radius..radius
                ) {
                    if (
                        maxOf(
                            abs(offsetX),
                            abs(offsetZ),
                        ) != radius
                    ) {
                        continue
                    }

                    add(
                        WorldPosition(
                            x =
                                target.x +
                                    offsetX,
                            z =
                                target.z +
                                    offsetZ,
                            level =
                                target.level,
                        )
                    )
                }
            }
        }

    private fun expandWaypoints(
        start: WorldPosition,
        waypoints: List<WorldPosition>,
    ): List<WorldPosition> {
        val steps =
            ArrayList<WorldPosition>()

        var current =
            start

        for (waypoint in waypoints) {
            while (
                current.x != waypoint.x ||
                current.z != waypoint.z
            ) {
                val dx =
                    (
                        waypoint.x -
                            current.x
                        ).coerceIn(
                        minimumValue = -1,
                        maximumValue = 1,
                    )

                val dz =
                    (
                        waypoint.z -
                            current.z
                        ).coerceIn(
                        minimumValue = -1,
                        maximumValue = 1,
                    )

                check(
                    abs(dx) <= 1 &&
                        abs(dz) <= 1
                ) {
                    "RSMod produced a non-unit movement segment."
                }

                current =
                    WorldPosition(
                        x =
                            current.x +
                                dx,
                        z =
                            current.z +
                                dz,
                        level =
                            current.level,
                    )

                steps +=
                    current
            }
        }

        return steps
    }
}

/**
 * Successful interaction route plus the exact tile selected as the
 * interaction endpoint.
 */
data class InteractionRoute(
    val destination: WorldPosition,
    val steps: List<WorldPosition>,
)