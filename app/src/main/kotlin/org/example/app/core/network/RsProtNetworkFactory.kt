@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.app.core.network

import net.rsprot.compression.provider.HuffmanCodecProvider
import net.rsprot.crypto.rsa.RsaKeyPair
import net.rsprot.protocol.api.AbstractNetworkServiceFactory
import net.rsprot.protocol.api.GameConnectionHandler
import net.rsprot.protocol.api.js5.Js5GroupProvider
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.message.codec.incoming.provider.GameMessageConsumerRepositoryProvider
import org.example.app.core.config.ServerConfig
import org.example.app.core.feature.FeatureRuntime
import org.example.app.core.player.Player

/** Builds and configures the RSProt network service (login, JS5, huffman, game message dispatch) from [ServerConfig] and the installed [org.example.app.core.feature.FeatureRuntime]. */
class RsProtNetworkFactory(
    config: ServerConfig,
    private val rsaKey: RsaKeyPair,
    private val huffmanProvider: HuffmanCodecProvider,
    private val js5Provider: Js5GroupProvider,
    private val features: FeatureRuntime,
) : AbstractNetworkServiceFactory<Player>() {
    private val connectionHandler = RsProtConnectionHandler(features)

    override val host: String = config.host

    override val ports: List<Int> = listOf(config.port)

    override val supportedClientTypes: List<OldSchoolClientType> =
        listOf(OldSchoolClientType.DESKTOP)

    override fun getRsaKeyPair(): RsaKeyPair = rsaKey

    override fun getHuffmanCodecProvider(): HuffmanCodecProvider =
        huffmanProvider

    override fun getJs5GroupProvider(): Js5GroupProvider = js5Provider

    override fun getGameMessageConsumerRepositoryProvider():
        GameMessageConsumerRepositoryProvider<Player> {
        return GameMessageConsumerRepositoryProvider {
            features.gameMessages
        }
    }

    override fun getGameConnectionHandler(): GameConnectionHandler<Player> =
        connectionHandler
}
