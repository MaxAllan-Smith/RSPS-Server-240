package org.example.app.engine

import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.info.Infos
import net.rsprot.protocol.game.outgoing.info.playerinfo.PlayerInfo
import net.rsprot.protocol.game.outgoing.info.util.PacketResult
import net.rsprot.protocol.game.outgoing.info.util.isEmpty
import net.rsprot.protocol.game.outgoing.info.util.safeReleaseOrThrow
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenTop
import net.rsprot.protocol.game.outgoing.map.RebuildLoginV2
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import net.rsprot.protocol.loginprot.outgoing.util.AuthenticatorResponse
import net.rsprot.protocol.message.OutgoingGameMessage
import org.example.app.network.LoginRequestQueue
import org.example.app.network.PendingLogin
import org.example.app.player.Player
import org.example.app.player.PlayerRepository
import org.example.app.player.WorldPosition
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameEngine(
    private val networkService: NetworkService<Player>,
    private val players: PlayerRepository,
    private val loginRequests: LoginRequestQueue,
) : AutoCloseable {

    private val running = AtomicBoolean(false)

    /*
     * Netty/RSProt can tell us about a disconnected player from a
     * network thread. PlayerRepository and RSProt InfoProtocols are
     * game-thread owned, so actual cleanup is deferred until the
     * beginning of the next engine cycle.
     */
    private val disconnectedPlayers = ConcurrentLinkedQueue<Player>()

    /*
     * The game simulation and RSProt information protocol are owned by
     * this single communication thread.
     */
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(
            runnable,
            "game-engine",
        ).apply {
            isDaemon = false
        }
    }

    private var cycleTask: ScheduledFuture<*>? = null

    private var communicationThreadBound = false

    fun start() {
        check(
            running.compareAndSet(
                false,
                true,
            )
        ) {
            "Game engine already started."
        }

        cycleTask = executor.scheduleAtFixedRate(
            ::runCycleSafely,
            0L,
            CYCLE_MILLIS,
            TimeUnit.MILLISECONDS,
        )

        println(
            "[Engine] Game engine started (${CYCLE_MILLIS}ms cycle)."
        )
    }

    private fun runCycleSafely() {
        if (!running.get()) {
            return
        }

        try {
            bindCommunicationThread()
            cycle()
        } catch (t: Throwable) {
            System.err.println(
                "[Engine] Unhandled exception in game cycle:"
            )

            t.printStackTrace()
        }
    }

    private fun bindCommunicationThread() {
        if (communicationThreadBound) {
            return
        }

        networkService.setCommunicationThread(
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
        /*
         * ---------------------------------------------------------
         * PHASE 1
         * Remove players whose network connection disappeared.
         * ---------------------------------------------------------
         */

        processDisconnections()

        /*
         * ---------------------------------------------------------
         * PHASE 2
         * Convert pending network login requests into game players.
         * ---------------------------------------------------------
         */

        processLogins()

        val activePlayers = players.snapshot()

        if (activePlayers.isEmpty()) {
            return
        }

        /*
         * ---------------------------------------------------------
         * PHASE 3
         * Process incoming game packets.
         *
         * MAP_BUILD_COMPLETE is registered in NetworkFactory, giving
         * us a positive signal that the OSRS client has finished
         * constructing the game scene.
         * ---------------------------------------------------------
         */

        for (player in activePlayers) {
            if (player.isDisconnected) {
                continue
            }

            player.session.processIncomingPackets(
                player
            )
        }

        /*
         * ---------------------------------------------------------
         * PHASE 4
         * Synchronise our player coordinates with RSProt.
         * ---------------------------------------------------------
         */

        for (player in activePlayers) {
            if (player.isDisconnected) {
                continue
            }

            updateInfoCoordinates(
                player
            )
        }

        /*
         * ---------------------------------------------------------
         * PHASE 5
         * INITIAL LOGIN REBUILD
         *
         * RebuildLoginV2 MUST be constructed/queued before the first
         * infoProtocols.update() for a newly allocated PlayerInfo.
         *
         * RebuildLoginV2 initializes the client's initial GPI state.
         * ---------------------------------------------------------
         */

        for (player in activePlayers) {
            if (player.isDisconnected) {
                continue
            }

            if (player.needsLoginRebuild) {
                queueLoginRebuild(
                    player
                )
            }

            if (player.needsInitialClientState) {
                queueInitialClientState(
                    player
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * PHASE 6
         * Build player/NPC/world-entity info exactly once per cycle.
         * ---------------------------------------------------------
         */

        networkService.infoProtocols.update()

        /*
         * ---------------------------------------------------------
         * PHASE 7
         * Queue the generated RSProt information packets.
         * ---------------------------------------------------------
         */

        for (player in activePlayers) {
            if (player.isDisconnected) {
                continue
            }

            queueOutput(
                player
            )
        }

        /*
         * ---------------------------------------------------------
         * PHASE 8
         * Flush everything to the client.
         * ---------------------------------------------------------
         */

        for (player in activePlayers) {
            if (player.isDisconnected) {
                continue
            }

            player.session.flush()
        }
    }

    private fun updateInfoCoordinates(
        player: Player,
    ) {
        val position = player.position

        player.infos.updateRootCoord(
            position.level,
            position.x,
            position.z,
        )
    }

    /*
     * This must execute before infoProtocols.update() on the player's
     * first game cycle.
     */
    private fun queueLoginRebuild(
        player: Player,
    ) {
        check(
            player.needsLoginRebuild
        ) {
            "Login rebuild already initialized for ${player.username}."
        }

        val position = player.position

        println(
            "[Login] Initializing GPI for " +
                "'${player.username}' " +
                "index=${player.index}"
        )

        /*
         * Since revision 237, the root/toplevel world ID is 0.
         */
        val rebuild = RebuildLoginV2(
            zoneX = position.zoneX,
            zoneZ = position.zoneZ,
            worldArea = ROOT_WORLD,
            playerInfo = player.infos.playerInfo,
        )

        /*
         * This is the first game packet sent after LOGIN_OK.
         */
        player.session.queue(
            rebuild
        )

        player.needsLoginRebuild = false
    }

    /*
     * The map rebuild is not enough by itself to turn the official
     * client into a usable in-game client.
     *
     * We also need to establish its root gameframe and its initial
     * client-side state.
     */
    private fun queueInitialClientState(
        player: Player,
    ) {
        check(
            !player.needsLoginRebuild
        ) {
            "Initial client state cannot be queued before the login rebuild."
        }

        check(
            player.needsInitialClientState
        ) {
            "Initial client state already queued for ${player.username}."
        }

        val topLevelInterface =
            if (player.resizable) {
                RESIZABLE_TOP_LEVEL_INTERFACE
            } else {
                FIXED_TOP_LEVEL_INTERFACE
            }

        /*
         * Open the root OSRS gameframe.
         *
         * 548 = fixed mode
         * 161 = classic resizable mode
         */
        player.session.queue(
            IfOpenTop(
                topLevelInterface
            )
        )

        /*
         * Reset client varps.
         */
        player.session.queue(
            VarpReset
        )

        /*
         * Public chat = On
         * Trade chat  = On
         */
        player.session.queue(
            ChatFilterSettings(
                0,
                0,
            )
        )

        /*
         * Enable normal interaction options.
         */
        player.session.queue(
            HideNpcOps(
                false
            )
        )

        player.session.queue(
            HideLocOps(
                false
            )
        )

        player.session.queue(
            HideObjOps(
                false
            )
        )

        /*
         * Establish normal animation/minimap state.
         */
        player.session.queue(
            ResetAnims
        )

        player.session.queue(
            MinimapToggle(
                0
            )
        )

        player.needsInitialClientState = false

        println(
            "[Login] Initial client state queued for " +
                "'${player.username}' " +
                "topLevel=$topLevelInterface " +
                "resizable=${player.resizable}"
        )
    }

    private fun processLogins() {
        while (true) {
            val request =
                loginRequests.poll()
                    ?: break

            acceptLogin(
                request
            )
        }
    }

    private fun acceptLogin(
        request: PendingLogin,
    ) {
        val username =
            request.loginBlock.username

        /*
         * A client can disconnect between RSProt decoding the login
         * and our next 600ms engine cycle.
         */
        if (
            !request.responseHandler
                .ctx
                .channel()
                .isActive
        ) {
            return
        }

        /*
         * Basic duplicate login protection.
         */
        if (
            players.isOnline(
                username
            )
        ) {
            println(
                "[Login] Duplicate login rejected: '$username'"
            )

            request.responseHandler
                .writeFailedResponse(
                    LoginResponse.Duplicate
                )

            return
        }

        val index =
            players.nextFreeIndex()

        if (index == null) {
            println(
                "[Login] World full; rejecting '$username'."
            )

            request.responseHandler
                .writeFailedResponse(
                    LoginResponse.ServerFull
                )

            return
        }

        var infos: Infos? =
            null

        try {
            /*
             * Allocate RSProt state for this client.
             *
             * OSRS player indices are 1..2047.
             */
            infos =
                networkService
                    .infoProtocols
                    .alloc(
                        index,
                        OldSchoolClientType.DESKTOP,
                    )

            val position =
                WorldPosition.LUMBRIDGE

            /*
             * RSProt needs the player's absolute coordinate before
             * creating RebuildLoginV2.
             */
            infos.updateRootCoord(
                position.level,
                position.x,
                position.z,
            )

            /*
             * Establish the normal static build area around Lumbridge.
             */
            infos.updateRootBuildAreaCenteredOnPlayer(
                position.x,
                position.z,
            )

            /*
             * Build a valid initial player appearance.
             */
            initializeAppearance(
                infos.playerInfo,
                username,
            )

            /*
             * Temporary local identity until persistent accounts are
             * implemented.
             */
            val identity =
                LocalIdentity.forUsername(
                    username
                )

            /*
             * Development server login response.
             */
            val response =
                LoginResponse.Ok(
                    authenticatorResponse =
                        AuthenticatorResponse.NoAuthenticator,
                    staffModLevel =
                        0,
                    playerMod =
                        false,
                    index =
                        index,
                    member =
                        true,
                    accountHash =
                        identity.accountHash,
                    userId =
                        identity.userId,
                    userHash =
                        identity.userHash,
                )

            /*
             * RSProt handles:
             *
             * - LOGIN_OK
             * - ISAAC setup
             * - login -> game pipeline transition
             * - Session<Player> creation
             */
            val session =
                request.responseHandler
                    .writeSuccessfulResponse(
                        response,
                        request.loginBlock,
                    )

            val player =
                Player(
                    username =
                        username,
                    index =
                        index,
                    session =
                        session,
                    infos =
                        infos,
                    position =
                        position,
                    resizable =
                        request.loginBlock.resizable,
                )

            /*
             * This callback can run on Netty's event loop.
             *
             * Do not directly mutate PlayerRepository or deallocate
             * RSProt info state from it.
             */
            session.setDisconnectionHook {
                if (
                    player.markDisconnected()
                ) {
                    disconnectedPlayers.offer(
                        player
                    )
                }
            }

            /*
             * It is possible for the socket to disappear during the
             * login transition.
             */
            if (
                player.isDisconnected
            ) {
                networkService
                    .infoProtocols
                    .dealloc(
                        infos
                    )

                return
            }

            players.add(
                player
            )

            println(
                "[Login] Accepted '$username' " +
                    "index=$index " +
                    "position=" +
                    "${position.x}," +
                    "${position.z}," +
                    "${position.level}"
            )
        } catch (t: Throwable) {
            if (infos != null) {
                try {
                    networkService
                        .infoProtocols
                        .dealloc(
                            infos
                        )
                } catch (
                    cleanupFailure: Throwable
                ) {
                    t.addSuppressed(
                        cleanupFailure
                    )
                }
            }

            System.err.println(
                "[Login] Failed to accept '$username'."
            )

            t.printStackTrace()

            /*
             * Once LOGIN_OK changes the pipeline we cannot safely
             * send another login response, so closing the channel is
             * the safest error path.
             */
            if (
                request.responseHandler
                    .ctx
                    .channel()
                    .isActive
            ) {
                try {
                    request.responseHandler
                        .ctx
                        .close()
                } catch (_: Throwable) {
                    /*
                     * Nothing else useful can be done here.
                     */
                }
            }
        }
    }

    private fun initializeAppearance(
        info: PlayerInfo,
        username: String,
    ) {
        val appearance =
            info.avatar.extendedInfo

        appearance.setName(
            username
        )

        /*
         * Fresh level-3 player.
         */
        appearance.setCombatLevel(
            3
        )

        appearance.setSkillLevel(
            0
        )

        appearance.setHidden(
            false
        )

        appearance.setBodyType(
            0
        )

        appearance.setPronoun(
            0
        )

        appearance.setSkullIcon(
            -1
        )

        appearance.setOverheadIcon(
            -1
        )

        /*
         * Default body colours.
         */
        for (index in 0..<5) {
            appearance.setColour(
                index,
                0,
            )
        }

        /*
         * Valid human ident-kit values for the revision-240 client.
         */
        appearance.setIdentKit(
            0,
            0,
        )

        appearance.setIdentKit(
            1,
            10,
        )

        appearance.setIdentKit(
            2,
            18,
        )

        appearance.setIdentKit(
            3,
            26,
        )

        appearance.setIdentKit(
            4,
            33,
        )

        appearance.setIdentKit(
            5,
            36,
        )

        appearance.setIdentKit(
            6,
            42,
        )

        /*
         * Standard human animation set:
         *
         * stand
         * turn
         * walk
         * turn-180
         * turn-90-cw
         * turn-90-ccw
         * run
         */
        appearance.setBaseAnimationSet(
            808,
            823,
            819,
            820,
            821,
            822,
            824,
        )
    }

    private fun queueOutput(
        player: Player,
    ) {
        /*
         * RebuildLoginV2 has already been queued before
         * infoProtocols.update().
         */
        val infoPackets =
            player.infos.getPackets()

        val root =
            infoPackets.rootWorldInfoPackets

        /*
         * Tell the client that the following information belongs to
         * root world 0.
         */
        player.session.queue(
            root.activeWorld
        )

        /*
         * NPC information uses an update origin even if there are
         * currently no NPCs.
         */
        player.session.queue(
            root.npcUpdateOrigin
        )

        /*
         * World-entity information.
         */
        if (
            !queueResult(
                player =
                    player,
                name =
                    "world-entity-info",
                result =
                    root.worldEntityInfo,
            )
        ) {
            return
        }

        /*
         * Player GPI information.
         */
        if (
            !queueResult(
                player =
                    player,
                name =
                    "player-info",
                result =
                    root.playerInfo,
            )
        ) {
            return
        }

        /*
         * OSRS does not send an empty NPC-info packet.
         *
         * RSProt still owns an allocated buffer for an empty result,
         * so release it correctly.
         */
        if (
            root.npcInfo.isEmpty()
        ) {
            root.npcInfo
                .safeReleaseOrThrow()
        } else {
            if (
                !queueResult(
                    player =
                        player,
                    name =
                        "npc-info",
                    result =
                        root.npcInfo,
                )
            ) {
                return
            }
        }

        /*
         * Dynamic world entities / instances are not implemented in
         * this server yet.
         */
        check(
            infoPackets.activeWorlds.isEmpty()
        ) {
            "Dynamic world entities are not implemented yet."
        }

        /*
         * Restore the client's root active-world selector after the
         * complete information sequence.
         *
         * This was missing from the old implementation.
         */
        player.session.queue(
            root.activeWorld
        )
    }

    private fun <T : OutgoingGameMessage> queueResult(
        player: Player,
        name: String,
        result: PacketResult<T>,
    ): Boolean {
        val packet =
            result.getOrNull()

        if (packet != null) {
            player.session.queue(
                packet
            )

            return true
        }

        val cause =
            result.exceptionOrNull()

        System.err.println(
            "[Engine] RSProt $name failed " +
                "for ${player.username}: " +
                cause
        )

        disconnect(
            player,
            "RSProt $name failure",
        )

        return false
    }

    private fun disconnect(
        player: Player,
        reason: String,
    ) {
        if (
            !player.markDisconnected()
        ) {
            return
        }

        println(
            "[Logout] ${player.username}: $reason"
        )

        player.session
            .requestClose()

        disconnectedPlayers.offer(
            player
        )
    }

    private fun processDisconnections() {
        while (true) {
            val player =
                disconnectedPlayers.poll()
                    ?: break

            /*
             * The player might already have been removed during
             * shutdown or a prior cleanup pass.
             */
            if (
                !players.remove(
                    player
                )
            ) {
                continue
            }

            networkService
                .infoProtocols
                .dealloc(
                    player.infos
                )

            println(
                "[Logout] Removed '${player.username}' " +
                    "index=${player.index}. " +
                    "Online=${players.size}"
            )
        }
    }

    override fun close() {
        if (
            !running.compareAndSet(
                true,
                false,
            )
        ) {
            return
        }

        cycleTask?.cancel(
            false
        )

        /*
         * RSProt info state must be released from its owning
         * communication/game thread.
         */
        val cleanup =
            executor.submit {
                bindCommunicationThread()

                val activePlayers =
                    players.snapshot()

                for (player in activePlayers) {
                    try {
                        player.session
                            .requestClose()
                    } finally {
                        try {
                            networkService
                                .infoProtocols
                                .dealloc(
                                    player.infos
                                )
                        } finally {
                            players.remove(
                                player
                            )
                        }
                    }
                }
            }

        try {
            cleanup.get(
                5L,
                TimeUnit.SECONDS,
            )
        } catch (t: Throwable) {
            System.err.println(
                "[Engine] Error while shutting down."
            )

            t.printStackTrace()
        } finally {
            executor.shutdown()

            try {
                executor.awaitTermination(
                    5L,
                    TimeUnit.SECONDS,
                )
            } catch (_: InterruptedException) {
                Thread
                    .currentThread()
                    .interrupt()
            }
        }

        println(
            "[Engine] Game engine stopped."
        )
    }

    /*
     * Temporary deterministic IDs for local development accounts.
     *
     * Replace this with persistent account/player IDs once your
     * account database is introduced.
     */
    private data class LocalIdentity(
        val accountHash: Long,
        val userId: Long,
        val userHash: Long,
    ) {
        companion object {

            fun forUsername(
                username: String,
            ): LocalIdentity {
                val normalized =
                    username
                        .trim()
                        .lowercase()

                return LocalIdentity(
                    accountHash =
                        stableLong(
                            "account:$normalized"
                        ),
                    userId =
                        stableLong(
                            "userid:$normalized"
                        ),
                    userHash =
                        stableLong(
                            "userhash:$normalized"
                        ),
                )
            }

            private fun stableLong(
                value: String,
            ): Long {
                val digest =
                    MessageDigest
                        .getInstance(
                            "SHA-256"
                        )
                        .digest(
                            value.toByteArray(
                                Charsets.UTF_8
                            )
                        )

                val valueLong =
                    ByteBuffer
                        .wrap(
                            digest
                        )
                        .long and
                        Long.MAX_VALUE

                return if (
                    valueLong == 0L
                ) {
                    1L
                } else {
                    valueLong
                }
            }
        }
    }

    private companion object {

        /*
         * Standard OSRS game tick.
         */
        const val CYCLE_MILLIS: Long =
            600L

        /*
         * Revision 237+ root/toplevel world.
         */
        const val ROOT_WORLD: Int =
            0

        /*
         * Fixed-mode OSRS gameframe.
         */
        const val FIXED_TOP_LEVEL_INTERFACE: Int =
            548

        /*
         * Classic resizable OSRS gameframe.
         */
        const val RESIZABLE_TOP_LEVEL_INTERFACE: Int =
            161
    }
}