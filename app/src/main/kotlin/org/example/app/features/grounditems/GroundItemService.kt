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
 * Lifecycle:
 *
 * inventory
 *     -> pending drop
 *     -> ground item
 *     -> pickup OR timeout
 *     -> deleted
 *
 * Ground items are transient runtime state and are intentionally not persisted.
 */
internal class GroundItemService(
    private val config: GroundItemConfig,
) {

    /**
     * Every currently-existing ground stack.
     */
    private val groundItems =
        LinkedHashMap<Long, GroundItem>()

    /**
     * Inventory drops waiting to be committed during the next game cycle.
     */
    private val pendingDrops =
        ArrayDeque<PendingGroundItemDrop>()

    /**
     * Ground items removed by pickup and waiting for their ObjDel broadcast.
     */
    private val pendingRemovals =
        ArrayDeque<GroundItem>()

    /**
     * Runtime identity for individual world stacks.
     */
    private var nextUid: Long =
        1L

    /**
     * Queues a newly-dropped inventory item.
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
     * Removes one matching ground item from authoritative world state.
     *
     * The client is not trusted to create an item by sending an arbitrary
     * ground-item interaction packet.
     */
    fun take(
        itemId: Int,
        position: WorldPosition,
    ): ItemStack? {
        val groundItem =
            groundItems.values
                .firstOrNull { candidate ->
                    candidate.item.id ==
                        itemId &&
                        candidate.position ==
                        position
                }
                ?: return null

        groundItems.remove(
            groundItem.uid
        )

        pendingRemovals.addLast(
            groundItem
        )

        return groundItem.item
    }

    /**
     * Advances the complete ground-item lifecycle once per game tick.
     */
    fun cycle(
        context: GameContext,
    ) {
        /*
         * Process pickups first.
         */
        flushPendingRemovals(
            context = context,
        )

        /*
         * Garbage-collect old objects.
         */
        expireGroundItems(
            context = context,
        )

        /*
         * Spawn newly-dropped objects last so they receive their full configured
         * lifetime.
         */
        flushPendingDrops(
            context = context,
        )
    }

    /**
     * Replays all active ground objects into a newly-loaded player scene.
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
            player.groundItemSyncState

        if (
            syncState.initialized &&
            syncState.baseZoneX ==
            baseZoneX &&
            syncState.baseZoneZ ==
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
                    position = groundItem.position,
                )
            ) {
                queueAdd(
                    player = player,
                    groundItem = groundItem,
                )
            }
        }

        syncState.initialized =
            true

        syncState.baseZoneX =
            baseZoneX

        syncState.baseZoneZ =
            baseZoneZ
    }

    /**
     * Converts staged drops into live world objects.
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
                groundItem = groundItem,
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
     * Broadcasts deletion for items already taken by players.
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
                groundItem = groundItem,
            )
        }
    }

    /**
     * Garbage collector for abandoned world items.
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
                groundItem.ticksRemaining <=
                0
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
                groundItem = groundItem,
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
                    position = groundItem.position,
                )
            ) {
                continue
            }

            queueAdd(
                player = player,
                groundItem = groundItem,
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
                    position = groundItem.position,
                )
            ) {
                continue
            }

            queueDelete(
                player = player,
                groundItem = groundItem,
            )
        }
    }

    /**
     * Sends one ground-object spawn.
     *
     * Important:
     *
     * OSRS item definitions use ground action slot 3 for "Take".
     *
     * Therefore op3 is enabled here.
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

                    opFlags =
                        TAKE_OP_FLAGS,
                ),
        )
    }

    /**
     * Sends one exact ground-object deletion.
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
     * Revision-240 ground-object packets must be encoded inside
     * UpdateZonePartialEnclosed.
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
         * UpdateZonePartialEnclosed retains the ByteBuf.
         *
         * Release the reference returned by computeZoneForClient after creating
         * the packet wrapper.
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
     * Converts an absolute world coordinate to the containing zone's
     * south-western tile relative to the player's build area.
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
     * Checks whether an absolute tile belongs to the player's currently-loaded
     * static scene.
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

        const val ZONE_TILE_MASK: Int =
            -8

        const val BUILD_AREA_SIZE: Int =
            104

        /**
         * Ground action slots:
         *
         * op1 = ground action index 0
         * op2 = ground action index 1
         * op3 = ground action index 2
         * op4 = ground action index 3
         * op5 = ground action index 4
         *
         * Standard OSRS items put "Take" in ground action index 2, therefore
         * op3 must be enabled.
         */
        val TAKE_OP_FLAGS: Byte =
            OpFlags.ofOps(
                op1 = false,
                op2 = false,
                op3 = true,
                op4 = false,
                op5 = false,
            )
    }
}

/**
 * Tracks which build area has received a complete ground-item synchronization.
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