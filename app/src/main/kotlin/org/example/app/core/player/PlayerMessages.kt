package org.example.app.core.player

import net.rsprot.protocol.game.outgoing.misc.player.MessageGame

/** Extension helper for queuing a standard client game-message chat line to a player. */
fun Player.sendGameMessage(text: String) {
    session.queue(
        MessageGame(
            GAME_MESSAGE_TYPE,
            text,
        )
    )
}

private const val GAME_MESSAGE_TYPE: Int = 0