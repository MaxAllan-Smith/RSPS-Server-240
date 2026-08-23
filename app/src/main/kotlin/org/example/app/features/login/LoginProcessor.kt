package org.example.app.features.login

import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.info.Infos
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import net.rsprot.protocol.loginprot.outgoing.util.AuthenticatorResponse
import org.example.app.core.engine.GameContext
import org.example.app.core.network.LoginAttempt
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

internal class LoginProcessor(
    private val queue: LoginQueue,
) {
    fun process(context: GameContext) {
        while (true) {
            val attempt = queue.poll() ?: break
            accept(context, attempt)
        }
    }

    private fun accept(
        context: GameContext,
        attempt: LoginAttempt,
    ) {
        val username = attempt.block.username
        val responseHandler = attempt.responseHandler

        if (!responseHandler.ctx.channel().isActive) {
            return
        }

        if (context.players.isOnline(username)) {
            println("[Login] Duplicate login rejected: '$username'")
            responseHandler.writeFailedResponse(LoginResponse.Duplicate)
            return
        }

        val index = context.players.nextFreeIndex()

        if (index == null) {
            println("[Login] World full; rejecting '$username'.")
            responseHandler.writeFailedResponse(LoginResponse.ServerFull)
            return
        }

        var infos: Infos? = null

        try {
            infos =
                context.networkService.infoProtocols.alloc(
                    index,
                    OldSchoolClientType.DESKTOP,
                )

            val position = WorldPosition.LUMBRIDGE

            infos.updateRootCoord(
                position.level,
                position.x,
                position.z,
            )
            infos.updateRootBuildAreaCenteredOnPlayer(
                position.x,
                position.z,
            )

            DefaultAppearance.apply(infos.playerInfo, username)

            val identity = LocalIdentity.forUsername(username)
            val response =
                LoginResponse.Ok(
                    authenticatorResponse =
                        AuthenticatorResponse.NoAuthenticator,
                    staffModLevel = 0,
                    playerMod = false,
                    index = index,
                    member = true,
                    accountHash = identity.accountHash,
                    userId = identity.userId,
                    userHash = identity.userHash,
                )

            val session =
                responseHandler.writeSuccessfulResponse(
                    response,
                    attempt.block,
                )

            val player =
                Player(
                    username = username,
                    index = index,
                    session = session,
                    infos = infos,
                    position = position,
                    resizable = attempt.block.resizable,
                )

            if (!context.players.add(player)) {
                return
            }

            println(
                "[Login] Accepted '$username' " +
                    "index=$index " +
                    "position=${position.x},${position.z},${position.level}"
            )
        } catch (t: Throwable) {
            if (infos != null) {
                try {
                    context.networkService.infoProtocols.dealloc(infos)
                } catch (cleanupFailure: Throwable) {
                    t.addSuppressed(cleanupFailure)
                }
            }

            System.err.println("[Login] Failed to accept '$username'.")
            t.printStackTrace()

            if (responseHandler.ctx.channel().isActive) {
                try {
                    responseHandler.ctx.close()
                } catch (_: Throwable) {
                    // Pipeline may already be tearing down.
                }
            }
        }
    }
}
