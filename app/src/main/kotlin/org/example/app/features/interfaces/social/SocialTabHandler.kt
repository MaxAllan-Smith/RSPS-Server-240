package org.example.app.features.interfaces.social

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout
import org.example.app.features.interfaces.gameframe.GameframeService

internal class SocialTabHandler(
    private val gameframeService: GameframeService,
) {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.componentId != SWITCH_LIST_COMPONENT) {
            return
        }

        if (packet.op != 1) {
            return
        }

        val target =
            when (packet.interfaceId) {
                GameframeLayout.Interface.FRIENDS_LIST ->
                    SocialView.IGNORE

                GameframeLayout.Interface.IGNORE_LIST ->
                    SocialView.FRIENDS

                else ->
                    return
            }

        gameframeService.selectSocialView(
            player = player,
            view = target,
        )
    }

    private companion object {
        const val SWITCH_LIST_COMPONENT: Int = 1
    }
}