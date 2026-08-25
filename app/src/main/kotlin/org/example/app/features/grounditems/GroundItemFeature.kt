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

/**
 * Generic inventory-drop and ground-item interaction feature.
 *
 * Ground-item pickup differs from scenery interaction:
 *
 * - locs are normally interacted with from an adjacent approach tile;
 * - ground items must be picked up while standing on their exact tile.
 *
 * A Take interaction therefore routes directly onto the object's coordinate,
 * revalidates the object once that coordinate is reached, and only then moves
 * it into the player's inventory.
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
             * If3Button already has a specific listener elsewhere for
             * equipment interactions.
             *
             * Observe inventory Drop through the global packet listener.
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
             * Ground-object operations.
             */
            addListener<OpObjV2> { packet ->
                handleGroundItem(
                    player = this,
                    packet = packet,
                )
            }
        }

        /*
         * Temporary ground-item lifecycle:
         *
         * - pending spawns;
         * - pending removals;
         * - expiry / garbage collection.
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
         * Complete pending Take interactions after movement has advanced.
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
         * Replay live ground objects after login or scene rebuild.
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
         * Validate the client's claimed item against authoritative inventory
         * state before removing anything.
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
     * Handles ground action three: Take.
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
         * A newly-clicked ground item replaces the previous pending pickup.
         */
        player.groundItemInteractionState
            .clear()

        /*
         * If we are already standing directly on the ground item, there is no
         * need to calculate a movement route.
         */
        if (
            player.position ==
            targetPosition
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
         * Ground items differ from scenery.
         *
         * Do NOT use requestNear here.
         *
         * We want the ordinary exact-destination movement request so the
         * player's final coordinate must equal the ground item's coordinate.
         */
        val routeAccepted =
            movement.request(
                player =
                    player,

                x =
                    targetPosition.x,

                z =
                    targetPosition.z,
            )

        if (
            !routeAccepted
        ) {
            println(
                "[GroundItems] '${player.username}' could not route onto " +
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
            )

        println(
            "[GroundItems] '${player.username}' routing onto " +
                "item=${packet.id} " +
                "at ${targetPosition.x}," +
                "${targetPosition.z}," +
                "${targetPosition.level}."
        )
    }

    /**
     * Completes a pending Take only after the player physically occupies the
     * ground item's tile.
     */
    private fun processPendingPickup(
        player: Player,
    ) {
        val pickup =
            player.groundItemInteractionState
                .pickup
                ?: return

        /*
         * This is intentionally exact equality.
         *
         * Adjacent, diagonal, two tiles away, etc. are all insufficient.
         */
        if (
            player.position !=
            pickup.position
        ) {
            return
        }

        player.groundItemInteractionState
            .clear()

        /*
         * The exact destination has been reached, so clear any residual
         * movement state before completing the gameplay action.
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
     * Final authoritative pickup operation.
     */
    private fun completePickup(
        player: Player,
        itemId: Int,
        position: WorldPosition,
    ) {
        /*
         * Hard requirement:
         *
         * the player must physically occupy the exact ground-object tile.
         */
        if (
            player.position !=
            position
        ) {
            println(
                "[GroundItems] '${player.username}' rejected pickup because " +
                    "player=${player.position.x}," +
                    "${player.position.z}," +
                    "${player.position.level} " +
                    "item=${position.x}," +
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
         * Final existence revalidation.
         *
         * The ground item might have:
         *
         * - expired while the player was walking;
         * - been taken by another player;
         * - otherwise been removed.
         *
         * In all those cases take() returns null and nothing enters the
         * inventory.
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
         * Third ground action = Take.
         */
        const val TAKE_OPERATION: Int =
            3

        const val GROUND_ITEM_LIFECYCLE_PRIORITY: Int =
            5

        /*
         * Must run after movement processing.
         */
        const val GROUND_ITEM_INTERACTION_PRIORITY: Int =
            20

        const val GROUND_ITEM_SYNC_PRIORITY: Int =
            20
    }
}

/**
 * One pending exact-tile ground-item pickup.
 */
private data class GroundItemPickup(
    val itemId: Int,
    val position: WorldPosition,
)

/**
 * Per-player transient ground-item interaction state.
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