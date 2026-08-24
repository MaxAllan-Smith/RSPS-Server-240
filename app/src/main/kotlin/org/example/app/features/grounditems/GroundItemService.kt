package org.example.app.features.grounditems

import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
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
 * All expired items are physically removed from this repository, so the
 * garbage collector does not leave dead entries accumulating in memory.
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
     * Drops originate from an incoming network packet, where GameContext is
     * intentionally unavailable.
     *
     * They are therefore staged here and committed during the next game cycle.
     */
    private val pendingDrops =
        ArrayDeque<PendingGroundItemDrop>()

    /**
     * Pickups logically remove an item immediately.
     *
     * The corresponding ObjDel broadcast is staged until the next game cycle,
     * where GameContext is available.
     */
    private val pendingRemovals =
        ArrayDeque<GroundItem>()

    private var nextUid: Long =
        1L

    /**
     * Stages an inventory item to appear on the floor.
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
     * Attempts to remove one matching ground item for collection.
     *
     * Matching is intentionally server-side:
     *
     * - item id must match;
     * - absolute world coordinate must match;
     * - level must match.
     *
     * The client is never trusted to manufacture an item that does not exist in
     * this repository.
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
     * Advances all ground-item world state once per 600ms game cycle.
     */
    fun cycle(
        context: GameContext,
    ) {
        /*
         * 1. Broadcast items picked up since the previous game cycle.
         */
        flushPendingRemovals(
            context
        )

        /*
         * 2. Age existing items and garbage-collect expired entries.
         */
        expireGroundItems(
            context
        )

        /*
         * 3. Commit new drops.
         *
         * Doing this after the timer pass means a newly-spawned item receives
         * its complete configured lifetime rather than losing one tick before
         * it has even appeared client-side.
         */
        flushPendingDrops(
            context
        )
    }

    /**
     * Sends all currently-live ground items when the player's loaded scene
     * changes.
     *
     * This handles:
     *
     * - login near an existing ground item;
     * - normal scene rebuilds;
     * - walking into an area containing an existing item.
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
     * Converts pending network-side drops into actual world objects.
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
     * Broadcasts items that have been collected since the previous cycle.
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
     * Garbage collector.
     *
     * Entries are removed from [groundItems] once their lifetime reaches zero,
     * then ObjDel is broadcast to every player currently viewing the tile.
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
     * ObjAdd creates one ground object.
     *
     * Only operation 1 is enabled, which is the normal Take operation.
     */
    private fun queueAdd(
        player: Player,
        groundItem: GroundItem,
    ) {
        queueZonePayload(
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

                    opFlags =
                        TAKE_ONLY_OP_FLAGS,
                ),
        )
    }

    /**
     * ObjDel requires the same item id AND quantity as the existing client
     * object.
     */
    private fun queueDelete(
        player: Player,
        groundItem: GroundItem,
    ) {
        queueZonePayload(
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
     * Positions the client's zone pointer before sending a normal zone payload.
     */
    private fun queueZonePayload(
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

        player.session.queue(
            UpdateZonePartialFollows(
                zoneX =
                    localZone.x,

                zoneZ =
                    localZone.z,

                level =
                    position.level,
            )
        )

        player.session.queue(
            payload
        )
    }

    /**
     * Converts an absolute world zone to coordinates relative to the player's
     * currently-loaded scene.
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
     * Whether this absolute coordinate is inside the player's active 104x104
     * static build area.
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

    private data class PendingGroundItemDrop(
        val item: ItemStack,
        val position: WorldPosition,
    )

    private data class LocalZone(
        val x: Int,
        val z: Int,
    )

    private companion object {
        const val ZONE_SHIFT: Int =
            3

        const val ZONE_MASK: Int =
            7

        /*
         * Clear the lowest three coordinate bits to obtain the south-west tile
         * of an 8x8 zone.
         */
        const val ZONE_TILE_MASK: Int =
            -8

        const val BUILD_AREA_SIZE: Int =
            104

        /**
         * Floor objects should expose only Take at this stage.
         *
         * RSProt's flags represent op1..op5 as bit flags.
         */
        val TAKE_ONLY_OP_FLAGS: Byte =
            OpFlags.ofOps(
                true,
                false,
                false,
                false,
                false,
            )
    }
}

/**
 * Tracks the last scene into which active ground items were synchronized.
 *
 * Item additions/removals inside an unchanged scene are handled by direct
 * broadcasts, so we only need to resend the complete collection after a scene
 * transition.
 */
private data class GroundItemSyncState(
    var initialized: Boolean = false,
    var baseZoneX: Int? = null,
    var baseZoneZ: Int? = null,
)

private val Player.groundItemSyncState:
    GroundItemSyncState
    get() =
        featureState.getOrPut(
            GroundItemSyncState::class,
            ::GroundItemSyncState,
        )