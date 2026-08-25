package org.example.app.core.persistence

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.items.ItemStack
import org.example.app.core.player.WorldPosition
import org.example.app.core.skills.Skill

/** Plain snapshot of everything persisted for one player: position, skills, inventory and equipment. */
data class PlayerSaveData(
    val position: WorldPosition,
    val skillExperience: Map<Skill, Int>,
    val inventory: Map<Int, ItemStack>,
    val equipment: Map<EquipmentSlot, ItemStack>,
)