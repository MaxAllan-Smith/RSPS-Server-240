package org.example.app.features.inventory

import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.features.inventory.state.inventoryState

internal class InventoryInterfaceService {

    fun initialize(
        player: Player,
    ) {
        if (player.inventoryState.interfaceInitialized) {
            return
        }

        player.inventoryState.interfaceInitialized =
            true

        player.session.queue(
            IfSetEventsV2(
                interfaceId = INVENTORY_INTERFACE,
                componentId = INVENTORY_CONTAINER,
                start = 0,
                end = PlayerInventory.CAPACITY - 1,
                events1 = 0,
                events2 = INVENTORY_ITEM_OPTIONS,
            ),
        )

        println(
            "[Inventory] Enabled item options for '${player.username}'."
        )
    }

    private companion object {
        const val INVENTORY_INTERFACE: Int = 149
        const val INVENTORY_CONTAINER: Int = 0

        const val INVENTORY_ITEM_OPTIONS: Int =
            0x3FF
    }
}