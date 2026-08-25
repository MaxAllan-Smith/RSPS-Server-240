package org.example.app.features.chat

import org.example.app.core.player.Player

/** Per-player chat mode and last-message state. */
data class ChatState(
    var publicMode: Int = 0,
    var privateMode: Int = 0,
    var lastMessage: String? = null
)

internal val Player.chatState: ChatState
    get() = 
        featureState.getOrPut(
            ChatState::class,
                ::ChatState
        )
