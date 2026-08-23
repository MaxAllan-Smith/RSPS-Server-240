package org.example.app.features.interfaces

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import org.example.app.core.player.Player

/**
 * Initializes the persistent parts of the player's gameframe.
 *
 * Each child interface is mounted only once for the lifetime of the player's
 * current session. The mounted state is tracked in [GameframeState].
 */
internal class GameframeService {

    fun mountInitialLayout(
        player: Player,
    ) {
        /*
         * We currently only support the revision-240 resizable gameframe.
         * Fixed-mode layout support can be added separately later.
         */
        if (!player.resizable) {
            return
        }

        val state = player.gameframeState

        /*
         * The chatbox client script checks varbit 8119 to determine whether
         * the account has an active display name.
         *
         * The client already knows the player's username through player-info
         * appearance data; this varbit simply unlocks normal chatbox input.
         *
         * PlayerVars resolves the backing varp and bit range from the active
         * cache, so the interface feature does not need to know those details.
         */
        player.vars.setVarbit(
            id = CHATBOX_UNLOCKED_VARBIT,
            value = 1,
        )

        /*
         * Mount interface 160 into the minimap/orb container on the stretched
         * top-level interface.
         *
         * 161:33 -> 160
         *
         * Interface 160 contains the minimap-side UI such as the status orbs.
         */
        if (!state.minimapMounted) {
            player.session.queue(
                IfOpenSub(
                    destinationInterfaceId =
                        GameframeLayout.TopLevel.RESIZABLE,
                    destinationComponentId =
                        GameframeLayout.Slot.MINIMAP_ORBS,
                    interfaceId =
                        GameframeLayout.Interface.MINIMAP,
                    type =
                        OVERLAY_TYPE,
                )
            )

            state.minimapMounted = true

            println(
                "[Interfaces] Mounted minimap/orbs " +
                    "${GameframeLayout.Interface.MINIMAP} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.MINIMAP_ORBS} " +
                    "for '${player.username}'."
            )
        }

        /*
         * Mount the persistent chatbox into the stretched gameframe.
         *
         * 161:96 -> 162
         */
        if (!state.chatboxMounted) {
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

            state.chatboxMounted =
                true

            println(
                "[Interfaces] Mounted chatbox " +
                    "${GameframeLayout.Interface.CHATBOX} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.CHATBOX} " +
                    "for '${player.username}'."
            )
        }
    }

    private companion object {

        /**
         * IF_OPENSUB type used for persistent, non-modal child interfaces.
         */
        const val OVERLAY_TYPE: Int =
            1

        /**
         * Client varbit indicating that the player has an active display name
         * and is permitted to use normal chatbox input.
         */
        const val CHATBOX_UNLOCKED_VARBIT: Int =
            8119
    }
}