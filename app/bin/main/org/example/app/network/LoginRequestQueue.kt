package org.example.app.network

import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import org.example.app.player.Player
import java.util.concurrent.ConcurrentLinkedQueue

data class PendingLogin(
    val responseHandler:
        GameLoginResponseHandler<Player>,
    val loginBlock:
        LoginBlock<AuthenticationType>,
)

class LoginRequestQueue {

    private val queue =
        ConcurrentLinkedQueue<PendingLogin>()

    fun offer(
        request: PendingLogin,
    ) {
        queue.offer(
            request
        )
    }

    fun poll(): PendingLogin? {
        return queue.poll()
    }
}