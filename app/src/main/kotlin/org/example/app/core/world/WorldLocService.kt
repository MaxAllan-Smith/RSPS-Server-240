package org.example.app.core.world

import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.game.outgoing.zone.payload.LocDel
import net.rsprot.protocol.game.outgoing.zone.payload.SoundArea
import org.example.app.core.engine.GameContext
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Server-authoritative runtime state for temporary world-location changes.
 *
 * This is generic world state, not a gameplay feature: any vertical slice
 * may replace or spawn a loc through it. Supports both:
 *
 * - replacing an existing static location temporarily;
 * - spawning a completely new temporary location.
 *
 * Examples: tree -> stump -> tree, rock -> depleted rock -> rock, temporary
 * doors, player-made fires, quest scenery, temporary event objects.
 */
class WorldLocService {

    private val overrides = LinkedHashMap<WorldLocKey, DynamicWorldLoc>()
    private var revision: Long = 0L

    fun isOverridden(position: WorldPosition, shape: Int): Boolean =
        overrides.containsKey(WorldLocKey(position, shape))

    /** Temporarily replaces a static cache/world location. */
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
        require(originalId >= 0) { "Original loc id must be non-negative." }
        require(replacementId >= 0) { "Replacement loc id must be non-negative." }
        require(shape in 0..22) { "Loc shape must be in range 0..22." }
        require(rotation in 0..3) { "Loc rotation must be in range 0..3." }
        require(respawnTicks > 0) { "Respawn ticks must be positive." }
        require(soundRadius in 0..31) { "Area sound radius must be in range 0..31." }

        val key = WorldLocKey(position, shape)
        if (key in overrides) return false

        val override = DynamicWorldLoc(
            originalId = originalId,
            replacementId = replacementId,
            position = position,
            shape = shape,
            rotation = rotation,
            ticksRemaining = respawnTicks,
        )
        overrides[key] = override
        revision++

        broadcastLoc(context, override, replacementId, OpFlags.NONE_SHOWN)
        if (soundId != null) {
            broadcastSound(context, position, soundId, soundRadius)
        }

        println(
            "[World] Replaced loc $originalId -> $replacementId at " +
                "${position.x},${position.z},${position.level} for $respawnTicks ticks.",
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
        require(id >= 0) { "Loc id must be non-negative." }
        require(shape in 0..22) { "Loc shape must be in range 0..22." }
        require(rotation in 0..3) { "Loc rotation must be in range 0..3." }
        require(lifetimeTicks > 0) { "Temporary loc lifetime must be positive." }

        val key = WorldLocKey(position, shape)
        if (key in overrides) return false

        val loc = DynamicWorldLoc(
            originalId = null,
            replacementId = id,
            position = position,
            shape = shape,
            rotation = rotation,
            ticksRemaining = lifetimeTicks,
        )
        overrides[key] = loc
        revision++

        broadcastLoc(context, loc, id, OpFlags.NONE_SHOWN)
        println(
            "[World] Spawned temporary loc $id at " +
                "${position.x},${position.z},${position.level} for $lifetimeTicks ticks.",
        )
        return true
    }

    /** Advances all dynamic world-location timers. */
    fun cycle(context: GameContext) {
        if (overrides.isEmpty()) return

        val expired = mutableListOf<Pair<WorldLocKey, DynamicWorldLoc>>()
        for ((key, loc) in overrides) {
            loc.ticksRemaining--
            if (loc.ticksRemaining <= 0) expired += key to loc
        }

        for ((key, loc) in expired) {
            overrides.remove(key)
            revision++

            val originalId = loc.originalId
            if (originalId != null) {
                // Replacement override: restore the static/original loc.
                broadcastLoc(context, loc, originalId, OpFlags.ALL_SHOWN)
                println(
                    "[World] Respawned loc $originalId at " +
                        "${loc.position.x},${loc.position.z},${loc.position.level}.",
                )
            } else {
                // Spawn-only override: delete the temporary loc.
                broadcastDelete(context, loc)
                println(
                    "[World] Removed temporary loc ${loc.replacementId} at " +
                        "${loc.position.x},${loc.position.z},${loc.position.level}.",
                )
            }
        }
    }

    /** Applies currently-active dynamic locations after login or scene rebuild. */
    fun synchronize(player: Player) {
        val mapState = player.worldMapState
        val baseZoneX = mapState.baseZoneX ?: return
        val baseZoneZ = mapState.baseZoneZ ?: return

        val syncState = player.worldLocSyncState
        if (
            syncState.synchronizedRevision == revision &&
            syncState.synchronizedBaseZoneX == baseZoneX &&
            syncState.synchronizedBaseZoneZ == baseZoneZ
        ) {
            return
        }

        for (loc in overrides.values) {
            if (!ZoneBroadcast.isVisible(player, loc.position)) continue
            queueLoc(player, loc, loc.replacementId, OpFlags.NONE_SHOWN)
        }

        syncState.synchronizedRevision = revision
        syncState.synchronizedBaseZoneX = baseZoneX
        syncState.synchronizedBaseZoneZ = baseZoneZ
    }

    private fun broadcastLoc(context: GameContext, loc: DynamicWorldLoc, id: Int, opFlags: Byte) {
        ZoneBroadcast.broadcastToVisible(context, loc.position) { player -> queueLoc(player, loc, id, opFlags) }
    }

    /** Deletes a spawn-only temporary location from every visible client. */
    private fun broadcastDelete(context: GameContext, loc: DynamicWorldLoc) {
        ZoneBroadcast.broadcastToVisible(context, loc.position) { player -> queueDelete(player, loc) }
    }

    private fun broadcastSound(context: GameContext, position: WorldPosition, soundId: Int, radius: Int) {
        val sound = SoundArea(
            id = soundId,
            delay = 0,
            loops = 1,
            radius = radius,
            size = 1,
            xInZone = position.x and ZoneBroadcast.ZONE_MASK,
            zInZone = position.z and ZoneBroadcast.ZONE_MASK,
        )

        ZoneBroadcast.broadcastToVisible(context, position) { player ->
            ZoneBroadcast.queueEnclosedZonePayload(player, position, listOf(sound))
        }
    }

    private fun queueLoc(player: Player, loc: DynamicWorldLoc, id: Int, opFlags: Byte) {
        ZoneBroadcast.queueFollowingZonePayload(
            player,
            loc.position,
            LocAddChangeV2(
                id = id,
                xInZone = loc.position.x and ZoneBroadcast.ZONE_MASK,
                zInZone = loc.position.z and ZoneBroadcast.ZONE_MASK,
                shape = loc.shape,
                rotation = loc.rotation,
                opFlags = opFlags,
            ),
        )
    }

    private fun queueDelete(player: Player, loc: DynamicWorldLoc) {
        ZoneBroadcast.queueFollowingZonePayload(
            player,
            loc.position,
            LocDel(
                xInZone = loc.position.x and ZoneBroadcast.ZONE_MASK,
                zInZone = loc.position.z and ZoneBroadcast.ZONE_MASK,
                shape = loc.shape,
                rotation = loc.rotation,
            ),
        )
    }

    private data class WorldLocKey(val position: WorldPosition, val shape: Int)

    private data class DynamicWorldLoc(
        val originalId: Int?,
        val replacementId: Int,
        val position: WorldPosition,
        val shape: Int,
        val rotation: Int,
        var ticksRemaining: Int,
    )

    private companion object {
        const val DEFAULT_SOUND_RADIUS: Int = 10
    }
}
