package org.example.app.core.inventory

import org.example.app.core.player.Player

/**
 * Core-owned contract for requesting an immediate inventory UI resync.
 *
 * Combat's equipment and inventory packet handlers mutate inventory contents
 * directly (equipping/unequipping moves items between containers) and need
 * to tell the inventory feature its displayed state is stale, without
 * importing that feature's internals. The inventory feature's own
 * per-cycle [org.example.app.core.feature.FeatureRegistrar.beforeInfoUpdate]
 * hook is a revision-gated safety net regardless; implementing this contract
 * only lets combat request that resync a tick early, right after the click
 * that caused it.
 *
 * [org.example.app.features.inventory.InventorySyncService] is the only
 * implementation.
 */
fun interface InventoryUiSync {
    fun synchronize(player: Player)
}
