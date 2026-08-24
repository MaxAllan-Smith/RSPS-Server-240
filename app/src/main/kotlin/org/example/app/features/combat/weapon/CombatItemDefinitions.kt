package org.example.app.features.combat.weapon

import org.example.app.core.equipment.EquipmentDefinition
import org.example.app.core.equipment.EquipmentSkillRequirement
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.items.ItemDefinition
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.skills.Skill

internal object CombatItemDefinitions {

    val repository =
        ItemDefinitionRepository(
            definitions =
                listOf(
                    ItemDefinition(
                        id = 1351,
                        equipment =
                            EquipmentDefinition(
                                slot = EquipmentSlot.WEAPON,
                                skillRequirements =
                                    listOf(
                                        EquipmentSkillRequirement(
                                            skill = Skill.ATTACK,
                                            level = 1,
                                        ),
                                    ),
                            ),
                    ),
                    ItemDefinition(
                        id = 4151,
                        equipment =
                            EquipmentDefinition(
                                slot = EquipmentSlot.WEAPON,
                                skillRequirements =
                                    listOf(
                                        EquipmentSkillRequirement(
                                            skill = Skill.ATTACK,
                                            level = 70,
                                        ),
                                    ),
                            ),
                    ),
                ).associateBy(
                    ItemDefinition::id,
                ),
        )
}