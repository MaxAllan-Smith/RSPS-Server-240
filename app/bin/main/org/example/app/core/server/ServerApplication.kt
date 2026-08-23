package org.example.app.core.server

import org.example.app.core.cache.CacheBootstrap
import org.example.app.core.cache.CacheTarget
import org.example.app.core.cache.HuffmanLoader
import org.example.app.core.cache.OpenRs2ArchiveClient
import org.example.app.core.cache.PreparedCache
import org.example.app.core.cache.RsProtJs5Provider
import org.example.app.core.config.ServerConfig
import org.example.app.core.engine.GameContext
import org.example.app.core.engine.GameEngine
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistry
import org.example.app.core.network.RsProtNetworkFactory
import org.example.app.core.player.PlayerManager
import org.example.app.core.protocol.RsProtInfoSynchronizer
import org.example.app.core.security.RsaKeyManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application lifecycle/composition boundary.
 *
 * The core receives a list of [Feature] implementations but never imports any
 * concrete feature. Adding or removing a feature only changes FeatureCatalog.
 */
class ServerApplication(
    private val config: ServerConfig,
    private val features: List<Feature>,
) {

    private val shuttingDown =
        AtomicBoolean(false)

    private val shutdownLatch =
        CountDownLatch(1)

    /**
     * RSProt's NetworkService.start() is currently marked with both
     * ExperimentalUnsignedTypes and ExperimentalStdlibApi.
     *
     * Keep the opt-in scoped to this application lifecycle boundary rather
     * than enabling experimental APIs for the entire project.
     */
    @OptIn(
        ExperimentalUnsignedTypes::class,
        ExperimentalStdlibApi::class,
    )
    fun run() {
        val preparedCache =
            prepareCache()

        val rsa =
            prepareRsa()

        val huffman =
            HuffmanLoader.load(
                preparedCache.directory
            )

        val js5Provider =
            RsProtJs5Provider.open(
                preparedCache.directory
            )

        try {
            val featureRuntime =
                FeatureRegistry()
                    .install(features)

            println(
                "[Features] Installed: " +
                        featureRuntime.featureIds.joinToString()
            )

            val networkService =
                RsProtNetworkFactory(
                    config = config,
                    rsaKey = rsa.rsProtKey,
                    huffmanProvider = huffman,
                    js5Provider = js5Provider,
                    features = featureRuntime,
                ).build()

            val playerManager =
                PlayerManager(
                    networkService
                )

            val context =
                GameContext(
                    networkService,
                    playerManager,
                )

            val infoSynchronizer =
                RsProtInfoSynchronizer(
                    playerManager
                )

            val gameEngine =
                GameEngine(
                    context = context,
                    features = featureRuntime,
                    infoSynchronizer = infoSynchronizer,
                    cycleMillis = config.gameCycleMillis,
                )

            try {
                println()
                println(
                    "[Server] Starting RSProt..."
                )

                networkService.start()
                gameEngine.start()
            } catch (t: Throwable) {
                try {
                    gameEngine.close()
                } catch (_: Throwable) {
                    // Best-effort cleanup.
                }

                try {
                    networkService.shutdownNow()
                } catch (_: Throwable) {
                    // Best-effort cleanup.
                }

                throw t
            }

            installShutdownHook(
                gameEngine = gameEngine,
                networkShutdown =
                    networkService::shutdownNow,
                js5Provider = js5Provider,
            )

            printOnlineSummary(
                preparedCache
            )

            println()
            println(
                "Press Ctrl+C to stop."
            )

            shutdownLatch.await()
        } catch (t: Throwable) {
            js5Provider.close()
            throw t
        }
    }

    private fun prepareCache():
            PreparedCache {

        val target =
            CacheTarget(
                major =
                    config.protocolRevision,
                minor =
                    config.cacheMinorRevision,
                windowStart =
                    config.patchWindowStart,
                windowEndExclusive =
                    config.patchWindowEndExclusive,
            )

        return CacheBootstrap(
            archiveClient =
                OpenRs2ArchiveClient(),
            cacheRoot =
                config.cacheRootDirectory,
            cacheDirectory =
                config.cacheDirectory,
        ).prepare(
            target
        )
    }

    private fun prepareRsa() =
        RsaKeyManager
            .loadOrCreate(
                privateKeyFile =
                    config.rsaPrivateKey,
                publicInfoFile =
                    config.rsaPublicInfo,
            )
            .also {
                println()

                println(
                    "RSA public information written to: " +
                            config.rsaPublicInfo
                                .toAbsolutePath()
                )
            }

    private fun installShutdownHook(
        gameEngine: GameEngine,
        networkShutdown: () -> Unit,
        js5Provider: RsProtJs5Provider,
    ) {
        Runtime
            .getRuntime()
            .addShutdownHook(
                Thread(
                    {
                        if (
                            shuttingDown.compareAndSet(
                                false,
                                true,
                            )
                        ) {
                            println()
                            println(
                                "[Server] Shutting down..."
                            )

                            try {
                                gameEngine.close()
                            } finally {
                                try {
                                    networkShutdown()
                                } finally {
                                    js5Provider.close()
                                }
                            }

                            println(
                                "[Server] Shutdown complete."
                            )

                            shutdownLatch.countDown()
                        }
                    },
                    "server-shutdown",
                )
            )
    }

    private fun printOnlineSummary(
        preparedCache: PreparedCache,
    ) {
        println()
        println(
            "======================================"
        )
        println(
            "       RSProt server is online"
        )
        println(
            "======================================"
        )
        println(
            "Protocol revision : " +
                    config.protocolRevision
        )
        println(
            "Client patch      : " +
                    config.clientPatch
        )
        println(
            "Host              : " +
                    config.host
        )
        println(
            "Port              : " +
                    config.port
        )
        println(
            "OpenRS2 cache id  : " +
                    preparedCache.metadata.id
        )
        println(
            "Cache timestamp   : " +
                    preparedCache.metadata.timestamp
        )
        println(
            "Cache directory   : " +
                    preparedCache.directory
                        .toAbsolutePath()
        )
        println(
            "Game cycle        : " +
                    "${config.gameCycleMillis}ms"
        )
        println(
            "======================================"
        )
    }
}