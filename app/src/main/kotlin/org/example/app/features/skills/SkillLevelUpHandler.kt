package org.example.app.features.skills

import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.game.outgoing.interfaces.IfCloseSub
import org.example.app.core.player.Player

internal class SkillLevelUpHandler {

    fun handle(
        player: Player,
        packet: ResumePauseButton,
    ) {
        if (packet.interfaceId != LEVEL_UP_INTERFACE) {
            return
        }

        if (packet.componentId != CONTINUE_COMPONENT) {
            return
        }

        player.session.queue(
            IfCloseSub(
                interfaceId = CHATBOX_INTERFACE,
                componentId = CHAT_MODAL_COMPONENT,
            )
        )

        println(
            "[Skills] Closed level-up display " +
                "for '${player.username}'."
        )
    }

    private companion object {
        const val CHATBOX_INTERFACE: Int = 162
        const val CHAT_MODAL_COMPONENT: Int = 567

        const val LEVEL_UP_INTERFACE: Int = 233
        const val CONTINUE_COMPONENT: Int = 3
    }
}