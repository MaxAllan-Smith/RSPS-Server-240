package org.example.app.core.inventory

import org.example.app.core.items.ItemStack

class PlayerInventory {

    private val items: Array<ItemStack?> =
        arrayOfNulls(CAPACITY)

    var revision: Int = 0
        private set

    operator fun get(
        slot: Int,
    ): ItemStack? {
        require(slot in items.indices) {
            "Inventory slot $slot is outside 0..${items.lastIndex}."
        }

        return items[slot]
    }

    fun add(
        item: ItemStack,
    ): Boolean {
        val slot =
            items.indexOfFirst {
                it == null
            }

        if (slot == -1) {
            return false
        }

        items[slot] =
            item

        revision++

        return true
    }

    fun set(
        slot: Int,
        item: ItemStack?,
    ): ItemStack? {
        require(slot in items.indices) {
            "Inventory slot $slot is outside 0..${items.lastIndex}."
        }

        val previous =
            items[slot]

        if (previous == item) {
            return previous
        }

        items[slot] =
            item

        revision++

        return previous
    }

    fun clear(
        slot: Int,
    ): ItemStack? {
        require(slot in items.indices) {
            "Inventory slot $slot is outside 0..${items.lastIndex}."
        }

        val previous =
            items[slot]
                ?: return null

        items[slot] =
            null

        revision++

        return previous
    }

    companion object {
        const val CAPACITY: Int = 28
    }
}