package org.example.app.features.chat

import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class ChatFeature(
    private val chatService: ChatService =
        ChatService(),
) : Feature {

    private val publicChatHandler =
        PublicChatHandler(
            chatService = chatService,
        )

    override val id: String =
        "chat"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<MessagePublic> { packet ->
                publicChatHandler.handle(
                    player = this,
                    packet = packet,
                )
            }
        }
    }
}