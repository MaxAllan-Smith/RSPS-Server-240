package org.example.app.features.interfaces

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import org.example.app.core.player.Player

internal class GameframeService {

    fun mountInitialLayout(
        player: Player,
    ) {
        if (!player.resizable) {
            return
        }

        val state = player.gameframeState

        if (state.chatboxMounted) {
            return
        }

        player.session.queue(
            IfOpenSub(
                destinationInterfaceId =
                    GameframeLayout.TopLevel.RESIZABLE,
                destinationComponentId =
                    GameframeLayout.Slot.CHATBOX,
                interfaceId =
                    GameframeLayout.Interface.CHATBOX,
                type =
                    OVERLAY_TYPE,
            )
        )

        state.chatboxMounted = true

        println(
            "[Interfaces] Mounted chatbox " +
                "${GameframeLayout.Interface.CHATBOX} at " +
                "${GameframeLayout.TopLevel.RESIZABLE}:" +
                "${GameframeLayout.Slot.CHATBOX} " +
                "for '${player.username}'."
        )
    }

    private companion object {
        const val OVERLAY_TYPE: Int = 1
    }
}