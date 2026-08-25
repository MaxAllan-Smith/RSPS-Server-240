package org.example.app.features.inventory

import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player

/**
 * Generic player-inventory feature.
 *
 * Owns:
 *
 * - inventory interface permissions;
 * - inventory synchronization;
 * - development inventory commands;
 * - inventory slot rearrangement.
 *
 * Gameplay-specific item actions belong to their respective features rather
 * than being implemented here.
 */
internal class InventoryFeature :
    Feature {

    override val id: String =
        "inventory"

    private val syncService =
        InventorySyncService()

    private val interfaceService =
        InventoryInterfaceService()

    private val commandHandler =
        InventoryCommandHandler()

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.command(
            commandHandler::handle,
        )

        registrar.packets {

            /*
             * Revision-240 inventory drag packet.
             */
            addListener<IfButtonD> { packet ->
                handleDrag(
                    player = this,
                    packet = packet,
                )
            }
        }

        registrar.beforeInfoUpdate { _, player ->
            interfaceService.initialize(
                player
            )

            syncService.synchronize(
                player
            )
        }
    }

    /**
     * Handles click-hold-drag inventory rearrangement.
     *
     * Both source and destination are validated against the player's
     * authoritative inventory before anything is mutated.
     */
    private fun handleDrag(
        player: Player,
        packet: IfButtonD,
    ) {
        /*
         * We currently support ordinary inventory -> ordinary inventory
         * dragging only.
         *
         * Future container systems such as bank/equipment can implement their
         * own explicit transfer semantics.
         */
        if (
            packet.selectedInterfaceId !=
            INVENTORY_INTERFACE_ID ||
            packet.selectedComponentId !=
            INVENTORY_COMPONENT_ID ||
            packet.targetInterfaceId !=
            INVENTORY_INTERFACE_ID ||
            packet.targetComponentId !=
            INVENTORY_COMPONENT_ID
        ) {
            return
        }

        val sourceSlot =
            packet.selectedSub

        val targetSlot =
            packet.targetSub

        if (
            sourceSlot !in
            0 until
                PlayerInventory.CAPACITY ||
            targetSlot !in
            0 until
                PlayerInventory.CAPACITY
        ) {
            println(
                "[Inventory] '${player.username}' rejected drag with " +
                    "invalid slots source=$sourceSlot target=$targetSlot."
            )

            return
        }

        if (
            sourceSlot ==
            targetSlot
        ) {
            return
        }

        val sourceItem =
            player.inventory[
                sourceSlot
            ]
                ?: run {
                    println(
                        "[Inventory] '${player.username}' rejected drag: " +
                            "source slot $sourceSlot is empty."
                    )

                    return
                }

        /*
         * Never trust the source object id supplied by the client.
         */
        if (
            sourceItem.id !=
            packet.selectedObj
        ) {
            println(
                "[Inventory] '${player.username}' rejected drag: " +
                    "source slot=$sourceSlot, " +
                    "clientItem=${packet.selectedObj}, " +
                    "serverItem=${sourceItem.id}."
            )

            return
        }

        val targetItem =
            player.inventory[
                targetSlot
            ]

        /*
         * If the destination was occupied when the client performed the drag,
         * validate that object as well.
         *
         * For an empty target IfButtonD exposes targetObj = -1.
         */
        if (
            targetItem == null
        ) {
            if (
                packet.targetObj !=
                EMPTY_ITEM_ID
            ) {
                println(
                    "[Inventory] '${player.username}' rejected drag: " +
                        "target slot=$targetSlot is empty server-side, " +
                        "but clientTarget=${packet.targetObj}."
                )

                return
            }
        } else if (
            targetItem.id !=
            packet.targetObj
        ) {
            println(
                "[Inventory] '${player.username}' rejected drag: " +
                    "target slot=$targetSlot, " +
                    "clientItem=${packet.targetObj}, " +
                    "serverItem=${targetItem.id}."
            )

            return
        }

        if (
            !player.inventory.swap(
                firstSlot =
                    sourceSlot,

                secondSlot =
                    targetSlot,
            )
        ) {
            return
        }

        println(
            "[Inventory] '${player.username}' moved " +
                "item=${sourceItem.id} " +
                "slot=$sourceSlot -> $targetSlot" +
                (
                    if (
                        targetItem == null
                    ) {
                        "."
                    } else {
                        "; swapped with item=${targetItem.id}."
                    }
                    )
        )
    }

    private companion object {

        /**
         * inventory:items
         */
        const val INVENTORY_INTERFACE_ID: Int =
            149

        const val INVENTORY_COMPONENT_ID: Int =
            0

        const val EMPTY_ITEM_ID: Int =
            -1
    }
}