package org.example.app.features.interfaces.social

import org.example.app.features.interfaces.gameframe.GameframeLayout

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