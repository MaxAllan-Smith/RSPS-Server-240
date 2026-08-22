@file:OptIn(
    ExperimentalUnsignedTypes::class,
    ExperimentalStdlibApi::class,
)

package org.example.app

import org.example.app.cache.CacheBootstrap
import org.example.app.cache.CacheTarget
import org.example.app.cache.HuffmanLoader
import org.example.app.cache.OpenRs2ArchiveClient
import org.example.app.cache.RsProtJs5Provider
import org.example.app.config.ServerConfig
import org.example.app.crypto.RsaKeyManager
import org.example.app.network.NetworkFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    printBanner()

    val target =
        CacheTarget(
            major =
                ServerConfig.PROTOCOL_REVISION,
            minor = 3,
            windowStart =
                ServerConfig.PATCH_WINDOW_START,
            windowEndExclusive =
                ServerConfig.PATCH_WINDOW_END_EXCLUSIVE,
        )

    /*
     * 1. Resolve and download the correct OpenRS2 cache.
     */
    val preparedCache =
        CacheBootstrap(
            archiveClient =
                OpenRs2ArchiveClient(),
            cacheRoot =
                ServerConfig.CACHE_ROOT_DIRECTORY,
            cacheDirectory =
                ServerConfig.CACHE_DIRECTORY,
        ).prepare(target)

    /*
     * 2. Persistent RSA.
     */
    val rsa =
        RsaKeyManager.loadOrCreate(
            privateKeyFile =
                ServerConfig.RSA_PRIVATE_KEY,
            publicInfoFile =
                ServerConfig.RSA_PUBLIC_INFO,
        )

    println()
    println("RSA public exponent:")
    println(rsa.publicExponent)

    println()
    println("RSA modulus (decimal):")
    println(rsa.modulus)

    println()
    println("RSA modulus (hex):")
    println(rsa.modulus.toString(16))

    println()
    println(
        "RSA public information written to: " +
            ServerConfig.RSA_PUBLIC_INFO
                .toAbsolutePath()
    )

    /*
     * 3. Load the actual Huffman table out of the cache.
     */
    val huffman =
        HuffmanLoader.load(
            preparedCache.directory
        )

    /*
     * 4. Open the cache as a real RSProt JS5 provider.
     */
    val js5Provider =
        RsProtJs5Provider.open(
            preparedCache.directory
        )

    /*
     * 5. Build RSProt.
     */
    val networkService =
        NetworkFactory(
            rsaKey = rsa.rsProtKey,
            huffmanProvider = huffman,
            js5Provider = js5Provider,
        ).build()

    try {
        println()
        println("[Server] Starting RSProt...")

        networkService.start()
    } catch (t: Throwable) {
        js5Provider.close()
        throw t
    }

    val shuttingDown =
        AtomicBoolean(false)

    Runtime.getRuntime().addShutdownHook(
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

                    js5Provider.use { js5Provider ->
                        networkService.shutdownNow()
                    }

                    println(
                        "[Server] Shutdown complete."
                    )
                }
            },
            "server-shutdown",
        )
    )

    println()
    println("======================================")
    println("       RSProt server is online")
    println("======================================")
    println("Protocol revision : ${ServerConfig.PROTOCOL_REVISION}")
    println("Client patch      : ${ServerConfig.CLIENT_PATCH}")
    println("Host              : ${ServerConfig.HOST}")
    println("Port              : ${ServerConfig.PORT}")
    println("OpenRS2 cache id  : ${preparedCache.metadata.id}")
    println("Cache timestamp   : ${preparedCache.metadata.timestamp}")
    println(
        "Cache directory   : " +
            preparedCache.directory.toAbsolutePath()
    )
    println("======================================")
    println()
    println("Press Ctrl+C to stop.")

    /*
     * Keep the process alive.
     *
     * Netty operates on its own threads.
     */
    CountDownLatch(1).await()
}

private fun printBanner() {
    println(
        """
        ======================================
                 RSProt OSRS Server
        ======================================
        Target protocol : ${ServerConfig.PROTOCOL_REVISION}
        Client patch    : ${ServerConfig.CLIENT_PATCH}
        Cache source    : OpenRS2 Archive
        ======================================
        """.trimIndent()
    )

    println()
}