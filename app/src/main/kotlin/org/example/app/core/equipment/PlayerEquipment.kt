package org.example.app.core.equipment

import org.example.app.core.items.ItemStack

/** Per-player worn-item container, revisioned so sync services can detect changes cheaply. */
class PlayerEquipment {

    private val items: Array<ItemStack?> =
        arrayOfNulls(CAPACITY)

    var revision: Int = 0
        private set

    operator fun get(
        slot: EquipmentSlot,
    ): ItemStack? =
        items[slot.id]

    fun set(
        slot: EquipmentSlot,
        item: ItemStack,
    ): ItemStack? {
        val previous =
            items[slot.id]

        if (previous == item) {
            return previous
        }

        items[slot.id] =
            item

        revision++

        return previous
    }

    fun clear(
        slot: EquipmentSlot,
    ): ItemStack? {
        val previous =
            items[slot.id]
                ?: return null

        items[slot.id] =
            null

        revision++

        return previous
    }

    private companion object {
        const val CAPACITY: Int = 14
    }
}