package org.example.app.core.engine

import org.example.app.core.feature.FeatureRuntime
import org.example.app.core.player.Player
import org.example.app.core.protocol.RsProtInfoSynchronizer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the fixed-rate game cycle and network pulse scheduling that drives every feature's cycle/info-update hooks. */
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
    private var networkTask: ScheduledFuture<*>? = null
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

        networkTask =
            executor.scheduleWithFixedDelay(
                ::runNetworkPulseSafely,
                0L,
                NETWORK_PULSE_MILLIS,
                TimeUnit.MILLISECONDS,
            )

        println("[Engine] Game engine started (${cycleMillis}ms cycle).")
        println("[Engine] Network pulse = ${NETWORK_PULSE_MILLIS}ms.")
    }

    private fun runNetworkPulseSafely() {
        if (!running.get()) return

        try {
            bindCommunicationThread()
            networkPulse()
        } catch (t: Throwable) {
            System.err.println("[Engine] Unhandled exception in network pulse:")
            t.printStackTrace()
        }
    }

    private fun runCycleSafely() {
        if (!running.get()) return

        try {
            bindCommunicationThread()
            cycle()
        } catch (t: Throwable) {
            System.err.println("[Engine] Unhandled exception in game cycle:")
            t.printStackTrace()
        }
    }

    private fun bindCommunicationThread() {
        if (communicationThreadBound) return

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

    private fun networkPulse() {
        context.players.processDisconnections()

        val activePlayers = context.players.snapshot()
        if (activePlayers.isEmpty()) return

        forEachConnected(activePlayers) { player ->
            player.session.processIncomingPackets(player)
        }

        forEachConnected(activePlayers) { player ->
            player.session.flush()
        }
    }

    private fun cycle() {
        // 1. World-level feature work, e.g. accepting queued logins.
        features.cycleStart(context)

        val activePlayers = context.players.snapshot()
        if (activePlayers.isEmpty()) return

        // 2. Keep RSProt's root coordinate in sync with game state.
        forEachConnected(activePlayers) { player ->
            val position = player.position

            player.infos.updateRootCoord(
                position.level,
                position.x,
                position.z,
            )
        }

        // 3. Feature state that must precede infoProtocols.update().
        forEachConnected(activePlayers) { player ->
            features.beforeInfoUpdate(context, player)
        }

        // 4. Build player/NPC/world-entity information.
        context.networkService.infoProtocols.update()

        // 5. Queue generic RSProt information output.
        forEachConnected(activePlayers) { player ->
            infoSynchronizer.queue(player)
        }

        // 6. Feature output that should follow info packets.
        forEachConnected(activePlayers) { player ->
            features.afterInfoUpdate(context, player)
        }

        // 7. Flush everything produced by this game cycle.
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
        if (!running.compareAndSet(true, false)) return

        networkTask?.cancel(false)
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

    private companion object {
        const val NETWORK_PULSE_MILLIS: Long = 20L
    }
}