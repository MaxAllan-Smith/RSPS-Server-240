package org.example.app.core.items

data class ItemStack(
    val id: Int,
    val amount: Int = 1,
) {
    init {
        require(id >= 0) {
            "Item id must be non-negative."
        }

        require(amount > 0) {
            "Item amount must be positive."
        }
    }
}