package org.example.app.features.inventory

import net.rsprot.protocol.common.game.outgoing.inv.InventoryObject
import net.rsprot.protocol.game.outgoing.inv.UpdateInvFull
import org.example.app.core.inventory.InventoryUiSync
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.features.inventory.state.inventoryState

/**
 * Sends the full inventory contents to the client whenever its revision
 * changes, and implements the core-owned [InventoryUiSync] contract so other
 * features (combat's equip/unequip handlers) can request an immediate resync
 * without depending on this class directly.
 */
internal class InventorySyncService : InventoryUiSync {

    override fun synchronize(
        player: Player,
    ) {
        val currentRevision =
            player.inventory.revision

        if (
            player.inventoryState.synchronizedRevision ==
                currentRevision
        ) {
            return
        }

        player.inventoryState.synchronizedRevision =
            currentRevision

        player.session.queue(
            UpdateInvFull(
                inventoryId = INVENTORY_ID,
                capacity = PlayerInventory.CAPACITY,
            ) { slot ->
                val item =
                    player.inventory[slot]

                if (item == null) {
                    InventoryObject.NULL
                } else {
                    InventoryObject(
                        id = item.id,
                        count = item.amount,
                    )
                }
            },
        )
    }

    private companion object {
        const val INVENTORY_ID: Int = 93
    }
}