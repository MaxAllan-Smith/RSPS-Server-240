package org.example.app.features.combat.weapon

import org.example.app.core.equipment.EquipmentSkillRequirement
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.skills.Skill
import org.example.app.features.combat.model.CombatWeaponCategory

internal class CombatWeaponRepository(
    definitions: Iterable<CombatWeaponDefinition> =
        DEFAULT_DEFINITIONS,
) {

    private val definitionsByItem =
        definitions.associateBy(
            CombatWeaponDefinition::itemId,
        )

    fun find(
        itemId: Int,
    ): CombatWeaponDefinition? =
        definitionsByItem[itemId]

    private companion object {

        val DEFAULT_DEFINITIONS =
            listOf(
                CombatWeaponDefinition(
                    itemId = 1351,
                    category = CombatWeaponCategory.AXE,
                    equipmentSlot = EquipmentSlot.WEAPON,
                    skillRequirements =
                        listOf(
                            EquipmentSkillRequirement(
                                skill = Skill.ATTACK,
                                level = 1,
                            ),
                        ),
                ),
                CombatWeaponDefinition(
                    itemId = 4151,
                    category = CombatWeaponCategory.WHIP,
                    equipmentSlot = EquipmentSlot.WEAPON,
                    skillRequirements =
                        listOf(
                            EquipmentSkillRequirement(
                                skill = Skill.ATTACK,
                                level = 70,
                            ),
                        ),
                ),
            )
    }
}