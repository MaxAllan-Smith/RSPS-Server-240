@file:OptIn(
    kotlin.ExperimentalUnsignedTypes::class,
    ExperimentalStdlibApi::class,
)

package org.example.app

import org.example.app.core.config.ServerConfig
import org.example.app.core.server.ServerApplication
import org.example.app.features.FeatureCatalog

fun main() {
    val config = ServerConfig.load()

    printBanner(config)

    ServerApplication(
        config = config,
        features = FeatureCatalog.all,
    ).run()
}

private fun printBanner(config: ServerConfig) {
    println(
        """
        ======================================
                 RSProt OSRS Server
        ======================================
        Target protocol : ${config.protocolRevision}
        Client patch    : ${config.clientPatch}
        Cache source    : OpenRS2 Archive
        Architecture    : Vertical slices
        ======================================
        """.trimIndent()
    )

    println()
}
