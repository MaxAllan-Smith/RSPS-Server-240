package org.example.app.features.itemuse

import net.rsprot.protocol.game.incoming.buttons.IfButtonT
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.items.ItemOnItemDispatcher
import org.example.app.core.items.ItemOnItemInteraction
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage

/**
 * Generic selected-item interaction feature.
 *
 * This slice handles inventory item -> inventory item targeting.
 *
 * The actual gameplay interactions are registered by vertical gameplay
 * features such as Firemaking rather than being hard-coded here.
 */
internal class ItemUseFeature(
    private val itemOnItem:
        ItemOnItemDispatcher,
) : Feature {

    override val id: String =
        "item-use"

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
         * Unlike inventory dragging, these ids describe the actual selected
         * item interaction and are therefore validated.
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

        const val INVENTORY_INTERFACE_ID: Int =
            149

        const val INVENTORY_COMPONENT_ID: Int =
            0
    }
}