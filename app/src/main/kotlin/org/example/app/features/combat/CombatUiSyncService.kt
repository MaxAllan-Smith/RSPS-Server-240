package org.example.app.features.combat

import org.example.app.core.inventory.InventoryUiSync
import org.example.app.core.player.Player
import org.example.app.features.combat.weapon.CombatEquipmentService

/**
 * Resynchronizes both the inventory and equipment UI immediately after a
 * combat-owned equip/unequip interaction changes either container.
 *
 * The inventory feature's own [org.example.app.core.feature.FeatureRegistrar.beforeInfoUpdate]
 * hook already resyncs on a revision-gated basis every game cycle, so this is
 * a same-tick UX improvement rather than the only path to correctness.
 * [inventorySync] is the narrow core contract that lets this class trigger
 * that resync without importing the inventory feature's internals.
 *
 * Shares [equipmentSyncService] with [CombatFeature]'s own per-cycle resync
 * rather than constructing a second instance, since both ultimately gate on
 * the same per-player synchronized-revision state.
 */
internal class CombatUiSyncService(
    private val equipmentSyncService: CombatEquipmentService,
    private val inventorySync: InventoryUiSync,
) {

    fun synchronize(player: Player) {
        inventorySync.synchronize(player)
        equipmentSyncService.synchronize(player)
    }
}
