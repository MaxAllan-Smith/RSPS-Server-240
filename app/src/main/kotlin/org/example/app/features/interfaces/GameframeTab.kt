package org.example.app.features.interfaces

internal enum class GameframeTab(
    val slot: Int,
    val interfaceId: Int,
) {
    COMBAT(
        slot = GameframeLayout.Slot.COMBAT,
        interfaceId = GameframeLayout.Interface.COMBAT,
    ),

    SKILLS(
        slot = GameframeLayout.Slot.SKILLS,
        interfaceId = GameframeLayout.Interface.SKILLS,
    ),

    INVENTORY(
        slot = GameframeLayout.Slot.INVENTORY,
        interfaceId = GameframeLayout.Interface.INVENTORY,
    ),

    EQUIPMENT(
        slot = GameframeLayout.Slot.EQUIPMENT,
        interfaceId = GameframeLayout.Interface.EQUIPMENT,
    ),

    PRAYER(
        slot = GameframeLayout.Slot.PRAYER,
        interfaceId = GameframeLayout.Interface.PRAYER,
    ),
}