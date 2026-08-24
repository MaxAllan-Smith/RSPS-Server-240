package org.example.app.core.config

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

/**
 * Process-level server configuration.
 *
 * Feature-specific configuration belongs inside the feature that owns it.
 * Keeping this class limited to infrastructure settings prevents the core
 * configuration object from becoming a dumping ground as the game grows.
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
) {
    val cacheRootDirectory: Path =
        dataDirectory.resolve("cache")

    val cacheDirectory: Path =
        cacheRootDirectory.resolve("osrs-$clientPatch")

    val rsaDirectory: Path =
        dataDirectory.resolve("keys")

    val rsaPrivateKey: Path =
        rsaDirectory.resolve("game-private.pem")

    val rsaPublicInfo: Path =
        rsaDirectory.resolve("game-public.txt")

    val databaseDirectory: Path =
        dataDirectory.resolve("database")

    val databaseFile: Path =
        databaseDirectory.resolve(
            "server.sqlite"
        )

    companion object {
        fun load(): ServerConfig {
            val dataDirectory =
                System.getProperty("rsps.data.dir")
                    ?.takeIf(String::isNotBlank)
                    ?.let(Paths::get)
                    ?.toAbsolutePath()
                    ?.normalize()
                    ?: Paths.get(".data")
                        .toAbsolutePath()
                        .normalize()

            return ServerConfig(
                host = "127.0.0.1",
                port = 43594,
                protocolRevision = 240,
                clientPatch = "240.3",
                cacheMinorRevision = 3,
                patchWindowStart =
                    Instant.parse(
                        "2026-08-06T00:00:00Z"
                    ),
                patchWindowEndExclusive =
                    Instant.parse(
                        "2026-08-17T00:00:00Z"
                    ),
                dataDirectory = dataDirectory,
                gameCycleMillis = 600L,
            )
        }
    }
}