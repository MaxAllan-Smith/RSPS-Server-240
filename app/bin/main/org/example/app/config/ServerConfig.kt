package org.example.app.config

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

object ServerConfig {

    const val HOST: String = "127.0.0.1"
    const val PORT: Int = 43594
    const val PROTOCOL_REVISION: Int = 240
    const val CLIENT_PATCH: String = "240.3"

    val PATCH_WINDOW_START: Instant =
        Instant.parse(
            "2026-08-06T00:00:00Z"
        )

    val PATCH_WINDOW_END_EXCLUSIVE: Instant =
        Instant.parse(
            "2026-08-17T00:00:00Z"
        )

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