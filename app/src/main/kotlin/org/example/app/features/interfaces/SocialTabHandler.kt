package org.example.app.features.interfaces

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player

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