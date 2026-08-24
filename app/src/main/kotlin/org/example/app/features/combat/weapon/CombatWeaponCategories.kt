package org.example.app.features.combat.weapon

import org.example.app.features.combat.model.CombatAttackType
import org.example.app.features.combat.model.CombatStance
import org.example.app.features.combat.model.CombatStyle
import org.example.app.features.combat.model.CombatStyleDefinition
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.model.CombatWeaponCategoryDefinition

internal object CombatWeaponCategories {

    val UNARMED =
        CombatWeaponCategoryDefinition(
            category = CombatWeaponCategory.UNARMED,
            styles =
                listOf(
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_0,
                        name = "Punch",
                        stance = CombatStance.ACCURATE,
                        attackType = CombatAttackType.CRUSH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_1,
                        name = "Kick",
                        stance = CombatStance.AGGRESSIVE,
                        attackType = CombatAttackType.CRUSH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_3,
                        name = "Block",
                        stance = CombatStance.DEFENSIVE,
                        attackType = CombatAttackType.CRUSH,
                    ),
                ),
        )

    val AXE =
        CombatWeaponCategoryDefinition(
            category = CombatWeaponCategory.AXE,
            styles =
                listOf(
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_0,
                        name = "Chop",
                        stance = CombatStance.ACCURATE,
                        attackType = CombatAttackType.SLASH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_1,
                        name = "Hack",
                        stance = CombatStance.AGGRESSIVE,
                        attackType = CombatAttackType.SLASH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_2,
                        name = "Smash",
                        stance = CombatStance.AGGRESSIVE,
                        attackType = CombatAttackType.CRUSH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_3,
                        name = "Block",
                        stance = CombatStance.DEFENSIVE,
                        attackType = CombatAttackType.SLASH,
                    ),
                ),
        )

    val WHIP =
        CombatWeaponCategoryDefinition(
            category = CombatWeaponCategory.WHIP,
            styles =
                listOf(
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_0,
                        name = "Flick",
                        stance = CombatStance.ACCURATE,
                        attackType = CombatAttackType.SLASH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_1,
                        name = "Lash",
                        stance = CombatStance.CONTROLLED,
                        attackType = CombatAttackType.SLASH,
                    ),
                    CombatStyleDefinition(
                        style = CombatStyle.STYLE_3,
                        name = "Deflect",
                        stance = CombatStance.DEFENSIVE,
                        attackType = CombatAttackType.SLASH,
                    ),
                ),
        )

    private val definitions: Map<
        CombatWeaponCategory,
        CombatWeaponCategoryDefinition,
    > =
        listOf(
            UNARMED,
            AXE,
            WHIP,
        ).associateBy {
            it.category
        }

    fun find(
        category: CombatWeaponCategory,
    ): CombatWeaponCategoryDefinition? =
        definitions[category]
}