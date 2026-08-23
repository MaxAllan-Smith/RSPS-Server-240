package org.example.app.features.interfaces

internal enum class SocialView(
    val interfaceId: Int,
) {
    FRIENDS(
        interfaceId = GameframeLayout.Interface.FRIENDS_LIST,
    ),

    IGNORE(
        interfaceId = GameframeLayout.Interface.IGNORE_LIST,
    ),
}