package org.example.app.core.network

import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.api.GameConnectionHandler
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import org.example.app.core.feature.FeatureRuntime
import org.example.app.core.player.Player

/**
 * Thin RSProt transport adapter.
 *
 * It performs transport-level connection validation, then delegates login and
 * reconnect policy to installed features. No gameplay feature is imported.
 */
class RsProtConnectionHandler(
    private val features: FeatureRuntime,
) : GameConnectionHandler<Player> {
    override fun onLogin(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<AuthenticationType>,
    ) {
        println(
            "[Login] Decoded login request " +
                "username='${block.username}' " +
                "from=${responseHandler.ctx.channel().remoteAddress()}"
        )

        if (!responseHandler.validateNewConnection()) {
            println("[Login] Connection limit rejected '${block.username}'.")
            responseHandler.writeFailedResponse(LoginResponse.TooManyAttempts)
            return
        }

        val handled =
            features.dispatchLogin(
                LoginAttempt(
                    responseHandler = responseHandler,
                    block = block,
                )
            )

        if (!handled) {
            System.err.println(
                "[Login] No installed feature accepts normal logins."
            )
            responseHandler.writeFailedResponse(LoginResponse.ConnectFail)
        }
    }

    override fun onReconnect(
        responseHandler: GameLoginResponseHandler<Player>,
        block: LoginBlock<XteaKey>,
    ) {
        val handled =
            features.dispatchReconnect(
                ReconnectAttempt(
                    responseHandler = responseHandler,
                    block = block,
                )
            )

        if (!handled) {
            System.err.println(
                "[Login] No installed feature handles reconnects."
            )
            responseHandler.writeFailedResponse(LoginResponse.ConnectFail)
        }
    }
}
