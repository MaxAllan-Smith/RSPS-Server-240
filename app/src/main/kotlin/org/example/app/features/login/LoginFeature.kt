package org.example.app.features.login

import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * Login vertical slice.
 *
 * Owns login ingress, game-thread login acceptance, local identity and default
 * appearance policy. Core networking only forwards decoded RSProt attempts.
 */
class LoginFeature : Feature {
    override val id: String = "login"

    private val queue = LoginQueue()
    private val processor = LoginProcessor(queue)

    override fun install(registrar: FeatureRegistrar) {
        registrar.onLogin(queue::offer)

        registrar.onReconnect { attempt ->
            println(
                "[Login] Reconnect requested " +
                    "from=${attempt.responseHandler.ctx.channel().remoteAddress()} " +
                    "but reconnect is not implemented yet."
            )

            attempt.responseHandler.writeFailedResponse(
                LoginResponse.ConnectFail
            )
        }

        // Login acceptance must happen before per-player processing this tick.
        registrar.onCycleStart(
            priority = LOGIN_PRIORITY,
            handler = processor::process,
        )
    }

    private companion object {
        const val LOGIN_PRIORITY = -1_000
    }
}
