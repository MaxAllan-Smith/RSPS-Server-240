package org.example.app.core.items

data class ItemDefinition(
    val id: Int,
) {
    init {
        require(id >= 0) {
            "Item id must be non-negative."
        }
    }
}