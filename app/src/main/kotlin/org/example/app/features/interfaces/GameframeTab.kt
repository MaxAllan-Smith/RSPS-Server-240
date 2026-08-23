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
    SPELLBOOK(
        slot = GameframeLayout.Slot.SPELLBOOK,
        interfaceId = GameframeLayout.Interface.SPELLBOOK
    ),
    FRIENDS_CHAT(
        slot = GameframeLayout.Slot.FRIENDS_CHAT,
        interfaceId = GameframeLayout.Interface.FRIENDS_CHAT
    ),
    FRIENDS_LIST(
        slot = GameframeLayout.Slot.FRIENDS_LIST,
        interfaceId = GameframeLayout.Interface.FRIENDS_LIST
    ),
    IGNORE_LIST(
        slot = GameframeLayout.Slot.IGNORE_LIST,
        interfaceId = GameframeLayout.Interface.IGNORE_LIST
    ),
    LOGOUT(
        slot = GameframeLayout.Slot.LOGOUT,
        interfaceId = GameframeLayout.Interface.LOGOUT
    ),
    SETTINGS(
        slot = GameframeLayout.Slot.SETTINGS,
        interfaceId = GameframeLayout.Interface.SETTINGS
    ),
    EMOTES(
        slot = GameframeLayout.Slot.EMOTES,
        interfaceId = GameframeLayout.Interface.EMOTES
    ),
    MUSIC(
        slot = GameframeLayout.Slot.MUSIC,
        interfaceId = GameframeLayout.Interface.MUSIC
    )
}