package org.example.app.features.chat

import org.example.app.core.player.Player

internal class ChatService {

    fun updateChatModes(
        player: Player,
        publicMode: Int,
        privateMode: Int,
    ) {
        val state =
            player.chatState

        state.publicMode =
            publicMode

        state.privateMode =
            privateMode
    }

    fun recordMessage(
        player: Player,
        message: String,
        colour: Int,
        effect: Int,
        autotyper: Boolean,
        pattern: ByteArray?,
    ) {
        val normalizedMessage =
            message.trim()

        if (normalizedMessage.isEmpty()) {
            return
        }

        player.chatState.lastMessage =
            normalizedMessage

        player
            .infos
            .playerInfo
            .avatar
            .extendedInfo
            .setChat(
                colour = colour,
                effects = effect,
                modicon = 0,
                autotyper = autotyper,
                text = normalizedMessage,
                pattern = pattern,
            )
    }
}