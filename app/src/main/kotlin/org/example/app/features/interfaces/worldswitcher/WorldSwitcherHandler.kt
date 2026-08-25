package org.example.app.features.interfaces.worldswitcher

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout
import org.example.app.features.interfaces.gameframe.GameframeService
import org.example.app.features.interfaces.logout.LogoutTabView

/** Handles opening the world-switcher interface from the logout tab. */
internal class WorldSwitcherHandler(
    private val gameframeService: GameframeService,
) {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.op != 1) {
            return
        }

        when {
            packet.interfaceId == GameframeLayout.Interface.LOGOUT &&
                packet.componentId == WORLD_SWITCHER_COMPONENT -> {
                gameframeService.selectLogoutView(
                    player = player,
                    view = LogoutTabView.WORLD_SWITCHER,
                )
            }

            packet.interfaceId == GameframeLayout.Interface.WORLD_SWITCHER &&
                packet.componentId == CLOSE_COMPONENT -> {
                gameframeService.selectLogoutView(
                    player = player,
                    view = LogoutTabView.LOGOUT,
                )
            }

            packet.interfaceId == GameframeLayout.Interface.WORLD_SWITCHER &&
                packet.componentId == OPTIONS_COMPONENT -> {
                gameframeService.openWorldSwitcherOptions(player)
            }
        }
    }

    private companion object {
        const val WORLD_SWITCHER_COMPONENT: Int = 3

        const val OPTIONS_COMPONENT: Int = 4
        const val CLOSE_COMPONENT: Int = 5
    }
}