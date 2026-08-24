package org.example.app.features.world

import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.SoundArea
import net.rsprot.protocol.message.ZoneProt
import org.example.app.core.engine.GameContext
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Server-authoritative runtime state for temporary world-location changes.
 *
 * Examples:
 *
 * - tree -> stump -> tree;
 * - rock -> depleted rock -> rock;
 * - temporary doors;
 * - quest scenery;
 * - temporary event objects.
 *
 * Static definitions belong in cache/database-backed definition repositories.
 * This service owns only transient runtime world overrides.
 */
class WorldLocService {

    private val overrides =
        LinkedHashMap<WorldLocKey, DynamicWorldLoc>()

    private var revision: Long =
        0L

    /**
     * Returns true when this location/layer currently has a dynamic override.
     */
    fun isOverridden(
        position: WorldPosition,
        shape: Int,
    ): Boolean =
        overrides.containsKey(
            WorldLocKey(
                position = position,
                shape = shape,
            )
        )

    /**
     * Temporarily replaces a static world location.
     *
     * The replacement is broadcast to every player whose current scene
     * contains the location.
     *
     * @return false when this exact location/layer is already overridden.
     */
    fun replace(
        context: GameContext,
        originalId: Int,
        replacementId: Int,
        position: WorldPosition,
        shape: Int,
        rotation: Int,
        respawnTicks: Int,
        soundId: Int? = null,
        soundRadius: Int = DEFAULT_SOUND_RADIUS,
    ): Boolean {
        require(originalId >= 0) {
            "Original loc id must be non-negative."
        }

        require(replacementId >= 0) {
            "Replacement loc id must be non-negative."
        }

        require(shape in 0..22) {
            "Loc shape must be in range 0..22."
        }

        require(rotation in 0..3) {
            "Loc rotation must be in range 0..3."
        }

        require(respawnTicks > 0) {
            "Respawn ticks must be positive."
        }

        require(soundRadius in 0..31) {
            "Area sound radius must be in range 0..31."
        }

        val key =
            WorldLocKey(
                position = position,
                shape = shape,
            )

        if (key in overrides) {
            return false
        }

        val override =
            DynamicWorldLoc(
                originalId = originalId,
                replacementId = replacementId,
                position = position,
                shape = shape,
                rotation = rotation,
                ticksRemaining = respawnTicks,
            )

        overrides[key] =
            override

        revision++

        /*
         * Location replacement may be sent with partial-follows because it is
         * a normal zone payload.
         */
        broadcastLoc(
            context = context,
            loc = override,
            id = replacementId,
            opFlags = OpFlags.NONE_SHOWN,
        )

        /*
         * SoundArea is different.
         *
         * As of revision 221 on Java clients RSProt requires SoundArea to be
         * encoded inside UpdateZonePartialEnclosed. Sending SoundArea directly
         * causes Session.validateMessage to reject it.
         */
        if (soundId != null) {
            broadcastSound(
                context = context,
                position = position,
                soundId = soundId,
                radius = soundRadius,
            )
        }

        println(
            "[World] Replaced loc " +
                "$originalId -> $replacementId " +
                "at ${position.x}," +
                "${position.z}," +
                "${position.level} " +
                "for $respawnTicks ticks."
        )

        return true
    }

    /**
     * Advances temporary world-location timers once per game cycle.
     */
    fun cycle(
        context: GameContext,
    ) {
        if (overrides.isEmpty()) {
            return
        }

        val expired =
            ArrayList<Pair<WorldLocKey, DynamicWorldLoc>>()

        for (
        (key, loc) in
        overrides
        ) {

            loc.ticksRemaining--

            if (
                loc.ticksRemaining <= 0
            ) {
                expired +=
                    key to loc
            }
        }

        for (
            (key, loc) in
            expired
        ) {
            overrides.remove(
                key
            )

            revision++

            /*
             * Restore the original static location.
             */
            broadcastLoc(
                context = context,
                loc = loc,
                id = loc.originalId,
                opFlags = OpFlags.ALL_SHOWN,
            )

            println(
                "[World] Respawned loc " +
                    "${loc.originalId} " +
                    "at ${loc.position.x}," +
                    "${loc.position.z}," +
                    "${loc.position.level}."
            )
        }
    }

    /**
     * Applies active dynamic overrides after login or a scene rebuild.
     *
     * Without this, a player entering an area after another player depleted a
     * tree would temporarily see the original cache tree rather than its stump.
     */
    fun synchronize(
        player: Player,
    ) {
        val mapState =
            player.worldMapState

        val baseZoneX =
            mapState.baseZoneX
                ?: return

        val baseZoneZ =
            mapState.baseZoneZ
                ?: return

        val syncState =
            player.worldLocSyncState

        if (
            syncState.synchronizedRevision ==
            revision &&
            syncState.synchronizedBaseZoneX ==
            baseZoneX &&
            syncState.synchronizedBaseZoneZ ==
            baseZoneZ
        ) {
            return
        }

        for (
            loc in
            overrides.values
        ) {
            if (
                !isVisible(
                    player = player,
                    position = loc.position,
                )
            ) {
                continue
            }

            queueLoc(
                player = player,
                loc = loc,
                id = loc.replacementId,
                opFlags = OpFlags.NONE_SHOWN,
            )
        }

        syncState.synchronizedRevision =
            revision

        syncState.synchronizedBaseZoneX =
            baseZoneX

        syncState.synchronizedBaseZoneZ =
            baseZoneZ
    }

    /**
     * Sends a location replacement to every player that currently has the
     * location inside their loaded scene.
     */
    private fun broadcastLoc(
        context: GameContext,
        loc: DynamicWorldLoc,
        id: Int,
        opFlags: Byte,
    ) {
        for (
            player in
            context.players.snapshot()
        ) {
            if (
                player.isDisconnected ||
                !isVisible(
                    player = player,
                    position = loc.position,
                )
            ) {
                continue
            }

            queueLoc(
                player = player,
                loc = loc,
                id = id,
                opFlags = opFlags,
            )
        }
    }

    /**
     * Broadcasts an area sound using revision-240's required
     * UpdateZonePartialEnclosed container.
     */
    private fun broadcastSound(
        context: GameContext,
        position: WorldPosition,
        soundId: Int,
        radius: Int,
    ) {
        val sound =
            SoundArea(
                id = soundId,
                delay = 0,

                /*
                 * RSProt documents loops=0 as "do not play".
                 */
                loops = 1,

                radius = radius,
                size = 1,

                xInZone =
                    position.x and
                        ZONE_MASK,

                zInZone =
                    position.z and
                        ZONE_MASK,
            )

        for (
            player in
            context.players.snapshot()
        ) {
            if (
                player.isDisconnected ||
                !isVisible(
                    player = player,
                    position = position,
                )
            ) {
                continue
            }

            queueEnclosedZonePayload(
                player = player,
                position = position,
                payloads =
                    listOf(
                        sound
                    ),
            )
        }
    }

    /**
     * Sends a location add/change through the normal partial-follows mechanism.
     *
     * LocAddChangeV2 is permitted as a directly-following zone payload in
     * revision 240.
     */
    private fun queueLoc(
        player: Player,
        loc: DynamicWorldLoc,
        id: Int,
        opFlags: Byte,
    ) {
        queueFollowingZonePayload(
            player = player,
            position = loc.position,
            payload =
                LocAddChangeV2(
                    id = id,

                    xInZone =
                        loc.position.x and
                            ZONE_MASK,

                    zInZone =
                        loc.position.z and
                            ZONE_MASK,

                    shape = loc.shape,
                    rotation = loc.rotation,
                    opFlags = opFlags,
                ),
        )
    }

    /**
     * Queues a zone payload using UpdateZonePartialFollows.
     *
     * This is efficient for a single zone packet and is valid for ordinary
     * payloads such as LocAddChangeV2.
     */
    private fun queueFollowingZonePayload(
        player: Player,
        position: WorldPosition,
        payload: ZoneProt,
    ) {
        val zone =
            localZone(
                player = player,
                position = position,
            )
                ?: return

        player.session.queue(
            UpdateZonePartialFollows(
                zoneX = zone.x,
                zoneZ = zone.z,
                level = position.level,
            )
        )

        player.session.queue(
            payload
        )
    }

    /**
     * Queues one or more zone payloads using UpdateZonePartialEnclosed.
     *
     * SoundArea MUST use this mechanism on revision-240 Java clients.
     *
     * ZonePartialEnclosedCacheBuffer creates the already-encoded payload for
     * the requested client type. UpdateZonePartialEnclosed retains that buffer,
     * so this method releases its original reference immediately after the
     * wrapper is constructed.
     */
    private fun queueEnclosedZonePayload(
        player: Player,
        position: WorldPosition,
        payloads: Collection<ZoneProt>,
    ) {
        if (payloads.isEmpty()) {
            return
        }

        val zone =
            localZone(
                player = player,
                position = position,
            )
                ?: return

        /*
         * This server currently supports the Desktop client only, matching the
         * NetworkService startup configuration.
         */
        val cache =
            ZonePartialEnclosedCacheBuffer(
                supportedClients =
                    listOf(
                        OldSchoolClientType.DESKTOP
                    )
            )

        val buffer =
            cache.computeZoneForClient(
                client =
                    OldSchoolClientType.DESKTOP,
                pendingTickProtList =
                    payloads,
            )

        /*
         * UpdateZonePartialEnclosed retains the supplied ByteBuf.
         *
         * Drop the original computeZoneForClient reference after creating the
         * wrapper so the queued message becomes the sole owner.
         */
        val message =
            try {
                UpdateZonePartialEnclosed(
                    zoneX = zone.x,
                    zoneZ = zone.z,
                    level = position.level,
                    payload = buffer,
                )
            } finally {
                buffer.release()
            }

        player.session.queue(
            message
        )
    }

    /**
     * Converts an absolute 8x8 world zone into its scene-local south-west
     * coordinate expected by RSProt's zone update headers.
     */
    private fun localZone(
        player: Player,
        position: WorldPosition,
    ): LocalZone? {
        val mapState =
            player.worldMapState

        val baseZoneX =
            mapState.baseZoneX
                ?: return null

        val baseZoneZ =
            mapState.baseZoneZ
                ?: return null

        val buildBaseX =
            baseZoneX shl
                ZONE_SHIFT

        val buildBaseZ =
            baseZoneZ shl
                ZONE_SHIFT

        val zoneSouthWestX =
            position.x and
                ZONE_TILE_MASK

        val zoneSouthWestZ =
            position.z and
                ZONE_TILE_MASK

        return LocalZone(
            x =
                zoneSouthWestX -
                    buildBaseX,
            z =
                zoneSouthWestZ -
                    buildBaseZ,
        )
    }

    /**
     * Tests whether an absolute world coordinate belongs to the player's
     * currently loaded 104x104 scene.
     */
    private fun isVisible(
        player: Player,
        position: WorldPosition,
    ): Boolean {
        if (
            player.position.level !=
            position.level
        ) {
            return false
        }

        val mapState =
            player.worldMapState

        val baseZoneX =
            mapState.baseZoneX
                ?: return false

        val baseZoneZ =
            mapState.baseZoneZ
                ?: return false

        val baseX =
            baseZoneX shl
                ZONE_SHIFT

        val baseZ =
            baseZoneZ shl
                ZONE_SHIFT

        return (
            position.x in
                baseX until
                    (
                        baseX +
                            BUILD_AREA_SIZE
                        ) &&
                position.z in
                baseZ until
                    (
                        baseZ +
                            BUILD_AREA_SIZE
                        )
            )
    }

    private data class LocalZone(
        val x: Int,
        val z: Int,
    )

    private data class WorldLocKey(
        val position: WorldPosition,
        val shape: Int,
    )

    private data class DynamicWorldLoc(
        val originalId: Int,
        val replacementId: Int,
        val position: WorldPosition,
        val shape: Int,
        val rotation: Int,
        var ticksRemaining: Int,
    )

    private companion object {
        const val ZONE_SHIFT: Int =
            3

        const val ZONE_MASK: Int =
            7

        /**
         * Removes the bottom three bits to obtain the south-western coordinate
         * of an 8x8 zone.
         */
        const val ZONE_TILE_MASK: Int =
            -8

        const val BUILD_AREA_SIZE: Int =
            104

        const val DEFAULT_SOUND_RADIUS: Int =
            10
    }
}