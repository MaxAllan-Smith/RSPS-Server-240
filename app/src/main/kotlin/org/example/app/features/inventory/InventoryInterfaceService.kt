package org.example.app.features.inventory

import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.features.inventory.state.inventoryState

/**
 * Initializes the interaction permissions for the player's inventory
 * interface.
 *
 * Revision 240 separates interface interaction permissions into:
 *
 * - events1: generic widget targeting/drag/use capabilities;
 * - events2: IF_BUTTON operations 1..10.
 *
 * The previous implementation only populated events2. That allowed options
 * such as Drop/Wield to transmit, but prevented the client from presenting the
 * generic "Use" action.
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
                 * Generic inventory "Use" targeting permissions.
                 *
                 * These allow an inventory item to be selected with Use and
                 * subsequently targeted at:
                 *
                 * - a ground item;
                 * - an NPC;
                 * - a world object/loc;
                 * - another player;
                 * - another inventory/widget item.
                 *
                 * WIDGET_USE_TARGET also makes inventory slots themselves
                 * valid targets for another selected inventory item.
                 */
                events1 =
                    INVENTORY_USE_EVENTS,

                /*
                 * Ordinary inventory options.
                 *
                 * Bits 0..9 correspond to button operations 1..10 in
                 * revision 240's events2 field.
                 */
                events2 =
                    INVENTORY_ITEM_OPTIONS,
            ),
        )

        println(
            "[Inventory] Enabled item options and Use targeting for " +
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

        /**
         * Revision-240 generic widget interaction mask.
         *
         * Values correspond to the client's widget click-mask flags:
         *
         * 0x00000800 = use on ground item
         * 0x00001000 = use on NPC
         * 0x00002000 = use on loc/object
         * 0x00004000 = use on player
         * 0x00008000 = use on inventory item
         * 0x00010000 = use widget
         * 0x00200000 = widget can be a use target
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

        const val WIDGET_USE_TARGET: Int =
            0x00200000

        const val INVENTORY_USE_EVENTS: Int =
            USE_GROUND_ITEM or
                USE_NPC or
                USE_OBJECT or
                USE_PLAYER or
                USE_ITEM or
                USE_WIDGET or
                WIDGET_USE_TARGET

        /**
         * Enable all ten normal inventory IF_BUTTON operations.
         */
        const val INVENTORY_ITEM_OPTIONS: Int =
            0x3FF
    }
}