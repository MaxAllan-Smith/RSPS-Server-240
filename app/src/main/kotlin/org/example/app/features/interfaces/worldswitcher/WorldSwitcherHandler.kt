package org.example.app.features.interfaces.worldswitcher

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout
import org.example.app.features.interfaces.gameframe.GameframeService
import org.example.app.features.interfaces.logout.LogoutTabView

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

        val target =
            when {
                packet.interfaceId == GameframeLayout.Interface.LOGOUT &&
                    packet.componentId == WORLD_SWITCHER_COMPONENT ->
                    LogoutTabView.WORLD_SWITCHER

                packet.interfaceId == GameframeLayout.Interface.WORLD_SWITCHER &&
                    packet.componentId == CLOSE_COMPONENT ->
                    LogoutTabView.LOGOUT

                else ->
                    return
            }

        gameframeService.selectLogoutView(
            player = player,
            view = target,
        )
    }

    private companion object {
        const val WORLD_SWITCHER_COMPONENT: Int = 3
        const val CLOSE_COMPONENT: Int = 5
    }
}