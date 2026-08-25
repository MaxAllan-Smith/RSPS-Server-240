package org.example.app.core.equipment

/** The wearable equipment slots and their RSProt/cache slot ids. */
enum class EquipmentSlot(
    val id: Int,
) {
    HEAD(0),
    CAPE(1),
    AMULET(2),
    WEAPON(3),
    BODY(4),
    SHIELD(5),
    LEGS(7),
    HANDS(9),
    FEET(10),
    RING(12),
    AMMO(13),
}