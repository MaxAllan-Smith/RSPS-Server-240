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
 * Generic inventory-drop and ground-item interaction feature.
 *
 * Responsibilities:
 *
 * - inventory Drop;
 * - ground-item Take;
 * - ground-item garbage collection;
 * - scene synchronization.
 *
 * Item-specific actions such as eating, wielding, lighting and fletching are
 * intentionally handled by their respective gameplay features.
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
             * If3Button already has a specific listener elsewhere for equipment
             * interactions.
             *
             * Global packet observation allows us to process Drop without
             * replacing that existing listener.
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
             * Ground object interactions.
             */
            addListener<OpObjV2> { packet ->
                handleGroundItem(
                    player = this,
                    packet = packet,
                )
            }
        }

        /*
         * Ground-item timers and pending world changes.
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
         * Resynchronize ground items after login or scene rebuild.
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
     * Handles inventory Drop.
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

        val serverItem =
            player.inventory[
                slot
            ]
                ?: return

        /*
         * Never trust the item id supplied by the client.
         */
        if (
            serverItem.id !=
            packet.obj
        ) {
            println(
                "[GroundItems] '${player.username}' rejected drop: " +
                    "slot=$slot, " +
                    "clientItem=${packet.obj}, " +
                    "serverItem=${serverItem.id}."
            )

            return
        }

        val removed =
            player.inventory.clear(
                slot
            )
                ?: return

        groundItems.drop(
            item =
                removed,

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
     * Handles the standard ground-item Take operation.
     *
     * OSRS item definitions use their third ground-action slot for Take, so
     * RSProt reports this as operation 3.
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

        /*
         * Full route-to-ground-item behavior will be added later.
         *
         * For this stage the player must already be on or adjacent to the
         * object.
         */
        if (
            !isInPickupRange(
                player = player,
                position = position,
            )
        ) {
            println(
                "[GroundItems] '${player.username}' ignored distant Take " +
                    "item=${packet.id} " +
                    "at ${position.x}," +
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
                ?: run {
                    println(
                        "[GroundItems] '${player.username}' attempted to take " +
                            "missing item=${packet.id} " +
                            "at ${position.x}," +
                            "${position.z}," +
                            "${position.level}."
                    )

                    return
                }

        /*
         * Because the inventory has already been checked and gameplay state is
         * serialized through the game thread, the add should succeed.
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

    /**
     * Temporary pickup-distance validation.
     */
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
         * inventory:items = 149:0
         */
        const val INVENTORY_INTERFACE_ID: Int =
            149

        const val INVENTORY_ITEMS_COMPONENT: Int =
            0

        /**
         * Current revision-240 inventory Drop operation observed from the
         * client.
         */
        const val DROP_OPERATION: Int =
            7

        /**
         * Ground-item Take occupies the third ground-action slot.
         */
        const val TAKE_OPERATION: Int =
            3

        /**
         * Same tile or one adjacent tile.
         */
        const val MAXIMUM_PICKUP_DISTANCE: Int =
            1

        const val GROUND_ITEM_CYCLE_PRIORITY: Int =
            5

        const val GROUND_ITEM_SYNC_PRIORITY: Int =
            20
    }
}