package org.example.app.config

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

object ServerConfig {

    const val HOST: String =
        "127.0.0.1"

    const val PORT: Int =
        43594

    /*
     * RSProt protocol revision.
     */
    const val PROTOCOL_REVISION: Int =
        240

    /*
     * Client patch targeted by this server.
     *
     * RSProt/OpenRS2 use protocol/cache build 240.
     * The ".3" portion is used by our client configuration and
     * cache-selection date boundary.
     */
    const val CLIENT_PATCH: String =
        "240.3"

    /*
     * OpenRS2 does not currently expose these OSRS caches as
     * build 240.3; they are identified as build 240.
     *
     * Restricting the cache catalogue search to this period
     * prevents a later 240.x cache being selected.
     *
     * This currently resolves OpenRS2 cache 2655:
     *
     *     oldschool / live / en
     *     build 240
     *     2026-08-12 10:30:28 UTC
     */
    val PATCH_WINDOW_START: Instant =
        Instant.parse(
            "2026-08-06T00:00:00Z"
        )

    val PATCH_WINDOW_END_EXCLUSIVE: Instant =
        Instant.parse(
            "2026-08-17T00:00:00Z"
        )

    /*
     * Optional override:
     *
     *   -Drsps.data.dir=C:\somewhere\data
     *
     * Normally this resolves to:
     *
     *   RSPS_RSProt_Server/.data
     *
     * because Gradle's run task is configured to use the
     * repository root as its working directory.
     */
    val DATA_DIRECTORY: Path =
        System
            .getProperty("rsps.data.dir")
            ?.takeIf { it.isNotBlank() }
            ?.let(Paths::get)
            ?.toAbsolutePath()
            ?.normalize()
            ?: Paths
                .get(".data")
                .toAbsolutePath()
                .normalize()

    val CACHE_ROOT_DIRECTORY: Path =
        DATA_DIRECTORY.resolve(
            "cache"
        )

    val CACHE_DIRECTORY: Path =
        CACHE_ROOT_DIRECTORY.resolve(
            "osrs-$CLIENT_PATCH"
        )

    val RSA_DIRECTORY: Path =
        DATA_DIRECTORY.resolve(
            "keys"
        )

    val RSA_PRIVATE_KEY: Path =
        RSA_DIRECTORY.resolve(
            "game-private.pem"
        )

    val RSA_PUBLIC_INFO: Path =
        RSA_DIRECTORY.resolve(
            "game-public.txt"
        )
}