package org.example.app.features.npcs

import org.example.app.core.engine.GameContext
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.RoutePlanner
import kotlin.math.abs
import kotlin.random.Random

/**
 * Process-world authoritative NPC registry.
 *
 * RSProt owns protocol representation while this service owns gameplay state,
 * placement and movement.
 */
internal class NpcService(
    private val planner: RoutePlanner,
) {

    private val npcs:
        MutableList<Npc> =
        mutableListOf()

    private var initialized:
        Boolean =
        false

    /**
     * Runs before NPC mutations for the cycle.
     *
     * postUpdate clears the previous cycle's RSProt movement state so any walk
     * issued later in this method belongs to the current cycle.
     */
    fun beginCycle(
        context: GameContext,
    ) {
        if (!initialized) {
            initialize(context)

            initialized =
                true

            return
        }

        for (npc in npcs) {
            npc.avatar.postUpdate()
        }

        for (npc in npcs) {
            processWandering(npc)
        }
    }

    private fun initialize(
        context: GameContext,
    ) {
        var nextIndex =
            FIRST_NPC_INDEX

        for (spawn in LumbridgeNpcSpawns.all) {
            val avatar =
                context
                    .networkService
                    .npcAvatarFactory
                    .alloc(
                        index =
                            nextIndex,

                        id =
                            spawn.id,

                        level =
                            spawn.position.level,

                        x =
                            spawn.position.x,

                        z =
                            spawn.position.z,

                        direction =
                            spawn.direction,
                    )

            val npc =
                Npc(
                    index =
                        nextIndex,

                    spawn =
                        spawn,

                    avatar =
                        avatar,
                )

            npc.wanderDelay =
                randomWanderDelay()

            npcs.add(npc)

            println(
                "[NPCs] Spawned index=${npc.index} " +
                    "id=${spawn.id} " +
                    "name='${spawn.name}' " +
                    "at ${spawn.position.x}," +
                    "${spawn.position.z}," +
                    "${spawn.position.level}."
            )

            nextIndex++
        }

        println(
            "[NPCs] Spawned ${npcs.size} Lumbridge NPCs."
        )
    }

    /**
     * Gives stationary world NPCs lightweight local roaming.
     *
     * An NPC:
     *
     * - waits several game cycles between attempts;
     * - selects one of the eight neighbouring tiles;
     * - remains inside its spawn roaming radius;
     * - asks the existing RSMod-backed RoutePlanner whether that exact adjacent
     *   tile can be reached;
     * - only then updates both authoritative position and RSProt avatar.
     */
    private fun processWandering(
        npc: Npc,
    ) {
        if (npc.wanderDelay > 0) {
            npc.wanderDelay--

            return
        }

        npc.wanderDelay =
            randomWanderDelay()

        /*
         * NPCs do not have to move every time their timer expires. This makes
         * local population movement less mechanical.
         */
        if (
            Random.nextInt(
                WANDER_ATTEMPT_CHANCE_DENOMINATOR
            ) != 0
        ) {
            return
        }

        val direction =
            WANDER_DIRECTIONS.random()

        val target =
            WorldPosition(
                x =
                    npc.position.x +
                        direction.deltaX,

                z =
                    npc.position.z +
                        direction.deltaZ,

                level =
                    npc.position.level,
            )

        if (
            !insideSpawnRadius(
                npc =
                    npc,

                target =
                    target,
            )
        ) {
            return
        }

        val route =
            planner.route(
                start =
                    npc.position,

                destination =
                    target,
            )

        /*
         * The selected target is adjacent. A valid exact movement should
         * therefore consist of exactly one route step ending on that tile.
         *
         * Anything else is rejected; NPC wandering must never take a longer
         * route merely to reach a neighbouring coordinate.
         */
        if (
            route.size != 1 ||
            route.first() != target
        ) {
            return
        }

        npc.avatar.walk(
            deltaX =
                direction.deltaX,

            deltaZ =
                direction.deltaZ,
        )

        npc.position =
            target
    }

    private fun insideSpawnRadius(
        npc: Npc,
        target: WorldPosition,
    ): Boolean {
        if (
            target.level !=
            npc.spawn.position.level
        ) {
            return false
        }

        val deltaX =
            abs(
                target.x -
                    npc.spawn.position.x
            )

        val deltaZ =
            abs(
                target.z -
                    npc.spawn.position.z
            )

        return maxOf(
            deltaX,
            deltaZ,
        ) <= WANDER_RADIUS
    }

    private fun randomWanderDelay(): Int =
        Random.nextInt(
            WANDER_DELAY_MIN_TICKS,
            WANDER_DELAY_MAX_TICKS + 1,
        )

    private data class WanderDirection(
        val deltaX: Int,
        val deltaZ: Int,
    )

    private companion object {

        const val FIRST_NPC_INDEX: Int =
            1

        /**
         * NPCs remain within three tiles of their original spawn.
         */
        const val WANDER_RADIUS: Int =
            3

        /**
         * 600ms game cycles:
         *
         * 3..7 ticks = 1.8..4.2 seconds between potential wander decisions.
         */
        const val WANDER_DELAY_MIN_TICKS: Int =
            3

        const val WANDER_DELAY_MAX_TICKS: Int =
            7

        /**
         * Only one in two timer expiries actually attempts movement.
         */
        const val WANDER_ATTEMPT_CHANCE_DENOMINATOR: Int =
            2

        val WANDER_DIRECTIONS:
            List<WanderDirection> =
            listOf(
                WanderDirection(
                    deltaX = -1,
                    deltaZ = 1,
                ),
                WanderDirection(
                    deltaX = 0,
                    deltaZ = 1,
                ),
                WanderDirection(
                    deltaX = 1,
                    deltaZ = 1,
                ),
                WanderDirection(
                    deltaX = -1,
                    deltaZ = 0,
                ),
                WanderDirection(
                    deltaX = 1,
                    deltaZ = 0,
                ),
                WanderDirection(
                    deltaX = -1,
                    deltaZ = -1,
                ),
                WanderDirection(
                    deltaX = 0,
                    deltaZ = -1,
                ),
                WanderDirection(
                    deltaX = 1,
                    deltaZ = -1,
                ),
            )
    }
}