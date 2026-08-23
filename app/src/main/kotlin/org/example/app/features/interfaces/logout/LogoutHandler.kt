package org.example.app.features.interfaces.logout

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.outgoing.logout.Logout
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout

internal class LogoutHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.op != 1) {
            return
        }

        if (!isLogoutButton(packet)) {
            return
        }

        player.session.discardLowPriorityCategoryPackets()
        player.session.queue(Logout)

        player.disconnect(
            reason = "Logout requested.",
        )
    }

    private fun isLogoutButton(packet: If3Button): Boolean =
        when (packet.interfaceId) {
            GameframeLayout.Interface.LOGOUT ->
                packet.componentId == LOGOUT_COMPONENT

            GameframeLayout.Interface.WORLD_SWITCHER ->
                packet.componentId == WORLD_SWITCHER_LOGOUT_COMPONENT

            else ->
                false
        }

    private companion object {
        const val LOGOUT_COMPONENT: Int = 8
        const val WORLD_SWITCHER_LOGOUT_COMPONENT: Int = 25
    }
}