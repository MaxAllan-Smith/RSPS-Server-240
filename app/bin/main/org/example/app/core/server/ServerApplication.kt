package org.example.app.core.server

import org.example.app.core.cache.*
import org.example.app.core.config.ServerConfig
import org.example.app.core.engine.GameContext
import org.example.app.core.engine.GameEngine
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistry
import org.example.app.core.network.RsProtNetworkFactory
import org.example.app.core.player.PlayerManager
import org.example.app.core.protocol.RsProtInfoSynchronizer
import org.example.app.core.security.RsaKeyManager
import org.example.app.core.vars.VarbitDefinitionRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application lifecycle/composition boundary.
 *
 * The core receives [Feature] implementations but never imports concrete
 * features. Adding or removing features only changes FeatureCatalog.
 */
class ServerApplication(
    private val config: ServerConfig,
    private val features: List<Feature>,
) {
    private val shuttingDown = AtomicBoolean(false)
    private val shutdownLatch = CountDownLatch(1)

    /**
     * Keep RSProt experimental API opt-ins scoped to the application boundary.
     */
    @OptIn(
        ExperimentalUnsignedTypes::class,
        ExperimentalStdlibApi::class,
    )
    fun run() {
        val cache = prepareCache()
        val rsa = prepareRsa()
        val huffman = HuffmanLoader.load(cache.directory)
        val js5 = RsProtJs5Provider.open(cache.directory)

        try {
            val featureRuntime = FeatureRegistry().install(features)

            println("[Features] Installed: ${featureRuntime.featureIds.joinToString()}")

            val networkService = RsProtNetworkFactory(
                config = config,
                rsaKey = rsa.rsProtKey,
                huffmanProvider = huffman,
                js5Provider = js5,
                features = featureRuntime,
            ).build()

            val playerManager = PlayerManager(networkService)

            val varbitDefinitions = VarbitDefinitionRepository(
                cache.directory
            )

            val context = GameContext(
                networkService = networkService,
                players = playerManager,
                varbits = varbitDefinitions

            )

            val engine = GameEngine(
                context = context,
                features = featureRuntime,
                infoSynchronizer = RsProtInfoSynchronizer(playerManager),
                cycleMillis = config.gameCycleMillis,
            )

            try {
                println("\n[Server] Starting RSProt...")
                networkService.start()
                engine.start()
            } catch (t: Throwable) {
                runCatching { engine.close() }
                runCatching { networkService.shutdownNow() }
                throw t
            }

            installShutdownHook(
                gameEngine = engine,
                networkShutdown = networkService::shutdownNow,
                js5Provider = js5,
            )

            printOnlineSummary(cache)

            println("\nPress Ctrl+C to stop.")
            shutdownLatch.await()
        } catch (t: Throwable) {
            js5.close()
            throw t
        }
    }

    private fun prepareCache(): PreparedCache {
        val target = CacheTarget(
            major = config.protocolRevision,
            minor = config.cacheMinorRevision,
            windowStart = config.patchWindowStart,
            windowEndExclusive = config.patchWindowEndExclusive,
        )

        return CacheBootstrap(
            archiveClient = OpenRs2ArchiveClient(),
            cacheRoot = config.cacheRootDirectory,
            cacheDirectory = config.cacheDirectory,
        ).prepare(target)
    }

    private fun prepareRsa() =
        RsaKeyManager.loadOrCreate(
            privateKeyFile = config.rsaPrivateKey,
            publicInfoFile = config.rsaPublicInfo,
        ).also {
            println(
                "\nRSA public information written to: " +
                        config.rsaPublicInfo.toAbsolutePath()
            )
        }

    private fun installShutdownHook(
        gameEngine: GameEngine,
        networkShutdown: () -> Unit,
        js5Provider: RsProtJs5Provider,
    ) {
        Runtime.getRuntime().addShutdownHook(
            Thread(
                {
                    if (!shuttingDown.compareAndSet(false, true)) return@Thread

                    println("\n[Server] Shutting down...")

                    try {
                        gameEngine.close()
                    } finally {
                        js5Provider.use { js5Provider ->
                            networkShutdown()
                        }
                    }

                    println("[Server] Shutdown complete.")
                    shutdownLatch.countDown()
                },
                "server-shutdown",
            )
        )
    }

    private fun printOnlineSummary(cache: PreparedCache) {
        println(
            """
            
            ======================================
                   RSProt server is online
            ======================================
            Protocol revision : ${config.protocolRevision}
            Client patch      : ${config.clientPatch}
            Host              : ${config.host}
            Port              : ${config.port}
            OpenRS2 cache id  : ${cache.metadata.id}
            Cache timestamp   : ${cache.metadata.timestamp}
            Cache directory   : ${cache.directory.toAbsolutePath()}
            Game cycle        : ${config.gameCycleMillis}ms
            ======================================
            """.trimIndent()
        )
    }
}