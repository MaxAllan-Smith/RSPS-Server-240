package org.example.app.network

import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.api.GameConnectionHandler
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import org.example.app.player.Player

class RsProtConnectionHandler(
    private val loginRequests:
        LoginRequestQueue,
) : GameConnectionHandler<Player> {

    override fun onLogin(
        responseHandler:
            GameLoginResponseHandler<Player>,
        block:
            LoginBlock<AuthenticationType>,
    ) {
        println(
            "[Login] Decoded login request " +
                "username='${block.username}' " +
                "from=" +
                responseHandler
                    .ctx
                    .channel()
                    .remoteAddress()
        )

        if (
            !responseHandler
                .validateNewConnection()
        ) {
            println(
                "[Login] Connection limit rejected " +
                    "'${block.username}'."
            )

            responseHandler
                .writeFailedResponse(
                    LoginResponse.TooManyAttempts
                )

            return
        }

        /*
         * Networking/login callback -> game thread.
         */
        loginRequests.offer(
            PendingLogin(
                responseHandler =
                    responseHandler,
                loginBlock =
                    block,
            )
        )
    }

    override fun onReconnect(
        responseHandler:
            GameLoginResponseHandler<Player>,
        block:
            LoginBlock<XteaKey>,
    ) {
        println(
            "[Login] Reconnect requested " +
                "from=" +
                responseHandler
                    .ctx
                    .channel()
                    .remoteAddress() +
                " but reconnect is not implemented yet."
        )

        /*
         * Full reconnect requires retaining the previous Session<Player>
         * and resetting PlayerInfo/NpcInfo/WorldEntityInfo reconnect state.
         *
         * We'll add this after normal world login is stable.
         */
        responseHandler
            .writeFailedResponse(
                LoginResponse.ConnectFail
            )
    }
}