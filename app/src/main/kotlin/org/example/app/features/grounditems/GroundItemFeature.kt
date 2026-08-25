package org.example.app.features.grounditems

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.objs.OpObjV2
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.movement.MovementCoordinator
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import org.example.app.core.world.GroundItemService

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
        MovementCoordinator,
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

        /*
         * Standard OSRS inventory-item drop sound.
         *
         * Play only after the authoritative inventory removal and ground-item
         * staging have both succeeded.
         */
        player.session.queue(
            SynthSound(
                id =
                    ITEM_DROP_SOUND_ID,

                loops =
                    1,

                delay =
                    0,
            )
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

        player.groundItemInteractionState
            .clear()

        if (
            player.position ==
            targetPosition
        ) {
            completePickup(
                player =
                    player,

                itemId =
                    packet.id,

                position =
                    targetPosition,
            )

            return
        }

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

        if (
            player.position !=
            pickup.position
        ) {
            return
        }

        player.groundItemInteractionState
            .clear()

        movement.clear(
            player =
                player
        )

        completePickup(
            player =
                player,

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

        /*
         * Standard OSRS item-pickup sound.
         */
        player.session.queue(
            SynthSound(
                id =
                    ITEM_PICKUP_SOUND_ID,

                loops =
                    1,

                delay =
                    0,
            )
        )

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

        const val INVENTORY_INTERFACE_ID: Int =
            149

        const val INVENTORY_ITEMS_COMPONENT: Int =
            0

        const val DROP_OPERATION: Int =
            7

        const val TAKE_OPERATION: Int =
            3

        /**
         * Standard OSRS inventory-item drop sound.
         */
        const val ITEM_DROP_SOUND_ID: Int =
            2739

        /**
         * Standard OSRS ground-item pickup sound.
         */
        const val ITEM_PICKUP_SOUND_ID: Int =
            2582

        const val GROUND_ITEM_LIFECYCLE_PRIORITY: Int =
            5

        const val GROUND_ITEM_INTERACTION_PRIORITY: Int =
            20

        const val GROUND_ITEM_SYNC_PRIORITY: Int =
            20
    }
}

private data class GroundItemPickup(
    val itemId: Int,
    val position: WorldPosition,
)

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