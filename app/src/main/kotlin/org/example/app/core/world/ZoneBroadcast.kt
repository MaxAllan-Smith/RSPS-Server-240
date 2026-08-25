package org.example.app.core.world

import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.message.ZoneProt
import org.example.app.core.engine.GameContext
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Shared RSProt wiring for broadcasting zone-scoped world-entity packets
 * (dynamic locs, ground items, area sounds) to every player whose build area
 * currently contains a given tile.
 *
 * [WorldLocService] and [GroundItemService] both need to translate an
 * absolute tile into a player's build-area-relative zone, decide whether
 * that tile is inside their currently-loaded 104x104 scene, and enqueue
 * either a single "following" zone update or an encoded batch of zone
 * payloads. This object is the single home for that mechanics so the two
 * services differ only in *what* they broadcast, never in *how*.
 */
object ZoneBroadcast {

    private const val ZONE_SHIFT = 3
    const val ZONE_MASK = 7
    private const val ZONE_TILE_MASK = -8
    private const val BUILD_AREA_SIZE = 104

    /** A zone's south-west tile expressed relative to the player's build area. */
    data class LocalZone(val x: Int, val z: Int)

    /** Whether [position] falls inside the static scene [player] currently has loaded. */
    fun isVisible(player: Player, position: WorldPosition): Boolean {
        if (player.position.level != position.level) return false

        val mapState = player.worldMapState
        val baseZoneX = mapState.baseZoneX ?: return false
        val baseZoneZ = mapState.baseZoneZ ?: return false

        val baseX = baseZoneX shl ZONE_SHIFT
        val baseZ = baseZoneZ shl ZONE_SHIFT

        return position.x in baseX until (baseX + BUILD_AREA_SIZE) &&
            position.z in baseZ until (baseZ + BUILD_AREA_SIZE)
    }

    /** Converts an absolute tile into the zone-local coordinate [player]'s client expects. */
    fun localZone(player: Player, position: WorldPosition): LocalZone? {
        val mapState = player.worldMapState
        val baseZoneX = mapState.baseZoneX ?: return null
        val baseZoneZ = mapState.baseZoneZ ?: return null

        val buildBaseX = baseZoneX shl ZONE_SHIFT
        val buildBaseZ = baseZoneZ shl ZONE_SHIFT

        return LocalZone(
            x = (position.x and ZONE_TILE_MASK) - buildBaseX,
            z = (position.z and ZONE_TILE_MASK) - buildBaseZ,
        )
    }

    /**
     * Queues one payload that follows the player's moving build area.
     *
     * Used for loc add/change/delete, where the payload targets a single
     * absolute tile.
     */
    fun queueFollowingZonePayload(player: Player, position: WorldPosition, payload: ZoneProt) {
        val zone = localZone(player, position) ?: return

        player.session.queue(
            UpdateZonePartialFollows(zoneX = zone.x, zoneZ = zone.z, level = position.level),
        )
        player.session.queue(payload)
    }

    /**
     * Encodes and queues a batch of zone payloads (ground-item add/delete,
     * area sounds) as a single enclosed zone update.
     *
     * Revision-240 ground-object and sound-area packets must be encoded this
     * way rather than sent as bare following updates.
     */
    fun queueEnclosedZonePayload(player: Player, position: WorldPosition, payloads: Collection<ZoneProt>) {
        if (payloads.isEmpty()) return

        val zone = localZone(player, position) ?: return

        val cache = ZonePartialEnclosedCacheBuffer(supportedClients = listOf(OldSchoolClientType.DESKTOP))
        val encoded = cache.computeZoneForClient(
            client = OldSchoolClientType.DESKTOP,
            pendingTickProtList = payloads,
        )

        // UpdateZonePartialEnclosed retains the ByteBuf; release the buffer
        // returned by computeZoneForClient once the packet wrapper owns it.
        val message = try {
            UpdateZonePartialEnclosed(zoneX = zone.x, zoneZ = zone.z, level = position.level, payload = encoded)
        } finally {
            encoded.release()
        }

        player.session.queue(message)
    }

    /** Runs [action] for every connected player whose scene currently includes [position]. */
    inline fun broadcastToVisible(context: GameContext, position: WorldPosition, action: (Player) -> Unit) {
        for (player in context.players.snapshot()) {
            if (player.isDisconnected || !isVisible(player, position)) continue
            action(player)
        }
    }
}
