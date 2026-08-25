package org.example.app.features.interfaces.logout

import org.example.app.features.interfaces.gameframe.GameframeLayout

/** The logout tab's interface id. */
internal enum class LogoutTabView(
    val interfaceId: Int,
) {
    LOGOUT(
        interfaceId = GameframeLayout.Interface.LOGOUT,
    ),

    WORLD_SWITCHER(
        interfaceId = GameframeLayout.Interface.WORLD_SWITCHER,
    ),
}