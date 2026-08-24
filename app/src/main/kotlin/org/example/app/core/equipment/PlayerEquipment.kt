package org.example.app.core.equipment

import org.example.app.core.items.ItemStack
import java.util.EnumMap

class PlayerEquipment {
    private val items =
        EnumMap<EquipmentSlot, ItemStack>(
            EquipmentSlot::class.java
        )
        
        operator fun get(
            slot: EquipmentSlot
        ): ItemStack? =
            items[slot]
            
        fun set(
            slot: EquipmentSlot,
            item: ItemStack?
        ) {
            if (item == null) {
                items.remove(slot)
                return
            }
            
            items[slot] = item
        }
        
        fun clear(
            slot: EquipmentSlot
        ): ItemStack? =
            items.remove(slot)
}