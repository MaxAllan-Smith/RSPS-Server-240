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
import org.example.app.core.feature.FeatureDependencies
import org.example.app.core.feature.FeatureRegistry
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.items.SqliteItemDefinitionSource
import org.example.app.core.items.wiki.WikiItemDataClient
import org.example.app.core.items.wiki.WikiItemDataRepository
import org.example.app.core.items.wiki.WikiItemDataWorker
import org.example.app.core.network.RsProtNetworkFactory
import org.example.app.core.persistence.PlayerPersistenceRepository
import org.example.app.core.persistence.SqliteDatabase
import org.example.app.core.player.PlayerManager
import org.example.app.core.protocol.RsProtInfoSynchronizer
import org.example.app.core.security.RsaKeyManager
import org.example.app.core.vars.VarbitDefinitionRepository
import org.example.app.core.world.WorldCollision
import org.example.app.core.world.collision.PrecomputedCollisionLoader
import org.example.app.core.world.collision.RemoteCollisionMapProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application lifecycle and dependency-composition boundary.
 *
 * Concrete features remain outside core. This class creates infrastructure,
 * supplies feature dependencies and owns orderly startup/shutdown.
 */
class ServerApplication(
    private val config: ServerConfig,

    private val featureFactory:
        (FeatureDependencies) -> List<Feature>,
) {

    private val shuttingDown =
        AtomicBoolean(
            false
        )

    private val shutdownLatch =
        CountDownLatch(
            1
        )

    @OptIn(
        ExperimentalUnsignedTypes::class,
        ExperimentalStdlibApi::class,
    )
    fun run() {
        val cache =
            prepareCache()

        val collision =
            prepareCollision()

        val rsa =
            prepareRsa()

        val database =
            SqliteDatabase(
                file =
                    config.databaseFile,
            )

        val persistence =
            PlayerPersistenceRepository(
                database =
                    database,
            )

        val itemDefinitions =
            ItemDefinitionRepository(
                source =
                    SqliteItemDefinitionSource(
                        database =
                            database,
                    ),
            )

        val wikiItemDataWorker =
            WikiItemDataWorker(
                client =
                    WikiItemDataClient(
                        baseUrl =
                            WIKI_ITEM_API_BASE,

                        userAgent =
                            WIKI_USER_AGENT,
                    ),

                repository =
                    WikiItemDataRepository(
                        database =
                            database,
                    ),
            )

        val huffman =
            HuffmanLoader.load(
                cache.directory
            )

        val js5 =
            RsProtJs5Provider.open(
                cache.directory
            )

        try {
            val features =
                featureFactory(
                    FeatureDependencies(
                        config =
                            config,

                        itemDefinitions =
                            itemDefinitions,

                        collision =
                            collision,
                    )
                )

            val featureRuntime =
                FeatureRegistry()
                    .install(
                        features
                    )

            println(
                "[Features] Installed: " +
                    featureRuntime
                        .featureIds
                        .joinToString()
            )

            val networkService =
                RsProtNetworkFactory(
                    config =
                        config,

                    rsaKey =
                        rsa.rsProtKey,

                    huffmanProvider =
                        huffman,

                    js5Provider =
                        js5,

                    features =
                        featureRuntime,
                )
                    .build()

            val playerManager =
                PlayerManager(
                    networkService =
                        networkService,

                    persistence =
                        persistence,
                )

            val varbitDefinitions =
                VarbitDefinitionRepository(
                    cache.directory
                )

            val context =
                GameContext(
                    networkService =
                        networkService,

                    players =
                        playerManager,

                    varbits =
                        varbitDefinitions,

                    persistence =
                        persistence,

                    itemDefinitions =
                        itemDefinitions,

                    collision =
                        collision,

                    cacheDirectory =
                        cache.directory,
                )

            val engine =
                GameEngine(
                    context =
                        context,

                    features =
                        featureRuntime,

                    infoSynchronizer =
                        RsProtInfoSynchronizer(
                            playerManager
                        ),

                    cycleMillis =
                        config.gameCycleMillis,
                )

            try {
                println(
                    "\n[Server] Starting RSProt..."
                )

                networkService.start()

                engine.start()

                wikiItemDataWorker.start()
            } catch (
                t: Throwable
            ) {
                runCatching {
                    wikiItemDataWorker.close()
                }

                runCatching {
                    engine.close()
                }

                runCatching {
                    networkService.shutdownNow()
                }

                throw t
            }

            installShutdownHook(
                gameEngine =
                    engine,

                networkShutdown =
                    networkService::shutdownNow,

                js5Provider =
                    js5,

                wikiItemDataWorker =
                    wikiItemDataWorker,
            )

            printOnlineSummary(
                cache
            )

            println(
                "\nPress Ctrl+C to stop."
            )

            shutdownLatch.await()
        } catch (
            t: Throwable
        ) {
            runCatching {
                wikiItemDataWorker.close()
            }

            js5.close()

            throw t
        }
    }

    private fun prepareCollision():
        WorldCollision {

        val collision =
            WorldCollision()

        PrecomputedCollisionLoader(
            provider =
                RemoteCollisionMapProvider(
                    file =
                        config.dataDirectory
                            .resolve(
                                "collision"
                            )
                            .resolve(
                                "collision-map-2026-08-13.zip"
                            ),
                ),
        )
            .loadInto(
                collision
            )

        return collision
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
        )
            .prepare(
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
                println(
                    "\nRSA public information written to: " +
                        config.rsaPublicInfo
                            .toAbsolutePath()
                )
            }

    private fun installShutdownHook(
        gameEngine: GameEngine,
        networkShutdown: () -> Unit,
        js5Provider: RsProtJs5Provider,
        wikiItemDataWorker: WikiItemDataWorker,
    ) {
        Runtime
            .getRuntime()
            .addShutdownHook(
                Thread(
                    {
                        if (
                            !shuttingDown.compareAndSet(
                                false,
                                true,
                            )
                        ) {
                            return@Thread
                        }

                        println(
                            "\n[Server] Shutting down..."
                        )

                        try {
                            wikiItemDataWorker.close()

                            gameEngine.close()
                        } finally {
                            js5Provider.use {
                                networkShutdown()
                            }
                        }

                        println(
                            "[Server] Shutdown complete."
                        )

                        shutdownLatch.countDown()
                    },

                    "server-shutdown",
                )
            )
    }

    private fun printOnlineSummary(
        cache: PreparedCache,
    ) {
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
            Database          : ${config.databaseFile.toAbsolutePath()}
            Collision         : RSMod Routefinder + pinned precomputed OSRS map
            Game cycle        : ${config.gameCycleMillis}ms
            Ground items      : ${config.groundItemDespawnTicks} ticks
            Fire lifetime     : ${config.fireLifetimeMinTicks}..${config.fireLifetimeMaxTicks} ticks
            ======================================
            """.trimIndent()
        )
    }

    private companion object {

        const val WIKI_ITEM_API_BASE: String =
            "https://prices.runescape.wiki/api/v1/osrs"

        const val WIKI_USER_AGENT: String =
            "RSPS_RSProt_Server item-data-sync"
    }
}