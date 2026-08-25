package org.example.app.features.itemuse

/**
 * Generic registry for inventory item-on-item interactions.
 *
 * Item pairs are order-independent by default:
 *
 * Logs -> Tinderbox
 *
 * and
 *
 * Tinderbox -> Logs
 *
 * resolve to the same registered interaction.
 */
internal class ItemOnItemDispatcher {

    private val handlers =
        mutableMapOf<ItemPair, ItemOnItemHandler>()

    /**
     * Registers one unordered item pair.
     */
    fun register(
        firstItemId: Int,
        secondItemId: Int,
        handler: ItemOnItemHandler,
    ) {
        val key =
            ItemPair.of(
                firstItemId,
                secondItemId,
            )

        check(
            handlers.putIfAbsent(
                key,
                handler,
            ) == null
        ) {
            "Duplicate item-on-item interaction registration for " +
                "${key.firstItemId} + ${key.secondItemId}."
        }
    }

    /**
     * Dispatches a validated interaction.
     *
     * @return true when a registered handler matched the item pair.
     */
    fun dispatch(
        interaction: ItemOnItemInteraction,
    ): Boolean {
        val key =
            ItemPair.of(
                interaction.selectedItemId,
                interaction.targetItemId,
            )

        val handler =
            handlers[key]
                ?: return false

        handler.handle(
            interaction
        )

        return true
    }
}

internal fun interface ItemOnItemHandler {

    fun handle(
        interaction: ItemOnItemInteraction,
    )
}

/**
 * Canonical unordered pair key.
 *
 * Sorting the two ids means A+B and B+A map to the same registry entry.
 */
private data class ItemPair(
    val firstItemId: Int,
    val secondItemId: Int,
) {

    companion object {

        fun of(
            firstItemId: Int,
            secondItemId: Int,
        ): ItemPair {
            return if (
                firstItemId <=
                secondItemId
            ) {
                ItemPair(
                    firstItemId =
                        firstItemId,

                    secondItemId =
                        secondItemId,
                )
            } else {
                ItemPair(
                    firstItemId =
                        secondItemId,

                    secondItemId =
                        firstItemId,
                )
            }
        }
    }
}