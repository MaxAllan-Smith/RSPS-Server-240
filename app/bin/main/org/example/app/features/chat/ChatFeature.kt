package org.example.app.features.chat

import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class ChatFeature(
    private val chatService: ChatService =
        ChatService(),
) : Feature {

    override val id: String =
        "chat"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<MessagePublic> { packet ->
                chatService.recordMessage(
                    player = this,
                    message = packet.message,
                )

                println(
                    "[Chat] '$username' " +
                        "message='${packet.message}' " +
                        "type=${packet.type} " +
                        "colour=${packet.colour} " +
                        "effect=${packet.effect}"
                )
            }
        }
    }
}