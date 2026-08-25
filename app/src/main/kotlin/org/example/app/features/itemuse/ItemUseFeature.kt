package org.example.app.features.itemuse

import net.rsprot.protocol.game.incoming.buttons.IfButtonT
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage

/**
 * Generic selected-item interaction feature.
 *
 * This initial slice handles inventory item -> inventory item targeting.
 *
 * Future extensions can add:
 *
 * - item -> loc;
 * - item -> NPC;
 * - item -> player;
 * - item -> ground item.
 */
internal class ItemUseFeature :
    Feature {

    override val id: String =
        "item-use"

    private val itemOnItem =
        ItemOnItemDispatcher()

    init {
        registerInteractions()
    }

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<IfButtonT> { packet ->
                handleItemOnItem(
                    player = this,
                    packet = packet,
                )
            }
        }
    }

    /**
     * Registers generic item combinations.
     *
     * Logs + Tinderbox is deliberately only a proof-of-dispatch interaction
     * here. Actual Firemaking behavior comes in the next vertical slice.
     */
    private fun registerInteractions() {
        itemOnItem.register(
            firstItemId =
                LOGS_ITEM_ID,

            secondItemId =
                TINDERBOX_ITEM_ID,
        ) { interaction ->
            println(
                "[ItemUse] '${interaction.player.username}' matched " +
                    "Logs + Tinderbox: " +
                    "selected=${interaction.selectedItemId}@" +
                    "${interaction.selectedSlot}, " +
                    "target=${interaction.targetItemId}@" +
                    "${interaction.targetSlot}."
            )

            interaction.player.sendGameMessage(
                "You strike the tinderbox against the logs."
            )
        }
    }

    /**
     * Handles inventory item -> inventory item targeting.
     */
    private fun handleItemOnItem(
        player: Player,
        packet: IfButtonT,
    ) {
        /*
         * This feature currently accepts inventory:items -> inventory:items
         * only.
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

        val selectedSlot =
            packet.selectedSub

        val targetSlot =
            packet.targetSub

        if (
            selectedSlot !in
            0 until
                PlayerInventory.CAPACITY ||
            targetSlot !in
            0 until
                PlayerInventory.CAPACITY
        ) {
            println(
                "[ItemUse] '${player.username}' rejected item-on-item: " +
                    "invalid slots selected=$selectedSlot target=$targetSlot."
            )

            return
        }

        /*
         * Using an item on itself in the same slot is not a meaningful
         * item-on-item interaction.
         */
        if (
            selectedSlot ==
            targetSlot
        ) {
            return
        }

        val selectedItem =
            player.inventory[
                selectedSlot
            ]
                ?: run {
                    println(
                        "[ItemUse] '${player.username}' rejected item-on-item: " +
                            "selected slot $selectedSlot is empty."
                    )

                    return
                }

        val targetItem =
            player.inventory[
                targetSlot
            ]
                ?: run {
                    println(
                        "[ItemUse] '${player.username}' rejected item-on-item: " +
                            "target slot $targetSlot is empty."
                    )

                    return
                }

        /*
         * Unlike inventory dragging, IfButtonT's object ids are part of the
         * actual item-use interaction identity.
         *
         * Validate them against authoritative server inventory state.
         */
        if (
            packet.selectedObj !=
            selectedItem.id
        ) {
            println(
                "[ItemUse] '${player.username}' rejected item-on-item: " +
                    "selected slot=$selectedSlot, " +
                    "packetItem=${packet.selectedObj}, " +
                    "serverItem=${selectedItem.id}."
            )

            return
        }

        if (
            packet.targetObj !=
            targetItem.id
        ) {
            println(
                "[ItemUse] '${player.username}' rejected item-on-item: " +
                    "target slot=$targetSlot, " +
                    "packetItem=${packet.targetObj}, " +
                    "serverItem=${targetItem.id}."
            )

            return
        }

        val interaction =
            ItemOnItemInteraction(
                player =
                    player,

                selectedSlot =
                    selectedSlot,

                selectedItemId =
                    selectedItem.id,

                targetSlot =
                    targetSlot,

                targetItemId =
                    targetItem.id,
            )

        if (
            itemOnItem.dispatch(
                interaction
            )
        ) {
            return
        }

        println(
            "[ItemUse] '${player.username}' has no item-on-item interaction " +
                "for ${selectedItem.id} + ${targetItem.id}."
        )

        player.sendGameMessage(
            "Nothing interesting happens."
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

        /**
         * Standard OSRS item ids.
         */
        const val TINDERBOX_ITEM_ID: Int =
            590

        const val LOGS_ITEM_ID: Int =
            1511
    }
}