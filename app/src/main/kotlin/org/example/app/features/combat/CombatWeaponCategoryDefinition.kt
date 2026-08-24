package org.example.app.features.combat

internal data class CombatWeaponCategoryDefinition(
    val category: CombatWeaponCategory,
    val styles: List<CombatStyleDefinition>,
) {
    fun style(
        style: CombatStyle,
    ): CombatStyleDefinition? =
        styles.firstOrNull {
            it.style == style
        }
}