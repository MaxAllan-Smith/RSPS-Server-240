package org.example.app.features.grounditems

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.objs.OpObjV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import kotlin.math.abs
import kotlin.math.max

/**
 * Generic inventory-drop and ground-item lifecycle feature.
 *
 * This feature owns:
 *
 * - dropping inventory items;
 * - spawning them into the world;
 * - collecting nearby ground items;
 * - synchronizing existing items after scene changes;
 * - garbage-collecting expired floor items.
 *
 * Item-specific gameplay such as eating, equipping, lighting or fletching does
 * not belong here.
 */
internal class GroundItemFeature(
    private val groundItems:
        GroundItemService,
) : Feature {

    override val id: String =
        "ground-items"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            /*
             * If3Button already has a specific consumer for equipment.
             *
             * RSProt allows only one specific consumer per message class, so
             * inventory Drop is observed through a global consumer instead.
             */
            addGlobalListener { player, message ->
                if (
                    message is
                    If3Button
                ) {
                    handleInventoryButton(
                        player = player,
                        packet = message,
                    )
                }
            }

            /*
             * Floor item operation 1 = Take.
             */
            addListener<OpObjV2> { packet ->
                handleGroundItem(
                    player = this,
                    packet = packet,
                )
            }
        }

        /*
         * Central garbage collector and pending world updates.
         */
        registrar.onCycleStart(
            priority =
                GROUND_ITEM_CYCLE_PRIORITY,
        ) { context ->
            groundItems.cycle(
                context
            )
        }

        /*
         * WorldBootstrapFeature has already updated worldMapState by the time
         * this later-priority handler executes.
         */
        registrar.beforeInfoUpdate(
            priority =
                GROUND_ITEM_SYNC_PRIORITY,
        ) { _, player ->
            groundItems.synchronize(
                player
            )
        }
    }

    /**
     * Handles the inventory component's Drop operation.
     */
    private fun handleInventoryButton(
        player: Player,
        packet: If3Button,
    ) {
        if (
            packet.interfaceId !=
            INVENTORY_INTERFACE_ID
        ) {
            return
        }

        if (
            packet.componentId !=
            INVENTORY_ITEMS_COMPONENT
        ) {
            return
        }

        if (
            packet.op !=
            DROP_OPERATION
        ) {
            return
        }

        val slot =
            packet.sub

        if (
            slot !in
            0 until
                PlayerInventory.CAPACITY
        ) {
            return
        }

        val item =
            player.inventory[
                slot
            ]
                ?: return

        /*
         * Never trust the client's claimed object id.
         */
        if (
            item.id !=
            packet.obj
        ) {
            println(
                "[GroundItems] '${player.username}' rejected drop: " +
                    "slot=$slot, " +
                    "clientItem=${packet.obj}, " +
                    "serverItem=${item.id}."
            )

            return
        }

        val removed =
            player.inventory.clear(
                slot
            )
                ?: return

        /*
         * Dropped items appear underneath the player's current world tile.
         */
        groundItems.drop(
            item = removed,
            position =
                player.position,
        )

        println(
            "[GroundItems] '${player.username}' dropped " +
                "item=${removed.id} " +
                "amount=${removed.amount} " +
                "at ${player.position.x}," +
                "${player.position.z}," +
                "${player.position.level}."
        )
    }

    /**
     * Collects a nearby ground item.
     *
     * Movement-to-item routing is intentionally separate. For this first
     * implementation the player must already be standing on or adjacent to the
     * item before Take succeeds.
     */
    private fun handleGroundItem(
        player: Player,
        packet: OpObjV2,
    ) {
        if (
            packet.op !=
            TAKE_OPERATION
        ) {
            return
        }

        val position =
            WorldPosition(
                x =
                    packet.x,

                z =
                    packet.z,

                level =
                    player.position.level,
            )

        if (
            !isInPickupRange(
                player = player,
                position = position,
            )
        ) {
            println(
                "[GroundItems] '${player.username}' ignored distant Take " +
                    "item=${packet.id} at " +
                    "${position.x}," +
                    "${position.z}," +
                    "${position.level}."
            )

            return
        }

        if (
            !player.inventory
                .hasFreeSlot()
        ) {
            player.sendGameMessage(
                "You don't have enough inventory space."
            )

            return
        }

        val item =
            groundItems.take(
                itemId =
                    packet.id,

                position =
                    position,
            )
                ?: return

        /*
         * The free-slot check above and the single game communication thread
         * make this mutation authoritative.
         */
        check(
            player.inventory.add(
                item
            )
        ) {
            "Inventory capacity changed unexpectedly during ground-item pickup."
        }

        println(
            "[GroundItems] '${player.username}' picked up " +
                "item=${item.id} " +
                "amount=${item.amount} " +
                "at ${position.x}," +
                "${position.z}," +
                "${position.level}."
        )
    }

    private fun isInPickupRange(
        player: Player,
        position: WorldPosition,
    ): Boolean {
        if (
            player.position.level !=
            position.level
        ) {
            return false
        }

        val deltaX =
            abs(
                player.position.x -
                    position.x
            )

        val deltaZ =
            abs(
                player.position.z -
                    position.z
            )

        return max(
            deltaX,
            deltaZ,
        ) <=
            MAXIMUM_PICKUP_DISTANCE
    }

    private companion object {

        /**
         * Revision-240 inventory component currently mounted by this server:
         *
         * inventory:items = 149:0
         */
        const val INVENTORY_INTERFACE_ID: Int =
            149

        const val INVENTORY_ITEMS_COMPONENT: Int =
            0

        /**
         * Current client mapping observed for Drop.
         */
        const val DROP_OPERATION: Int =
            7

        /**
         * First floor-item operation = Take.
         */
        const val TAKE_OPERATION: Int =
            1

        /**
         * Same tile or one surrounding tile.
         */
        const val MAXIMUM_PICKUP_DISTANCE: Int =
            1

        /*
         * Run after general world-state timers.
         */
        const val GROUND_ITEM_CYCLE_PRIORITY: Int =
            5

        /*
         * World bootstrap currently runs before this during before-info work.
         */
        const val GROUND_ITEM_SYNC_PRIORITY: Int =
            20
    }
}