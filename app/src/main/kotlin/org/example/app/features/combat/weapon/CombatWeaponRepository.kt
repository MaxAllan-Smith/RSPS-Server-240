package org.example.app.features.combat.weapon

internal class CombatWeaponRepository(
    definitions: Iterable<CombatWeaponDefinition>,
) {

    private val definitionsByItem =
        definitions.associateBy(
            CombatWeaponDefinition::itemId
        )

    fun find(
        itemId: Int,
    ): CombatWeaponDefinition? =
        definitionsByItem[itemId]
}
