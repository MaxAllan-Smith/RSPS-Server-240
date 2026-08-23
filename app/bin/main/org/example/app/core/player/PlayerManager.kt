package org.example.app.core.player

import net.rsprot.protocol.api.NetworkService
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Owns the boundary between Netty disconnect callbacks and game-thread player
 * state. Repository mutation and RSProt info deallocation happen only on the
 * game/communication thread.
 */
class PlayerManager(
    private val networkService: NetworkService<Player>,
    private val repository: PlayerRepository = PlayerRepository(),
) {
    private val disconnectedPlayers = ConcurrentLinkedQueue<Player>()

    fun nextFreeIndex(): Int? = repository.nextFreeIndex()

    fun isOnline(username: String): Boolean = repository.isOnline(username)

    fun snapshot(): List<Player> = repository.snapshot()

    val size: Int
        get() = repository.size

    /**
     * Attaches the RSProt disconnect hook and publishes the player into the
     * repository. Returns false only when the connection disappeared before
     * the player could be published.
     */
    fun add(player: Player): Boolean {
        player.session.setDisconnectionHook {
            if (player.markDisconnected()) {
                disconnectedPlayers.offer(player)
            }
        }

        if (player.isDisconnected) {
            networkService.infoProtocols.dealloc(player.infos)
            return false
        }

        repository.add(player)
        return true
    }

    fun disconnect(
        player: Player,
        reason: String,
    ) {
        if (!player.markDisconnected()) {
            return
        }

        println("[Logout] ${player.username}: $reason")
        player.session.requestClose()
        disconnectedPlayers.offer(player)
    }

    fun processDisconnections() {
        while (true) {
            val player = disconnectedPlayers.poll() ?: break

            if (!repository.remove(player)) {
                continue
            }

            networkService.infoProtocols.dealloc(player.infos)

            println(
                "[Logout] Removed '${player.username}' " +
                    "index=${player.index}. Online=$size"
            )
        }
    }

    fun closeAll() {
        for (player in repository.snapshot()) {
            try {
                player.session.requestClose()
            } finally {
                try {
                    networkService.infoProtocols.dealloc(player.infos)
                } finally {
                    repository.remove(player)
                }
            }
        }

        disconnectedPlayers.clear()
    }
}
