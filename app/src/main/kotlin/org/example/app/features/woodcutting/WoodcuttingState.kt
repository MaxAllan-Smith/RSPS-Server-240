package org.example.app.features.woodcutting

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Transient per-player Woodcutting state.
 *
 * Active interactions are runtime state only and are deliberately not
 * persisted across logout.
 */
internal class WoodcuttingState {

    /**
     * Tree the player is currently approaching.
     */
    var target:
        WoodcuttingTarget? =
        null

    /**
     * Active chopping process after reaching the tree.
     */
    var action:
        WoodcuttingAction? =
        null

    fun clear() {
        target = null
        action = null
    }
}

/**
 * Tree selected by the player.
 *
 * [approachPosition] is the exact collision-safe endpoint chosen by the
 * shared movement system.
 */
internal data class WoodcuttingTarget(
    val tree: WoodcuttingTree,
    val locId: Int,
    val position: WorldPosition,
    var approachPosition:
        WorldPosition? = null,
)

/**
 * Active chopping process.
 *
 * Resource acquisition is probability based. There is deliberately no
 * attempt counter or guaranteed-success threshold.
 *
 * [ticksUntilRoll] controls when the next independent success roll occurs.
 */
internal data class WoodcuttingAction(
    val target: WoodcuttingTarget,
    val approachPosition: WorldPosition,
    var axe: WoodcuttingAxe,
    var ticksUntilRoll: Int,
)

internal val Player.woodcuttingState:
    WoodcuttingState
    get() =
        featureState.getOrPut(
            WoodcuttingState::class,
            ::WoodcuttingState,
        )