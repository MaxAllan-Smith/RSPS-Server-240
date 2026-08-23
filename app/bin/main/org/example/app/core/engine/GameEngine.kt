package org.example.app.core.engine

import org.example.app.core.feature.FeatureRuntime
import org.example.app.core.player.Player
import org.example.app.core.protocol.RsProtInfoSynchronizer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stable single-threaded game-cycle coordinator.
 *
 * Gameplay is not implemented here. Features plug into the defined lifecycle
 * phases through FeatureRegistrar while RSProt packet processing/info building
 * remains centralized and ordered correctly.
 */
class GameEngine(
    private val context: GameContext,
    private val features: FeatureRuntime,
    private val infoSynchronizer: RsProtInfoSynchronizer,
    private val cycleMillis: Long,
) : AutoCloseable {
    private val running = AtomicBoolean(false)

    private val executor =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "game-engine").apply {
                isDaemon = false
            }
        }

    private var cycleTask: ScheduledFuture<*>? = null
    private var communicationThreadBound = false

    fun start() {
        check(running.compareAndSet(false, true)) {
            "Game engine already started."
        }

        cycleTask =
            executor.scheduleAtFixedRate(
                ::runCycleSafely,
                0L,
                cycleMillis,
                TimeUnit.MILLISECONDS,
            )

        println("[Engine] Game engine started (${cycleMillis}ms cycle).")
    }

    private fun runCycleSafely() {
        if (!running.get()) {
            return
        }

        try {
            bindCommunicationThread()
            cycle()
        } catch (t: Throwable) {
            System.err.println("[Engine] Unhandled exception in game cycle:")
            t.printStackTrace()
        }
    }

    private fun bindCommunicationThread() {
        if (communicationThreadBound) {
            return
        }

        context.networkService.setCommunicationThread(
            Thread.currentThread(),
            warnOnError = false,
        )

        communicationThreadBound = true

        println(
            "[Engine] RSProt communication thread = " +
                Thread.currentThread().name
        )
    }

    private fun cycle() {
        // 1. Apply Netty disconnect callbacks on the game thread.
        context.players.processDisconnections()

        // 2. World-level feature work, e.g. accepting queued logins.
        features.cycleStart(context)

        val activePlayers = context.players.snapshot()
        if (activePlayers.isEmpty()) {
            return
        }

        // 3. RSProt decodes only packets with registered feature consumers.
        forEachConnected(activePlayers) { player ->
            player.session.processIncomingPackets(player)
        }

        // 4. Keep RSProt's root coordinate in sync with game state.
        forEachConnected(activePlayers) { player ->
            val position = player.position
            player.infos.updateRootCoord(
                position.level,
                position.x,
                position.z,
            )
        }

        // 5. Feature packets/state that must precede infoProtocols.update().
        forEachConnected(activePlayers) { player ->
            features.beforeInfoUpdate(context, player)
        }

        // 6. Expensive RSProt player/NPC/world-entity info build, once per tick.
        context.networkService.infoProtocols.update()

        // 7. Queue generic RSProt information output.
        forEachConnected(activePlayers) { player ->
            infoSynchronizer.queue(player)
        }

        // 8. Feature output that should be sent after info packets.
        forEachConnected(activePlayers) { player ->
            features.afterInfoUpdate(context, player)
        }

        // 9. Flush once after every core/feature packet for this cycle is queued.
        forEachConnected(activePlayers) { player ->
            player.session.flush()
        }
    }

    private inline fun forEachConnected(
        players: List<Player>,
        action: (Player) -> Unit,
    ) {
        for (player in players) {
            if (!player.isDisconnected) {
                action(player)
            }
        }
    }

    override fun close() {
        if (!running.compareAndSet(true, false)) {
            return
        }

        cycleTask?.cancel(false)

        val cleanup =
            executor.submit {
                bindCommunicationThread()
                context.players.closeAll()
            }

        try {
            cleanup.get(5L, TimeUnit.SECONDS)
        } catch (t: Throwable) {
            System.err.println("[Engine] Error while shutting down.")
            t.printStackTrace()
        } finally {
            executor.shutdown()

            try {
                executor.awaitTermination(5L, TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        println("[Engine] Game engine stopped.")
    }
}
