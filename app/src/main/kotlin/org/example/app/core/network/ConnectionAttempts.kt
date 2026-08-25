package org.example.app.core.network

import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import org.example.app.core.player.Player

/** Value types describing one in-flight login or reconnect attempt, passed from RSProt's network layer into feature login handlers. */
data class LoginAttempt(
    val responseHandler: GameLoginResponseHandler<Player>,
    val block: LoginBlock<AuthenticationType>,
)

data class ReconnectAttempt(
    val responseHandler: GameLoginResponseHandler<Player>,
    val block: LoginBlock<XteaKey>,
)
