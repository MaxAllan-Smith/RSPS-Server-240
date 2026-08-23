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

        val state =
            player.gameframeState

        if (state.chatboxMounted) {
            return
        }

        /*
         * The chatbox client script checks varbit 8119 before allowing
         * normal chat input.
         *
         * 0 = player has not configured a display name
         * 1 = player has a display name and may use the chatbox
         *
         * PlayerVars resolves the actual backing varp and bit range from
         * the active revision-240.3 cache.
         */
        player.vars.setVarbit(
            id = CHATBOX_UNLOCKED_VARBIT,
            value = 1,
        )

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

        const val OVERLAY_TYPE: Int =
            1

        const val CHATBOX_UNLOCKED_VARBIT: Int =
            8119
    }
}