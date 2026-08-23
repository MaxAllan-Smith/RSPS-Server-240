package org.example.app.features.chat

import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import org.example.app.core.player.Player

internal class PublicChatHandler(
    private val chatService: ChatService,
) {

    fun handle(
        player: Player,
        packet: MessagePublic,
    ) {
        val pattern =
            packet.pattern

        if (
            packet.colour in 13..20 &&
            (
                pattern == null ||
                    !pattern.isValid() ||
                    pattern.length !=
                    packet.colour - 12
            )
        ) {
            println(
                "[Chat] Rejected invalid colour pattern " +
                    "from '${player.username}'."
            )

            return
        }

        chatService.recordMessage(
            player = player,
            message = packet.message,
            colour = packet.colour,
            effect = packet.effect,
            autotyper =
                packet.type == AUTOTYPER_TYPE,
            pattern =
                pattern?.toByteArray(),
        )

        println(
            "[Chat] '${player.username}' " +
                "message='${packet.message}' " +
                "type=${packet.type} " +
                "colour=${packet.colour} " +
                "effect=${packet.effect}"
        )
    }

    private companion object {
        const val AUTOTYPER_TYPE: Int =
            1
    }
}