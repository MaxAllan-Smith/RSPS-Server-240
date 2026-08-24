package org.example.app.features.grounditems

import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.common.client.OldSchoolClientType
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import net.rsprot.protocol.game.outgoing.zone.payload.ObjAdd
import net.rsprot.protocol.game.outgoing.zone.payload.ObjDel
import net.rsprot.protocol.message.ZoneProt
import org.example.app.core.engine.GameContext
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.world.worldMapState
import java.util.ArrayDeque

/**
 * Server-authoritative repository and lifecycle service for temporary ground
 * items.
 *
 * The server owns every item in [groundItems]. The client only receives a
 * projection of those items through RSProt zone packets.
 *
 * Lifecycle:
 *
 * inventory
 *     -> pending drop
 *     -> active ground item
 *     -> pickup OR expiry
 *     -> removed
 *
 * Expired items are physically removed from [groundItems], so dead entries do
 * not accumulate in memory.
 */
internal class GroundItemService(
    private val config: GroundItemConfig,
) {

    /**
     * Active items currently existing in the world.
     */
    private val groundItems =
        LinkedHashMap<Long, GroundItem>()

    /**
     * Incoming inventory interactions do not receive GameContext directly.
     *
     * Drops are therefore staged here and committed during the next game
     * cycle, where all nearby players can be synchronized safely.
     */
    private val pendingDrops =
        ArrayDeque<PendingGroundItemDrop>()

    /**
     * Pickups remove their authoritative ground-item entry immediately.
     *
     * The visual ObjDel broadcast is staged here until the next game cycle,
     * where GameContext is available.
     */
    private val pendingRemovals =
        ArrayDeque<GroundItem>()

    /**
     * Unique runtime identity for every individual floor stack.
     */
    private var nextUid: Long =
        1L

    /**
     * Stages an inventory item to become a world ground item.
     */
    fun drop(
        item: ItemStack,
        position: WorldPosition,
    ) {
        pendingDrops.addLast(
            PendingGroundItemDrop(
                item = item,
                position = position,
            )
        )
    }

    /**
     * Attempts to collect one matching ground item.
     *
     * Matching is entirely server-authoritative:
     *
     * - item id;
     * - exact absolute coordinate;
     * - height level.
     *
     * A malicious client cannot invent an item merely by sending OpObjV2.
     */
    fun take(
        itemId: Int,
        position: WorldPosition,
    ): ItemStack? {
        val matching =
            groundItems.values
                .firstOrNull { groundItem ->
                    groundItem.item.id ==
                        itemId &&
                        groundItem.position ==
                        position
                }
                ?: return null

        groundItems.remove(
            matching.uid
        )

        pendingRemovals.addLast(
            matching
        )

        return matching.item
    }

    /**
     * Advances the complete ground-item lifecycle once per 600ms game cycle.
     */
    fun cycle(
        context: GameContext,
    ) {
        /*
         * First remove items collected during packet processing.
         */
        flushPendingRemovals(
            context = context,
        )

        /*
         * Then age and garbage-collect existing world items.
         */
        expireGroundItems(
            context = context,
        )

        /*
         * Finally spawn newly-dropped items.
         *
         * Doing this last prevents a new item from immediately losing one tick
         * of its configured lifetime during the cycle in which it appears.
         */
        flushPendingDrops(
            context = context,
        )
    }

    /**
     * Resends active ground items after login or a scene rebuild.
     *
     * Without this, a player entering an area after an item was already dropped
     * would not know that item exists.
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

        val state =
            player.groundItemSyncState

        /*
         * Direct broadcasts handle changes while the player remains in the
         * same scene.
         *
         * A full resend is only needed when their scene itself changes.
         */
        if (
            state.initialized &&
            state.baseZoneX ==
            baseZoneX &&
            state.baseZoneZ ==
            baseZoneZ
        ) {
            return
        }

        for (
            groundItem in
            groundItems.values
        ) {
            if (
                isVisible(
                    player = player,
                    position =
                        groundItem.position,
                )
            ) {
                queueAdd(
                    player = player,
                    groundItem =
                        groundItem,
                )
            }
        }

        state.initialized =
            true

        state.baseZoneX =
            baseZoneX

        state.baseZoneZ =
            baseZoneZ
    }

    /**
     * Converts staged inventory drops into authoritative world objects.
     */
    private fun flushPendingDrops(
        context: GameContext,
    ) {
        while (
            pendingDrops.isNotEmpty()
        ) {
            val pending =
                pendingDrops.removeFirst()

            val groundItem =
                GroundItem(
                    uid =
                        nextUid++,

                    item =
                        pending.item,

                    position =
                        pending.position,

                    ticksRemaining =
                        config.despawnTicks,
                )

            groundItems[
                groundItem.uid
            ] =
                groundItem

            broadcastAdd(
                context = context,
                groundItem =
                    groundItem,
            )

            println(
                "[GroundItems] Spawned " +
                    "item=${groundItem.item.id} " +
                    "amount=${groundItem.item.amount} " +
                    "at ${groundItem.position.x}," +
                    "${groundItem.position.z}," +
                    "${groundItem.position.level}; " +
                    "despawn=${groundItem.ticksRemaining} ticks."
            )
        }
    }

    /**
     * Broadcasts world removals for items collected since the previous cycle.
     */
    private fun flushPendingRemovals(
        context: GameContext,
    ) {
        while (
            pendingRemovals.isNotEmpty()
        ) {
            val groundItem =
                pendingRemovals.removeFirst()

            broadcastDelete(
                context = context,
                groundItem =
                    groundItem,
            )
        }
    }

    /**
     * Ground-item garbage collector.
     *
     * Every active item loses one lifetime tick per server cycle.
     *
     * Once it reaches zero:
     *
     * - remove it from authoritative server memory;
     * - broadcast ObjDel to nearby players;
     * - leave no stale entry behind.
     */
    private fun expireGroundItems(
        context: GameContext,
    ) {
        if (
            groundItems.isEmpty()
        ) {
            return
        }

        val expired =
            ArrayList<GroundItem>()

        for (
            groundItem in
            groundItems.values
        ) {
            groundItem.ticksRemaining--

            if (
                groundItem.ticksRemaining <= 0
            ) {
                expired +=
                    groundItem
            }
        }

        for (
            groundItem in
            expired
        ) {
            groundItems.remove(
                groundItem.uid
            )

            broadcastDelete(
                context = context,
                groundItem =
                    groundItem,
            )

            println(
                "[GroundItems] Garbage-collected " +
                    "item=${groundItem.item.id} " +
                    "amount=${groundItem.item.amount} " +
                    "at ${groundItem.position.x}," +
                    "${groundItem.position.z}," +
                    "${groundItem.position.level}."
            )
        }
    }

    /**
     * Shows a newly-created ground item to every player whose current scene
     * contains it.
     */
    private fun broadcastAdd(
        context: GameContext,
        groundItem: GroundItem,
    ) {
        for (
            player in
            context.players.snapshot()
        ) {
            if (
                player.isDisconnected ||
                !isVisible(
                    player = player,
                    position =
                        groundItem.position,
                )
            ) {
                continue
            }

            queueAdd(
                player = player,
                groundItem =
                    groundItem,
            )
        }
    }

    /**
     * Removes a ground item from every player currently viewing it.
     */
    private fun broadcastDelete(
        context: GameContext,
        groundItem: GroundItem,
    ) {
        for (
            player in
            context.players.snapshot()
        ) {
            if (
                player.isDisconnected ||
                !isVisible(
                    player = player,
                    position =
                        groundItem.position,
                )
            ) {
                continue
            }

            queueDelete(
                player = player,
                groundItem =
                    groundItem,
            )
        }
    }

    /**
     * Creates an ObjAdd payload for this item.
     *
     * Revision 240 requires ObjAdd to be encoded inside
     * UpdateZonePartialEnclosed. It cannot be sent as a standalone zone packet.
     */
    private fun queueAdd(
        player: Player,
        groundItem: GroundItem,
    ) {
        queueEnclosedZonePayload(
            player = player,

            position =
                groundItem.position,

            payload =
                ObjAdd(
                    id =
                        groundItem.item.id,

                    quantity =
                        groundItem.item.amount,

                    xInZone =
                        groundItem.position.x and
                            ZONE_MASK,

                    zInZone =
                        groundItem.position.z and
                            ZONE_MASK,

                    /*
                     * Only floor-item operation 1 is enabled.
                     *
                     * That is the normal Take operation.
                     */
                    opFlags =
                        TAKE_ONLY_OP_FLAGS,
                ),
        )
    }

    /**
     * Creates an ObjDel payload for this item.
     *
     * ObjDel also must be encoded inside UpdateZonePartialEnclosed on revision
     * 240.
     *
     * The id and quantity must match the existing client-side floor object.
     */
    private fun queueDelete(
        player: Player,
        groundItem: GroundItem,
    ) {
        queueEnclosedZonePayload(
            player = player,

            position =
                groundItem.position,

            payload =
                ObjDel(
                    id =
                        groundItem.item.id,

                    quantity =
                        groundItem.item.amount,

                    xInZone =
                        groundItem.position.x and
                            ZONE_MASK,

                    zInZone =
                        groundItem.position.z and
                            ZONE_MASK,
                ),
        )
    }

    /**
     * Encodes one revision-240 zone payload into UpdateZonePartialEnclosed.
     *
     * RSProt explicitly rejects ObjAdd and ObjDel when they are submitted
     * directly to Session.queue().
     *
     * ZonePartialEnclosedCacheBuffer performs RSProt's own revision-specific
     * encoding. We therefore do not manually reproduce client opcodes or byte
     * transforms here.
     */
    private fun queueEnclosedZonePayload(
        player: Player,
        position: WorldPosition,
        payload: ZoneProt,
    ) {
        val localZone =
            localZone(
                player = player,
                position = position,
            )
                ?: return

        /*
         * This server currently advertises Desktop as its supported client
         * type, matching NetworkService startup.
         */
        val cache =
            ZonePartialEnclosedCacheBuffer(
                supportedClients =
                    listOf(
                        OldSchoolClientType.DESKTOP
                    )
            )

        val encodedPayload =
            cache.computeZoneForClient(
                client =
                    OldSchoolClientType.DESKTOP,

                pendingTickProtList =
                    listOf(
                        payload
                    ),
            )

        /*
         * UpdateZonePartialEnclosed retains the supplied ByteBuf.
         *
         * Release our original reference after the packet wrapper has retained
         * its own reference.
         */
        val message =
            try {
                UpdateZonePartialEnclosed(
                    zoneX =
                        localZone.x,

                    zoneZ =
                        localZone.z,

                    level =
                        position.level,

                    payload =
                        encodedPayload,
                )
            } finally {
                encodedPayload.release()
            }

        player.session.queue(
            message
        )
    }

    /**
     * Converts the absolute world zone containing [position] into the
     * south-western tile coordinate relative to the player's loaded build area.
     *
     * UpdateZonePartialEnclosed expects this absolute-within-build-area form,
     * not shifted zone indices.
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
     * Returns whether this absolute world coordinate currently belongs to the
     * player's loaded 104x104 scene.
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

    /**
     * Drop waiting to be committed into shared world state.
     */
    private data class PendingGroundItemDrop(
        val item: ItemStack,
        val position: WorldPosition,
    )

    /**
     * South-western coordinate of one 8x8 zone relative to the player's
     * current build area.
     */
    private data class LocalZone(
        val x: Int,
        val z: Int,
    )

    private companion object {

        /**
         * 8 tiles per zone.
         */
        const val ZONE_SHIFT: Int =
            3

        /**
         * Extracts a tile's 0..7 position within its current zone.
         */
        const val ZONE_MASK: Int =
            7

        /**
         * Clears the bottom three coordinate bits, producing the south-west
         * tile of the containing 8x8 zone.
         */
        const val ZONE_TILE_MASK: Int =
            -8

        /**
         * Normal OSRS static scene size.
         */
        const val BUILD_AREA_SIZE: Int =
            104

        /**
         * Only operation 1 should be available on spawned floor objects.
         */
        val TAKE_ONLY_OP_FLAGS: Byte =
            OpFlags.ofOps(
                op1 = true,
                op2 = false,
                op3 = false,
                op4 = false,
                op5 = false,
            )
    }
}

/**
 * Tracks the scene into which currently-live ground items were last
 * synchronized.
 *
 * While the scene remains unchanged, add/delete broadcasts keep the player
 * current. When a rebuild occurs, all active ground items are resent.
 */
private data class GroundItemSyncState(
    var initialized: Boolean =
        false,

    var baseZoneX: Int? =
        null,

    var baseZoneZ: Int? =
        null,
)

private val Player.groundItemSyncState:
    GroundItemSyncState
    get() =
        featureState.getOrPut(
            GroundItemSyncState::class,
            ::GroundItemSyncState,
        )