package org.example.app.core.world

import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.game.outgoing.zone.payload.ObjAdd
import net.rsprot.protocol.game.outgoing.zone.payload.ObjDel
import org.example.app.core.engine.GameContext
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import java.util.ArrayDeque

/**
 * Server-authoritative repository and lifecycle service for temporary ground
 * items.
 *
 * This is generic world state, not a gameplay feature: any vertical slice
 * may drop or take a ground item through it.
 *
 * Lifecycle: inventory -> pending drop -> ground item -> pickup OR timeout
 * -> deleted. Ground items are transient runtime state and are intentionally
 * not persisted.
 */
class GroundItemService(
    private val config: GroundItemConfig,
) {

    /** Every currently-existing ground stack. */
    private val groundItems = LinkedHashMap<Long, GroundItem>()

    /** Inventory drops waiting to be committed during the next game cycle. */
    private val pendingDrops = ArrayDeque<PendingGroundItemDrop>()

    /** Ground items removed by pickup and waiting for their ObjDel broadcast. */
    private val pendingRemovals = ArrayDeque<GroundItem>()

    /** Runtime identity for individual world stacks. */
    private var nextUid: Long = 1L

    /** Queues a newly-dropped inventory item. */
    fun drop(item: ItemStack, position: WorldPosition) {
        pendingDrops.addLast(PendingGroundItemDrop(item, position))
    }

    /**
     * Removes one matching ground item from authoritative world state.
     *
     * The client is not trusted to create an item by sending an arbitrary
     * ground-item interaction packet.
     */
    fun take(itemId: Int, position: WorldPosition): ItemStack? {
        val groundItem = groundItems.values.firstOrNull {
            it.item.id == itemId && it.position == position
        } ?: return null

        groundItems.remove(groundItem.uid)
        pendingRemovals.addLast(groundItem)
        return groundItem.item
    }

    /** Advances the complete ground-item lifecycle once per game tick. */
    fun cycle(context: GameContext) {
        // Process pickups first.
        flushPendingRemovals(context)
        // Garbage-collect old objects.
        expireGroundItems(context)
        // Spawn newly-dropped objects last so they receive their full configured lifetime.
        flushPendingDrops(context)
    }

    /** Replays all active ground objects into a newly-loaded player scene. */
    fun synchronize(player: Player) {
        val mapState = player.worldMapState
        val baseZoneX = mapState.baseZoneX ?: return
        val baseZoneZ = mapState.baseZoneZ ?: return

        val syncState = player.groundItemSyncState
        if (syncState.initialized && syncState.baseZoneX == baseZoneX && syncState.baseZoneZ == baseZoneZ) {
            return
        }

        for (groundItem in groundItems.values) {
            if (ZoneBroadcast.isVisible(player, groundItem.position)) {
                queueAdd(player, groundItem)
            }
        }

        syncState.initialized = true
        syncState.baseZoneX = baseZoneX
        syncState.baseZoneZ = baseZoneZ
    }

    /** Converts staged drops into live world objects. */
    private fun flushPendingDrops(context: GameContext) {
        while (pendingDrops.isNotEmpty()) {
            val pending = pendingDrops.removeFirst()
            val groundItem = GroundItem(
                uid = nextUid++,
                item = pending.item,
                position = pending.position,
                ticksRemaining = config.despawnTicks,
            )
            groundItems[groundItem.uid] = groundItem
            broadcastAdd(context, groundItem)

            println(
                "[GroundItems] Spawned item=${groundItem.item.id} amount=${groundItem.item.amount} at " +
                    "${groundItem.position.x},${groundItem.position.z},${groundItem.position.level}; " +
                    "despawn=${groundItem.ticksRemaining} ticks.",
            )
        }
    }

    /** Broadcasts deletion for items already taken by players. */
    private fun flushPendingRemovals(context: GameContext) {
        while (pendingRemovals.isNotEmpty()) {
            broadcastDelete(context, pendingRemovals.removeFirst())
        }
    }

    /** Garbage collector for abandoned world items. */
    private fun expireGroundItems(context: GameContext) {
        if (groundItems.isEmpty()) return

        val expired = mutableListOf<GroundItem>()
        for (groundItem in groundItems.values) {
            groundItem.ticksRemaining--
            if (groundItem.ticksRemaining <= 0) expired += groundItem
        }

        for (groundItem in expired) {
            groundItems.remove(groundItem.uid)
            broadcastDelete(context, groundItem)

            println(
                "[GroundItems] Garbage-collected item=${groundItem.item.id} " +
                    "amount=${groundItem.item.amount} at " +
                    "${groundItem.position.x},${groundItem.position.z},${groundItem.position.level}.",
            )
        }
    }

    private fun broadcastAdd(context: GameContext, groundItem: GroundItem) {
        ZoneBroadcast.broadcastToVisible(context, groundItem.position) { player -> queueAdd(player, groundItem) }
    }

    private fun broadcastDelete(context: GameContext, groundItem: GroundItem) {
        ZoneBroadcast.broadcastToVisible(context, groundItem.position) { player -> queueDelete(player, groundItem) }
    }

    /**
     * Sends one ground-object spawn.
     *
     * Standard OSRS item definitions use ground action slot 3 for "Take",
     * so op3 is the only enabled operation here.
     */
    private fun queueAdd(player: Player, groundItem: GroundItem) {
        ZoneBroadcast.queueEnclosedZonePayload(
            player,
            groundItem.position,
            listOf(
                ObjAdd(
                    id = groundItem.item.id,
                    quantity = groundItem.item.amount,
                    xInZone = groundItem.position.x and ZoneBroadcast.ZONE_MASK,
                    zInZone = groundItem.position.z and ZoneBroadcast.ZONE_MASK,
                    opFlags = TAKE_OP_FLAGS,
                ),
            ),
        )
    }

    /** Sends one exact ground-object deletion. */
    private fun queueDelete(player: Player, groundItem: GroundItem) {
        ZoneBroadcast.queueEnclosedZonePayload(
            player,
            groundItem.position,
            listOf(
                ObjDel(
                    id = groundItem.item.id,
                    quantity = groundItem.item.amount,
                    xInZone = groundItem.position.x and ZoneBroadcast.ZONE_MASK,
                    zInZone = groundItem.position.z and ZoneBroadcast.ZONE_MASK,
                ),
            ),
        )
    }

    private data class PendingGroundItemDrop(val item: ItemStack, val position: WorldPosition)

    private companion object {
        /**
         * Ground action slots: op1..op5 map to ground action indices 0..4.
         * Standard OSRS items put "Take" in ground action index 2, so op3
         * must be enabled.
         */
        val TAKE_OP_FLAGS: Byte = OpFlags.ofOps(op1 = false, op2 = false, op3 = true, op4 = false, op5 = false)
    }
}

/** Tracks which build area has received a complete ground-item synchronization. */
private data class GroundItemSyncState(
    var initialized: Boolean = false,
    var baseZoneX: Int? = null,
    var baseZoneZ: Int? = null,
)

private val Player.groundItemSyncState: GroundItemSyncState
    get() = featureState.getOrPut(GroundItemSyncState::class, ::GroundItemSyncState)
