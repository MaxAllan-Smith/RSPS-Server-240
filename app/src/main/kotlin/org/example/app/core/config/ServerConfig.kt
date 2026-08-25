package org.example.app.core.config

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

/**
 * Process-level server configuration.
 *
 * Infrastructure and globally-tunable gameplay timings live here so they can
 * be changed from one composition boundary rather than being scattered through
 * gameplay features.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val protocolRevision: Int,
    val clientPatch: String,
    val cacheMinorRevision: Int,
    val patchWindowStart: Instant,
    val patchWindowEndExclusive: Instant,
    val dataDirectory: Path,
    val gameCycleMillis: Long,

    /**
     * Generic world-item lifetime.
     *
     * This applies to ordinary dropped items and also to ashes produced by
     * player-made fires.
     */
    val groundItemDespawnTicks: Int,

    /**
     * Number of game cycles between Firemaking ignition rolls.
     */
    val firemakingRollIntervalTicks: Int,

    /**
     * Player-made fire lifetime is randomized inclusively between these two
     * values.
     */
    val fireLifetimeMinTicks: Int,
    val fireLifetimeMaxTicks: Int,
) {
    init {
        require(
            gameCycleMillis > 0L
        ) {
            "Game cycle duration must be positive."
        }

        require(
            groundItemDespawnTicks > 0
        ) {
            "Ground-item despawn time must be positive."
        }

        require(
            firemakingRollIntervalTicks > 0
        ) {
            "Firemaking roll interval must be positive."
        }

        require(
            fireLifetimeMinTicks > 0
        ) {
            "Minimum fire lifetime must be positive."
        }

        require(
            fireLifetimeMaxTicks >=
                fireLifetimeMinTicks
        ) {
            "Maximum fire lifetime must be >= minimum fire lifetime."
        }
    }

    val cacheRootDirectory: Path =
        dataDirectory.resolve(
            "cache"
        )

    val cacheDirectory: Path =
        cacheRootDirectory.resolve(
            "osrs-$clientPatch"
        )

    val rsaDirectory: Path =
        dataDirectory.resolve(
            "keys"
        )

    val rsaPrivateKey: Path =
        rsaDirectory.resolve(
            "game-private.pem"
        )

    val rsaPublicInfo: Path =
        rsaDirectory.resolve(
            "game-public.txt"
        )

    val databaseDirectory: Path =
        dataDirectory.resolve(
            "database"
        )

    val databaseFile: Path =
        databaseDirectory.resolve(
            "server.sqlite"
        )

    companion object {

        fun load(): ServerConfig {
            val dataDirectory =
                System.getProperty(
                    "rsps.data.dir"
                )
                    ?.takeIf(
                        String::isNotBlank
                    )
                    ?.let(
                        Paths::get
                    )
                    ?.toAbsolutePath()
                    ?.normalize()
                    ?: Paths.get(
                        ".data"
                    )
                        .toAbsolutePath()
                        .normalize()

            return ServerConfig(
                host =
                    "127.0.0.1",

                port =
                    43594,

                protocolRevision =
                    240,

                clientPatch =
                    "240.3",

                cacheMinorRevision =
                    3,

                patchWindowStart =
                    Instant.parse(
                        "2026-08-06T00:00:00Z"
                    ),

                patchWindowEndExclusive =
                    Instant.parse(
                        "2026-08-17T00:00:00Z"
                    ),

                dataDirectory =
                    dataDirectory,

                gameCycleMillis =
                    600L,

                /*
                 * 100 cycles x 600 ms = 60 seconds.
                 *
                 * Ashes use this same generic ground-item lifetime.
                 */
                groundItemDespawnTicks =
                    100,

                /*
                 * Four cycles = 2.4 seconds between failed ignition rolls.
                 */
                firemakingRollIntervalTicks =
                    4,

                /*
                 * Player-made fires now live for roughly 108-180 seconds.
                 *
                 * Each individual fire receives a random duration within this
                 * range.
                 */
                fireLifetimeMinTicks =
                    180,

                fireLifetimeMaxTicks =
                    300,
            )
        }
    }
}