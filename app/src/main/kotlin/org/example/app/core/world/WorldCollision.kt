package org.example.app.core.world

import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Server-wide collision/pathfinding capability.
 *
 * Collision data is loaded once during application bootstrap. Gameplay code
 * depends on this wrapper rather than directly constructing routefinder maps.
 */
class WorldCollision {
    val flags = CollisionFlagMap()

    val routeFinding by lazy {
        requireLoaded()
        RouteFinding(flags)
    }

    private var loaded: Boolean = false

    fun markLoaded() {
        check(!loaded) {
            "World collision has already been loaded."
        }
        loaded = true
    }

    fun requireLoaded() {
        check(loaded) {
            "World collision has not been loaded."
        }
    }

    /**
     * Guards bootstrap loaders against accidentally hydrating the shared
     * collision map more than once. Kept as a small compatibility helper so
     * stale loader files from an older source tree still compile during a
     * clean migration.
     */
    fun requireNotLoaded() {
        check(!loaded) {
            "World collision has already been loaded."
        }
    }

    val isLoaded: Boolean
        get() = loaded
}
