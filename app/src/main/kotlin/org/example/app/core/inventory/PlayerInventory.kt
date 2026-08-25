package org.example.app.core.inventory

import org.example.app.core.items.ItemStack

/**
 * Authoritative 28-slot player inventory.
 *
 * Every successful mutation increments [revision], allowing the protocol-facing
 * inventory feature to synchronize only when the contents actually change.
 */
class PlayerInventory {

    private val items:
        Array<ItemStack?> =
        arrayOfNulls(
            CAPACITY
        )

    var revision: Int =
        0
        private set

    operator fun get(
        slot: Int,
    ): ItemStack? {
        requireValidSlot(
            slot
        )

        return items[
            slot
        ]
    }

    /**
     * Whether at least one ordinary inventory slot is currently empty.
     *
     * General stack merging is not implemented yet, so adding another
     * non-stackable item currently requires a genuinely free slot.
     */
    fun hasFreeSlot(): Boolean =
        items.any {
            it == null
        }

    /**
     * Adds an item to the first available inventory slot.
     */
    fun add(
        item: ItemStack,
    ): Boolean {
        val slot =
            items.indexOfFirst {
                it == null
            }

        if (
            slot ==
            -1
        ) {
            return false
        }

        items[
            slot
        ] =
            item

        revision++

        return true
    }

    /**
     * Replaces the contents of one exact slot.
     *
     * Returns the previous contents.
     */
    fun set(
        slot: Int,
        item: ItemStack?,
    ): ItemStack? {
        requireValidSlot(
            slot
        )

        val previous =
            items[
                slot
            ]

        if (
            previous ==
            item
        ) {
            return previous
        }

        items[
            slot
        ] =
            item

        revision++

        return previous
    }

    /**
     * Removes and returns the contents of one exact slot.
     */
    fun clear(
        slot: Int,
    ): ItemStack? {
        requireValidSlot(
            slot
        )

        val previous =
            items[
                slot
            ]
                ?: return null

        items[
            slot
        ] =
            null

        revision++

        return previous
    }

    /**
     * Atomically exchanges two inventory slots.
     *
     * This is the primitive used by normal inventory drag/rearrangement.
     *
     * Examples:
     *
     * occupied -> occupied:
     *
     * A B C
     * drag A onto C
     * C B A
     *
     * occupied -> empty:
     *
     * A B -
     * drag A onto -
     * - B A
     *
     * Only one inventory revision is produced for the complete operation.
     */
    fun swap(
        firstSlot: Int,
        secondSlot: Int,
    ): Boolean {
        requireValidSlot(
            firstSlot
        )

        requireValidSlot(
            secondSlot
        )

        if (
            firstSlot ==
            secondSlot
        ) {
            return false
        }

        val first =
            items[
                firstSlot
            ]

        val second =
            items[
                secondSlot
            ]

        /*
         * Swapping two empty slots is not a mutation.
         */
        if (
            first == null &&
            second == null
        ) {
            return false
        }

        items[
            firstSlot
        ] =
            second

        items[
            secondSlot
        ] =
            first

        revision++

        return true
    }

    private fun requireValidSlot(
        slot: Int,
    ) {
        require(
            slot in
                items.indices
        ) {
            "Inventory slot $slot is outside 0..${items.lastIndex}."
        }
    }

    companion object {
        const val CAPACITY: Int =
            28
    }
}