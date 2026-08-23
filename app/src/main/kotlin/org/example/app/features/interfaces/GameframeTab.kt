package org.example.app.features.interfaces

internal enum class GameframeTab(
    val slot: Int,
    val interfaceId: Int,
) {
    COMBAT(
        slot = 76,
        interfaceId = 593,
    ),

    SKILLS(
        slot = 77,
        interfaceId = 320,
    ),

    INVENTORY(
        slot = 79,
        interfaceId = 149,
    ),

    EQUIPMENT(
        slot = 80,
        interfaceId = 387,
    ),
}