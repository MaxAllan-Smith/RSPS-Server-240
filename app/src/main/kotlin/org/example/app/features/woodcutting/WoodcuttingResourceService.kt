package org.example.app.features.woodcutting

import org.example.app.core.player.WorldPosition
import kotlin.random.Random

/**
 * Shared runtime state for live Woodcutting resources.
 *
 * The service models the important distinction between:
 *
 * - regular trees: one successful log, then depleted;
 * - timed trees: multiple logs while actively chopped;
 * - chance-depletion trees: independent depletion probability per success.
 *
 * Runtime state is keyed by the actual world loc so multiple players chopping
 * the same tree contribute to the same resource lifecycle.
 */
internal class WoodcuttingResourceService {

    /**
     * Active cutting duration accumulated for timer-based resources.
     */
    private val activeTicks =
        HashMap<
            ResourceKey,
            Int
        >()

    /**
     * A tree may be chopped by multiple players in the same cycle, but its
     * active lifetime advances only once per world game tick.
     */
    private val activeThisCycle =
        HashSet<ResourceKey>()

    /**
     * Called once before Woodcutting players are processed for a game cycle.
     */
    fun beginCycle() {
        activeThisCycle.clear()
    }

    /**
     * Marks a resource as actively being chopped during this cycle.
     */
    fun markActive(
        target: WoodcuttingTarget,
    ) {
        val tree =
            target.tree

        if (
            tree.depletion !is
            WoodcuttingDepletion.ActiveCutDuration
        ) {
            return
        }

        activeThisCycle +=
            key(
                target
            )

        activeTicks.putIfAbsent(
            key(target),
            0,
        )
    }

    /**
     * Completes the active-resource timer pass.
     *
     * Active resources gain one tick.
     *
     * Idle resources lose one tick. Once their accumulated active time reaches
     * zero, their timer state is discarded.
     *
     * This mirrors the important RSMod behaviour where partially-cut trees
     * recover their active-cut duration when left idle instead of remaining
     * permanently near depletion.
     */
    fun endCycle() {
        if (activeTicks.isEmpty()) {
            return
        }

        val iterator =
            activeTicks
                .entries
                .iterator()

        while (
            iterator.hasNext()
        ) {
            val entry =
                iterator.next()

            if (
                entry.key in
                activeThisCycle
            ) {
                entry.setValue(
                    entry.value + 1
                )

                continue
            }

            val reduced =
                entry.value - 1

            if (reduced <= 0) {
                iterator.remove()
            } else {
                entry.setValue(
                    reduced
                )
            }
        }
    }

    /**
     * Determines whether this successful product should deplete its resource.
     */
    fun shouldDeplete(
        target: WoodcuttingTarget,
        random: Random,
    ): Boolean =
        when (
            val depletion =
                target.tree
                    .depletion
        ) {
            WoodcuttingDepletion.Immediate ->
                true

            is WoodcuttingDepletion.ActiveCutDuration -> {
                val elapsed =
                    activeTicks[
                        key(target)
                    ] ?: 0

                elapsed >=
                    depletion.ticks
            }

            is WoodcuttingDepletion.ChancePerSuccess ->
                random.nextInt(
                    depletion.denominator
                ) == 0
        }

    /**
     * Removes transient lifetime state after a resource becomes depleted.
     */
    fun clear(
        target: WoodcuttingTarget,
    ) {
        val key =
            key(
                target
            )

        activeTicks.remove(
            key
        )

        activeThisCycle.remove(
            key
        )
    }

    private fun key(
        target: WoodcuttingTarget,
    ): ResourceKey =
        ResourceKey(
            position =
                target.position,

            shape =
                target.tree
                    .locShape,
        )

    private data class ResourceKey(
        val position: WorldPosition,
        val shape: Int,
    )
}