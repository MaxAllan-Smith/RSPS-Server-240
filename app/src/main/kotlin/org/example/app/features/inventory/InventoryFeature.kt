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
 * Gameplay-specific item actions belong to their respective gameplay features.
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
     * Inventory dragging is purely positional. The client tells us which
     * inventory slot was dragged and which inventory slot it was dropped on.
     *
     * The server remains authoritative over the actual objects occupying those
     * slots. We therefore never use the client-supplied object ids to decide
     * what gets moved.
     */
    private fun handleDrag(
        player: Player,
        packet: IfButtonD,
    ) {
        /*
         * Only allow inventory -> inventory rearrangement.
         *
         * Transfers involving equipment, banks, shops, etc. require their own
         * container-transfer semantics and must not fall through here.
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

        /*
         * The source slot must actually contain something server-side.
         *
         * This is the only item-presence condition required for a reorder.
         */
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

        val targetItem =
            player.inventory[
                targetSlot
            ]

        /*
         * Do not reject the operation based on selectedObj/targetObj.
         *
         * In the revision-240 client currently being tested, IfButtonD is
         * reporting an unexpected selectedObj value (observed as 6512 while
         * dragging item 1511).
         *
         * For a reorder this does not create a trust problem: both objects
         * being manipulated come exclusively from authoritative server slots.
         */
        if (
            packet.selectedObj !=
            sourceItem.id
        ) {
            println(
                "[Inventory] '${player.username}' drag packet source object " +
                    "differs from server state: " +
                    "slot=$sourceSlot, " +
                    "packet=${packet.selectedObj}, " +
                    "server=${sourceItem.id}; " +
                    "using authoritative server item."
            )
        }

        if (
            targetItem != null &&
            packet.targetObj !=
            targetItem.id
        ) {
            println(
                "[Inventory] '${player.username}' drag packet target object " +
                    "differs from server state: " +
                    "slot=$targetSlot, " +
                    "packet=${packet.targetObj}, " +
                    "server=${targetItem.id}; " +
                    "using authoritative server item."
            )
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
            buildString {
                append(
                    "[Inventory] '${player.username}' moved "
                )

                append(
                    "item=${sourceItem.id} "
                )

                append(
                    "slot=$sourceSlot -> $targetSlot"
                )

                if (
                    targetItem != null
                ) {
                    append(
                        "; swapped with item=${targetItem.id}"
                    )
                }

                append(
                    "."
                )
            }
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
    }
}