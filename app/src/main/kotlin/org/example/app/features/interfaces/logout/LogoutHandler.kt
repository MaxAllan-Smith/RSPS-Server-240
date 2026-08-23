package org.example.app.features.interfaces.logout

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout

internal class LogoutHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.interfaceId != GameframeLayout.Interface.LOGOUT) {
            return
        }

        if (packet.componentId != LOGOUT_COMPONENT || packet.op != 1) {
            return
        }

        player.disconnect(
            reason = "Logout requested.",
        )
    }

    private companion object {
        const val LOGOUT_COMPONENT: Int = 8
    }
}