package org.example.app.core.world

import org.rsmod.routefinder.collision.CollisionFlagMap

class WorldCollision {

    val flags =
        CollisionFlagMap()

    private var loaded: Boolean =
        false

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

    val isLoaded: Boolean
        get() =
            loaded
}