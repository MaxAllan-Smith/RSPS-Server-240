package org.example.app.core.equipment

data class WeaponDefinition(
    val categoryId: Int,
) {
    init {
        require(categoryId >= 0) {
            "Weapon category id must be non-negative."
        }
    }
}