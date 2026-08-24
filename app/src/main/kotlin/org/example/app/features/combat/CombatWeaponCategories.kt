package org.example.app.features.combat

internal object CombatWeaponCategories {

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
}