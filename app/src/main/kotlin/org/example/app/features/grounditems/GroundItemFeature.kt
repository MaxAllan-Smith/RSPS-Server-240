package org.example.app.features.grounditems

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.objs.OpObjV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import org.example.app.features.movement.MovementService
import kotlin.math.abs
import kotlin.math.max

/**
 * Generic inventory-drop and ground-item interaction feature.
 *
 * Responsibilities:
 *
 * - inventory Drop;
 * - collision-aware movement toward ground items;
 * - ground-item Take;
 * - ground-item garbage collection;
 * - scene synchronization.
 */
internal class GroundItemFeature(
    private val groundItems:
        GroundItemService,

    private val movement:
        MovementService,
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
             * Observe inventory Drop globally without replacing that listener.
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
             * Ground-object interactions.
             */
            addListener<OpObjV2> { packet ->
                handleGroundItem(
                    player = this,
                    packet = packet,
                )
            }
        }

        /*
         * Ground-item expiry, pending spawns and pending deletions.
         */
        registrar.onCycleStart(
            priority =
                GROUND_ITEM_LIFECYCLE_PRIORITY,
        ) { context ->
            groundItems.cycle(
                context
            )
        }

        /*
         * Movement has already advanced the player by the time this later
         * gameplay pass runs.
         *
         * Complete pending ground-item interactions once their selected
         * approach tile has been reached.
         */
        registrar.onCycleStart(
            priority =
                GROUND_ITEM_INTERACTION_PRIORITY,
        ) { context ->
            for (
                player in
                context.players.snapshot()
            ) {
                if (
                    player.isDisconnected
                ) {
                    continue
                }

                processPendingPickup(
                    player
                )
            }
        }

        /*
         * Resynchronize existing ground objects after login or scene rebuild.
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
     * Ground action slot three corresponds to Take.
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

        val targetPosition =
            WorldPosition(
                x =
                    packet.x,

                z =
                    packet.z,

                level =
                    player.position.level,
            )

        /*
         * A new interaction replaces any previous pending ground-item
         * interaction.
         */
        player.groundItemInteractionState
            .clear()

        /*
         * If already adjacent, perform the authoritative pickup immediately.
         */
        if (
            isInPickupRange(
                player = player,
                position = targetPosition,
            )
        ) {
            completePickup(
                player = player,

                itemId =
                    packet.id,

                position =
                    targetPosition,
            )

            return
        }

        /*
         * Ask the shared RSMod-backed movement service for a reachable tile
         * immediately surrounding the ground object.
         *
         * MovementService.requestNear expects the destination as separate
         * absolute x/z coordinates.
         */
        val approachPosition =
            movement.requestNear(
                player =
                    player,

                x =
                    targetPosition.x,

                z =
                    targetPosition.z,

                maximumRadius =
                    GROUND_ITEM_APPROACH_RADIUS,
            )
                ?: run {
                    println(
                        "[GroundItems] '${player.username}' could not route to " +
                            "item=${packet.id} " +
                            "at ${targetPosition.x}," +
                            "${targetPosition.z}," +
                            "${targetPosition.level}."
                    )

                    return
                }

        player.groundItemInteractionState
            .pickup =
            GroundItemPickup(
                itemId =
                    packet.id,

                position =
                    targetPosition,

                approachPosition =
                    approachPosition,
            )

        println(
            "[GroundItems] '${player.username}' routing to " +
                "item=${packet.id} " +
                "at ${targetPosition.x}," +
                "${targetPosition.z}," +
                "${targetPosition.level}; " +
                "approach=${approachPosition.x}," +
                "${approachPosition.z}," +
                "${approachPosition.level}."
        )
    }

    /**
     * Completes a pending Take interaction once the movement destination has
     * been reached.
     */
    private fun processPendingPickup(
        player: Player,
    ) {
        val pickup =
            player.groundItemInteractionState
                .pickup
                ?: return

        /*
         * requestNear returns the exact destination installed into the player's
         * movement state.
         */
        if (
            player.position !=
            pickup.approachPosition
        ) {
            return
        }

        player.groundItemInteractionState
            .clear()

        /*
         * Clear residual route state before completing the action.
         */
        movement.clear(
            player = player
        )

        completePickup(
            player = player,

            itemId =
                pickup.itemId,

            position =
                pickup.position,
        )
    }

    /**
     * Final server-authoritative pickup operation.
     *
     * GroundItemService.take() performs the final existence check, so an item
     * which expired or was taken while the player was walking cannot be
     * duplicated.
     */
    private fun completePickup(
        player: Player,
        itemId: Int,
        position: WorldPosition,
    ) {
        /*
         * Pickup may only occur on the same tile or an immediately-adjacent
         * tile.
         */
        if (
            !isInPickupRange(
                player = player,
                position = position,
            )
        ) {
            println(
                "[GroundItems] '${player.username}' reached invalid pickup " +
                    "distance for item=$itemId " +
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

        /*
         * Final revalidation.
         *
         * The item may have disappeared while the player was moving.
         */
        val item =
            groundItems.take(
                itemId =
                    itemId,

                position =
                    position,
            )
                ?: run {
                    println(
                        "[GroundItems] '${player.username}' reached missing " +
                            "item=$itemId " +
                            "at ${position.x}," +
                            "${position.z}," +
                            "${position.level}."
                    )

                    return
                }

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
     * Same tile or one surrounding tile.
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
         * Revision-240 inventory Drop.
         */
        const val DROP_OPERATION: Int =
            7

        /**
         * Ground action slot three = Take.
         */
        const val TAKE_OPERATION: Int =
            3

        /**
         * Same tile or directly adjacent.
         */
        const val MAXIMUM_PICKUP_DISTANCE: Int =
            1

        /**
         * Only search the immediately-surrounding ring for a reachable
         * interaction tile.
         */
        const val GROUND_ITEM_APPROACH_RADIUS: Int =
            1

        /**
         * Temporary world-object lifecycle.
         */
        const val GROUND_ITEM_LIFECYCLE_PRIORITY: Int =
            5

        /**
         * Gameplay interaction completion after movement.
         */
        const val GROUND_ITEM_INTERACTION_PRIORITY: Int =
            20

        const val GROUND_ITEM_SYNC_PRIORITY: Int =
            20
    }
}

/**
 * One pending Take interaction.
 */
private data class GroundItemPickup(
    val itemId: Int,
    val position: WorldPosition,
    val approachPosition: WorldPosition,
)

/**
 * Transient per-player ground-item interaction state.
 */
private class GroundItemInteractionState {

    var pickup:
        GroundItemPickup? =
        null

    fun clear() {
        pickup =
            null
    }
}

private val Player.groundItemInteractionState:
    GroundItemInteractionState
    get() =
        featureState.getOrPut(
            GroundItemInteractionState::class,
            ::GroundItemInteractionState,
        )