package org.example.app.features.inventory

import net.rsprot.protocol.common.game.outgoing.inv.InventoryObject
import net.rsprot.protocol.game.outgoing.inv.UpdateInvFull
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player

internal class InventorySyncService {

    private val synchronizedRevisions =
        mutableMapOf<Int, Int>()

    fun synchronize(
        player: Player,
    ) {
        val currentRevision =
            player.inventory.revision

        val previousRevision =
            synchronizedRevisions[player.index]

        if (
            previousRevision != null &&
            previousRevision == currentRevision
        ) {
            return
        }

        synchronizedRevisions[player.index] =
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