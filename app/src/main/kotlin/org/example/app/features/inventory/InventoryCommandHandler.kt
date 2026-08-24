package org.example.app.features.inventory

import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player

internal class InventoryCommandHandler {

    fun handle(
        player: Player,
        command: String,
        arguments: List<String>,
    ): Boolean =
        when (command) {
            GIVE_ITEM_COMMAND ->
                handleGiveItem(
                    player = player,
                    arguments = arguments,
                )

            else ->
                false
        }

    private fun handleGiveItem(
        player: Player,
        arguments: List<String>,
    ): Boolean {
        val itemId =
            arguments.getOrNull(0)
                ?.toIntOrNull()

        val amount =
            arguments.getOrNull(1)
                ?.toIntOrNull()
                ?: 1

        if (
            itemId == null ||
            itemId < 0 ||
            amount <= 0
        ) {
            println(
                "[Inventory] Usage: ::giveitem <itemId> [amount]"
            )

            return true
        }

        val added =
            player.inventory.add(
                ItemStack(
                    id = itemId,
                    amount = amount,
                ),
            )

        if (!added) {
            println(
                "[Inventory] '${player.username}' inventory is full."
            )

            return true
        }

        println(
            "[Inventory] '${player.username}' added " +
                "item=$itemId amount=$amount."
        )

        return true
    }

    private companion object {
        const val GIVE_ITEM_COMMAND: String =
            "giveitem"
    }
}