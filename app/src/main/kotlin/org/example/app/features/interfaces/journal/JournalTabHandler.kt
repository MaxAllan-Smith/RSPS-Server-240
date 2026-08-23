package org.example.app.features.interfaces.journal

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player
import org.example.app.features.interfaces.gameframe.GameframeLayout
import org.example.app.features.interfaces.gameframe.GameframeService

internal class JournalTabHandler(
    private val gameframeService: GameframeService,
) {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.interfaceId != GameframeLayout.Interface.JOURNAL) {
            return
        }

        if (packet.op != 1) {
            return
        }

        val tab =
            JournalTab.fromComponent(packet.componentId)
                ?: return

        gameframeService.selectJournalTab(
            player = player,
            tab = tab,
        )
    }
}