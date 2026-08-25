package org.example.app.features.inventory

import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.features.inventory.state.inventoryState

/**
 * Initializes interaction permissions for the player's inventory interface.
 *
 * Revision 240 separates:
 *
 * - events1: generic widget use/target/drag capabilities;
 * - events2: IF_BUTTON operations 1..10.
 */
internal class InventoryInterfaceService {

    /**
     * Sends inventory interaction permissions once per player session.
     */
    fun initialize(
        player: Player,
    ) {
        if (
            player.inventoryState
                .interfaceInitialized
        ) {
            return
        }

        player.inventoryState
            .interfaceInitialized =
            true

        player.session.queue(
            IfSetEventsV2(
                interfaceId =
                    INVENTORY_INTERFACE,

                componentId =
                    INVENTORY_CONTAINER,

                start =
                    0,

                end =
                    PlayerInventory.CAPACITY -
                        1,

                /*
                 * Generic inventory interaction permissions.
                 *
                 * This permits:
                 *
                 * - Use item -> ground item;
                 * - Use item -> NPC;
                 * - Use item -> loc;
                 * - Use item -> player;
                 * - Use item -> inventory/widget item;
                 * - items to act as use targets;
                 * - click-hold dragging;
                 * - inventory slots to accept dragged items.
                 */
                events1 =
                    INVENTORY_GENERIC_EVENTS,

                /*
                 * Ordinary item operations 1..10.
                 */
                events2 =
                    INVENTORY_ITEM_OPTIONS,
            ),
        )

        println(
            "[Inventory] Enabled item options, Use targeting and dragging for " +
                "'${player.username}'."
        )
    }

    private companion object {

        /**
         * inventory:items
         */
        const val INVENTORY_INTERFACE: Int =
            149

        const val INVENTORY_CONTAINER: Int =
            0

        /*
         * Widget click-mask capabilities.
         *
         * These values correspond to the OSRS client widget configuration
         * flags used by the current inventory component.
         */

        const val USE_GROUND_ITEM: Int =
            0x00000800

        const val USE_NPC: Int =
            0x00001000

        const val USE_OBJECT: Int =
            0x00002000

        const val USE_PLAYER: Int =
            0x00004000

        const val USE_ITEM: Int =
            0x00008000

        const val USE_WIDGET: Int =
            0x00010000

        /**
         * Allows the widget item to be dragged.
         */
        const val DRAG: Int =
            0x00020000

        /**
         * Allows another draggable widget item to be dropped onto this slot.
         */
        const val DRAG_ON: Int =
            0x00100000

        /**
         * Allows the widget to act as the target of USE_WIDGET.
         */
        const val WIDGET_USE_TARGET: Int =
            0x00200000

        const val INVENTORY_GENERIC_EVENTS: Int =
            USE_GROUND_ITEM or
                USE_NPC or
                USE_OBJECT or
                USE_PLAYER or
                USE_ITEM or
                USE_WIDGET or
                DRAG or
                DRAG_ON or
                WIDGET_USE_TARGET

        /**
         * Enable ordinary IF_BUTTON operations 1..10.
         */
        const val INVENTORY_ITEM_OPTIONS: Int =
            0x3FF
    }
}