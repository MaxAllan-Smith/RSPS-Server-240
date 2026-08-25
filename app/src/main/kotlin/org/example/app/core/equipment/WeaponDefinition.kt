package org.example.app.core.equipment

/** The weapon-specific portion of an item definition: its combat weapon category id. */
data class WeaponDefinition(
    val categoryId: Int,
) {
    init {
        require(categoryId >= 0) {
            "Weapon category id must be non-negative."
        }
    }
}