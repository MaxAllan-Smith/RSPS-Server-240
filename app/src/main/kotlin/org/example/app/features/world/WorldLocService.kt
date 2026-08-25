package org.example.app.features.world

import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.LocDel
import net.rsprot.protocol.game.outgoing.zone.payload.SoundArea
import net.rsprot.protocol.message.ZoneProt
import org.example.app.core.engine.GameContext
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Server-authoritative runtime state for temporary world-location changes.
 *
 * Supports both:
 *
 * - replacing an existing static location temporarily;
 * - spawning a completely new temporary location.
 *
 * Examples:
 *
 * - tree -> stump -> tree;
 * - rock -> depleted rock -> rock;
 * - temporary doors;
 * - player-made fires;
 * - quest scenery;
 * - temporary event objects.
 */
class WorldLocService {

    private val overrides =
        LinkedHashMap<WorldLocKey, DynamicWorldLoc>()

    private var revision: Long =
        0L

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
     * Temporarily replaces a static cache/world location.
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

        if (
            key in
            overrides
        ) {
            return false
        }

        val override =
            DynamicWorldLoc(
                originalId =
                    originalId,

                replacementId =
                    replacementId,

                position =
                    position,

                shape =
                    shape,

                rotation =
                    rotation,

                ticksRemaining =
                    respawnTicks,
            )

        overrides[key] =
            override

        revision++

        broadcastLoc(
            context =
                context,

            loc =
                override,

            id =
                replacementId,

            opFlags =
                OpFlags.NONE_SHOWN,
        )

        if (
            soundId !=
            null
        ) {
            broadcastSound(
                context =
                    context,

                position =
                    position,

                soundId =
                    soundId,

                radius =
                    soundRadius,
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
     * Spawns a completely new temporary location.
     *
     * Unlike [replace], there is no static original location to restore when
     * the timer expires. The client therefore receives LocDel on expiry.
     */
    fun spawnTemporary(
        context: GameContext,
        id: Int,
        position: WorldPosition,
        shape: Int,
        rotation: Int,
        lifetimeTicks: Int,
    ): Boolean {
        require(id >= 0) {
            "Loc id must be non-negative."
        }

        require(shape in 0..22) {
            "Loc shape must be in range 0..22."
        }

        require(rotation in 0..3) {
            "Loc rotation must be in range 0..3."
        }

        require(lifetimeTicks > 0) {
            "Temporary loc lifetime must be positive."
        }

        val key =
            WorldLocKey(
                position =
                    position,

                shape =
                    shape,
            )

        if (
            key in
            overrides
        ) {
            return false
        }

        val loc =
            DynamicWorldLoc(
                originalId =
                    null,

                replacementId =
                    id,

                position =
                    position,

                shape =
                    shape,

                rotation =
                    rotation,

                ticksRemaining =
                    lifetimeTicks,
            )

        overrides[key] =
            loc

        revision++

        broadcastLoc(
            context =
                context,

            loc =
                loc,

            id =
                id,

            opFlags =
                OpFlags.NONE_SHOWN,
        )

        println(
            "[World] Spawned temporary loc $id " +
                "at ${position.x}," +
                "${position.z}," +
                "${position.level} " +
                "for $lifetimeTicks ticks."
        )

        return true
    }

    /**
     * Advances all dynamic world-location timers.
     */
    fun cycle(
        context: GameContext,
    ) {
        if (
            overrides.isEmpty()
        ) {
            return
        }

        val expired =
            ArrayList<
                Pair<
                    WorldLocKey,
                    DynamicWorldLoc
                    >
                >()

        for (
            (key, loc) in
            overrides
        ) {
            loc.ticksRemaining--

            if (
                loc.ticksRemaining <=
                0
            ) {
                expired +=
                    key to
                    loc
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

            val originalId =
                loc.originalId

            if (
                originalId !=
                null
            ) {
                /*
                 * Replacement override:
                 * restore the static/original loc.
                 */
                broadcastLoc(
                    context =
                        context,

                    loc =
                        loc,

                    id =
                        originalId,

                    opFlags =
                        OpFlags.ALL_SHOWN,
                )

                println(
                    "[World] Respawned loc " +
                        "$originalId " +
                        "at ${loc.position.x}," +
                        "${loc.position.z}," +
                        "${loc.position.level}."
                )
            } else {
                /*
                 * Spawn-only override:
                 * delete the temporary loc.
                 */
                broadcastDelete(
                    context =
                        context,

                    loc =
                        loc,
                )

                println(
                    "[World] Removed temporary loc " +
                        "${loc.replacementId} " +
                        "at ${loc.position.x}," +
                        "${loc.position.z}," +
                        "${loc.position.level}."
                )
            }
        }
    }

    /**
     * Applies currently-active dynamic locations after login or scene rebuild.
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
                    player =
                        player,

                    position =
                        loc.position,
                )
            ) {
                continue
            }

            queueLoc(
                player =
                    player,

                loc =
                    loc,

                id =
                    loc.replacementId,

                opFlags =
                    OpFlags.NONE_SHOWN,
            )
        }

        syncState.synchronizedRevision =
            revision

        syncState.synchronizedBaseZoneX =
            baseZoneX

        syncState.synchronizedBaseZoneZ =
            baseZoneZ
    }

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
                    player =
                        player,

                    position =
                        loc.position,
                )
            ) {
                continue
            }

            queueLoc(
                player =
                    player,

                loc =
                    loc,

                id =
                    id,

                opFlags =
                    opFlags,
            )
        }
    }

    /**
     * Deletes a spawn-only temporary location from every visible client.
     */
    private fun broadcastDelete(
        context: GameContext,
        loc: DynamicWorldLoc,
    ) {
        for (
            player in
            context.players.snapshot()
        ) {
            if (
                player.isDisconnected ||
                !isVisible(
                    player =
                        player,

                    position =
                        loc.position,
                )
            ) {
                continue
            }

            queueDelete(
                player =
                    player,

                loc =
                    loc,
            )
        }
    }

    private fun broadcastSound(
        context: GameContext,
        position: WorldPosition,
        soundId: Int,
        radius: Int,
    ) {
        val sound =
            SoundArea(
                id =
                    soundId,

                delay =
                    0,

                loops =
                    1,

                radius =
                    radius,

                size =
                    1,

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
                    player =
                        player,

                    position =
                        position,
                )
            ) {
                continue
            }

            queueEnclosedZonePayload(
                player =
                    player,

                position =
                    position,

                payloads =
                    listOf(
                        sound
                    ),
            )
        }
    }

    private fun queueLoc(
        player: Player,
        loc: DynamicWorldLoc,
        id: Int,
        opFlags: Byte,
    ) {
        queueFollowingZonePayload(
            player =
                player,

            position =
                loc.position,

            payload =
                LocAddChangeV2(
                    id =
                        id,

                    xInZone =
                        loc.position.x and
                            ZONE_MASK,

                    zInZone =
                        loc.position.z and
                            ZONE_MASK,

                    shape =
                        loc.shape,

                    rotation =
                        loc.rotation,

                    opFlags =
                        opFlags,
                ),
        )
    }

    private fun queueDelete(
        player: Player,
        loc: DynamicWorldLoc,
    ) {
        queueFollowingZonePayload(
            player =
                player,

            position =
                loc.position,

            payload =
                LocDel(
                    xInZone =
                        loc.position.x and
                            ZONE_MASK,

                    zInZone =
                        loc.position.z and
                            ZONE_MASK,

                    shape =
                        loc.shape,

                    rotation =
                        loc.rotation,
                ),
        )
    }

    private fun queueFollowingZonePayload(
        player: Player,
        position: WorldPosition,
        payload: ZoneProt,
    ) {
        val zone =
            localZone(
                player =
                    player,

                position =
                    position,
            )
                ?: return

        player.session.queue(
            UpdateZonePartialFollows(
                zoneX =
                    zone.x,

                zoneZ =
                    zone.z,

                level =
                    position.level,
            )
        )

        player.session.queue(
            payload
        )
    }

    private fun queueEnclosedZonePayload(
        player: Player,
        position: WorldPosition,
        payloads: Collection<ZoneProt>,
    ) {
        if (
            payloads.isEmpty()
        ) {
            return
        }

        val zone =
            localZone(
                player =
                    player,

                position =
                    position,
            )
                ?: return

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

        val message =
            try {
                UpdateZonePartialEnclosed(
                    zoneX =
                        zone.x,

                    zoneZ =
                        zone.z,

                    level =
                        position.level,

                    payload =
                        buffer,
                )
            } finally {
                buffer.release()
            }

        player.session.queue(
            message
        )
    }

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
        val originalId: Int?,
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

        const val ZONE_TILE_MASK: Int =
            -8

        const val BUILD_AREA_SIZE: Int =
            104

        const val DEFAULT_SOUND_RADIUS: Int =
            10
    }
}