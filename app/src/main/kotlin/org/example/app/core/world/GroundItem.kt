package org.example.app.core.world

import org.example.app.core.items.ItemStack
import org.example.app.core.player.WorldPosition

/**
 * One server-authoritative item stack currently existing on the ground.
 *
 * Ground items are transient world state:
 *
 * - they are not persisted to SQLite;
 * - they disappear after their lifetime expires;
 * - picking them up removes them immediately;
 * - server restart naturally clears them.
 */
internal data class GroundItem(
    val uid: Long,
    val item: ItemStack,
    val position: WorldPosition,
    var ticksRemaining: Int,
) {

    init {
        require(uid > 0L) {
            "Ground-item uid must be positive."
        }

        require(ticksRemaining > 0) {
            "Ground-item lifetime must be positive."
        }
    }
}

/**
 * Runtime configuration for the generic ground-item system.
 *
 * One server game tick currently equals 600ms.
 */
data class GroundItemConfig(
    /**
     * Number of game ticks before an uncollected item is garbage-collected.
     *
     * 100 ticks × 600ms = 60 seconds.
     */
    val despawnTicks: Int = 100,
) {

    init {
        require(despawnTicks > 0) {
            "Ground-item despawn time must be positive."
        }
    }
}