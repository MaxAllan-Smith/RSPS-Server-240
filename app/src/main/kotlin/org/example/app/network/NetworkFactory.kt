@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.app.network

import net.rsprot.compression.provider.HuffmanCodecProvider
import net.rsprot.crypto.rsa.RsaKeyPair
import net.rsprot.crypto.xtea.XteaKey
import net.rsprot.protocol.api.AbstractNetworkServiceFactory
import net.rsprot.protocol.api.GameConnectionHandler
import net.rsprot.protocol.api.js5.Js5GroupProvider
import net.rsprot.protocol.api.login.GameLoginResponseHandler
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.loginprot.incoming.util.AuthenticationType
import net.rsprot.protocol.loginprot.incoming.util.LoginBlock
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import net.rsprot.protocol.message.codec.incoming.GameMessageConsumerRepository
import net.rsprot.protocol.message.codec.incoming.provider.GameMessageConsumerRepositoryProvider
import org.example.app.config.ServerConfig

class NetworkFactory(
    private val rsaKey: RsaKeyPair,
    private val huffmanProvider: HuffmanCodecProvider,
    private val js5Provider: Js5GroupProvider,
) : AbstractNetworkServiceFactory<Unit>() {

    override val host: String =
        ServerConfig.HOST

    override val ports: List<Int> =
        listOf(ServerConfig.PORT)

    override val supportedClientTypes:
        List<OldSchoolClientType> =
        listOf(
            OldSchoolClientType.DESKTOP
        )

    private val gameConsumers =
        GameMessageConsumerRepository<Unit>(
            consumers = emptyMap(),
            globalConsumers = emptyList(),
        )

    override fun getRsaKeyPair(): RsaKeyPair {
        return rsaKey
    }

    override fun getHuffmanCodecProvider():
        HuffmanCodecProvider {
        return huffmanProvider
    }

    override fun getJs5GroupProvider():
        Js5GroupProvider {
        return js5Provider
    }

    override fun getGameMessageConsumerRepositoryProvider():
        GameMessageConsumerRepositoryProvider<Unit> {
        return GameMessageConsumerRepositoryProvider {
            gameConsumers
        }
    }

    override fun getGameConnectionHandler():
        GameConnectionHandler<Unit> {
        return object : GameConnectionHandler<Unit> {

            override fun onLogin(
                responseHandler:
                    GameLoginResponseHandler<Unit>,
                block:
                    LoginBlock<AuthenticationType>,
            ) {
                /*
                 * Reaching this method is already a major milestone:
                 *
                 * - TCP connected
                 * - revision accepted
                 * - login protocol decoded
                 * - proof-of-work handled
                 * - RSA block decrypted successfully
                 */
                println(
                    "[Login] Successfully decoded login request from " +
                        responseHandler.ctx
                            .channel()
                            .remoteAddress()
                )

                /*
                 * We don't have Player/session/world state yet.
                 *
                 * Give the client a valid protocol response instead of
                 * pretending a complete login succeeded.
                 */
                responseHandler.writeFailedResponse(
                    LoginResponse.UpdateInProgress
                )
            }

            override fun onReconnect(
                responseHandler:
                    GameLoginResponseHandler<Unit>,
                block:
                    LoginBlock<XteaKey>,
            ) {
                println(
                    "[Login] Reconnect request from " +
                        responseHandler.ctx
                            .channel()
                            .remoteAddress()
                )

                responseHandler.writeFailedResponse(
                    LoginResponse.UpdateInProgress
                )
            }
        }
    }
}