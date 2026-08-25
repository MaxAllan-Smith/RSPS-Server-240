package org.example.app.core.config

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

/**
 * Process-level server configuration.
 *
 * Infrastructure and globally-tunable gameplay values live here so gameplay
 * features do not scatter timing/rate constants throughout the codebase.
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

    val groundItemDespawnTicks: Int,

    val firemakingRollIntervalTicks: Int,
    val fireLifetimeMinTicks: Int,
    val fireLifetimeMaxTicks: Int,

    /**
     * Run energy uses hundredths of one percent.
     *
     * 10,000 = 100%.
     *
     * These values are deliberately server-tunable.
     */
    val runEnergyDrainPerRunningCycle: Int,
    val runEnergyRestorePerIdleCycle: Int,
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

        require(
            runEnergyDrainPerRunningCycle > 0
        ) {
            "Run-energy drain must be positive."
        }

        require(
            runEnergyRestorePerIdleCycle >= 0
        ) {
            "Run-energy restoration must not be negative."
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

                groundItemDespawnTicks =
                    100,

                firemakingRollIntervalTicks =
                    4,

                fireLifetimeMinTicks =
                    180,

                fireLifetimeMaxTicks =
                    300,

                /*
                 * Run energy uses 10,000 units for 100%.
                 *
                 * 25 units = 0.25% per 600ms running cycle.
                 *
                 * From full energy this provides roughly four minutes of
                 * uninterrupted running, deliberately longer than standard
                 * game behavior.
                 */
                runEnergyDrainPerRunningCycle =
                    25,

                /*
                 * 15 units = 0.15% restoration per game cycle while the player
                 * is not actually running.
                 *
                 * This is deliberately kept independent from the run toggle:
                 * an enabled orb does not prevent regeneration while standing
                 * still or walking.
                 */
                runEnergyRestorePerIdleCycle =
                    15,
            )
        }
    }
}