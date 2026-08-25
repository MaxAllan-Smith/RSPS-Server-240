package org.example.app.features.combat.model

/** The attack styles available for one weapon category, in client display order. */
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
