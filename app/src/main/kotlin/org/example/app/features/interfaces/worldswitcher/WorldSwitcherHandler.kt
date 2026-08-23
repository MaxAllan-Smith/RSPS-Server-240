package org.example.app.features.interfaces.worldswitcher

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout

internal class WorldSwitcherHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.interfaceId != GameframeLayout.Interface.LOGOUT) {
            return
        }

        if (packet.componentId != WORLD_SWITCHER_COMPONENT || packet.op != 1) {
            return
        }

        player.session.queue(
            IfOpenSub(
                destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                destinationComponentId = GameframeLayout.Slot.MAIN_MODAL,
                interfaceId = GameframeLayout.Interface.WORLD_SWITCHER,
                type = MODAL_TYPE,
            )
        )

        println(
            "[Interfaces] Opened world switcher " +
                "${GameframeLayout.Interface.WORLD_SWITCHER} " +
                "for '${player.username}'."
        )
    }

    private companion object {
        const val WORLD_SWITCHER_COMPONENT: Int = 3
        const val MODAL_TYPE: Int = 0
    }
}