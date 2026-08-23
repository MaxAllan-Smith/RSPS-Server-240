package org.example.app.features.chat

import org.example.app.core.player.Player

internal class ChatService {
    
    fun updateChatModes(
        player: Player,
        publicMode: Int,
        privateMode: Int
    ) {
        val state = player.chatState
        
        state.publicMode = publicMode
        state.privateMode = privateMode
    }
    
    fun recordMessage(
        player: Player,
        message: String
    ) {
        val normalizeMessage = message.trim()
        
        if (normalizeMessage.isEmpty()) {
            return
        }
        
        player.chatState.lastMessage = normalizeMessage
    }
}