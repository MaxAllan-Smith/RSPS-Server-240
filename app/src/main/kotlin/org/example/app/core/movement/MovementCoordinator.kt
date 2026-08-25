package org.example.app.core.movement

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Core-owned contract for requesting player movement.
 *
 * Any feature that needs to walk a player toward a tile or an interactable
 * object (woodcutting, firemaking, ground items, ...) depends on this
 * interface instead of the concrete movement feature. That keeps the
 * movement vertical slice free of inbound feature coupling while still
 * letting other features drive it through constructor injection.
 *
 * [org.example.app.features.movement.MovementService] is the only
 * implementation, wired in by [org.example.app.features.FeatureCatalog].
 */
interface MovementCoordinator {

    /** Routes the player directly toward ([x], [z]) on their current level. */
    fun request(player: Player, x: Int, z: Int, keyCombination: Int = 0): Boolean

    /**
     * Routes the player to the closest reachable tile within [maximumRadius]
     * of ([x], [z]).
     *
     * @return the tile the route actually ends on, or null when no reachable
     *   tile exists within range.
     */
    fun requestNear(player: Player, x: Int, z: Int, maximumRadius: Int, keyCombination: Int = 0): WorldPosition?

    /** Cancels the player's in-progress route, if any. */
    fun clear(player: Player)
}
