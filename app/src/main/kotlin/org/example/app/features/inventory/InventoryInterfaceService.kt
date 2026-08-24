package org.example.app.features.inventory

import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player

internal class InventoryInterfaceService {

    private val initializedPlayers =
        mutableSetOf<Int>()

    fun initialize(
        player: Player,
    ) {
        if (!initializedPlayers.add(player.index)) {
            return
        }

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

        /**
         * Enable IfButton operations 1..10.
         *
         * We deliberately unlock all normal inventory item operations for
         * this discovery step so revision-240 can tell us exactly which op
         * corresponds to Wield.
         */
        const val INVENTORY_ITEM_OPTIONS: Int =
            0x3FF
    }
}