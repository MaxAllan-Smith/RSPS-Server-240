package org.example.app.features.woodcutting

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Transient per-player Woodcutting state.
 *
 * Active skilling interactions are deliberately not persisted. Logging out,
 * moving away, selecting another interaction, etc. starts from clean runtime
 * state on the next session/action.
 */
internal class WoodcuttingState {

    /**
     * Tree the player is currently walking toward.
     */
    var target:
        WoodcuttingTarget? =
        null

    /**
     * Active chopping process after the player reaches the tree.
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
 * [approachPosition] is the exact collision-safe endpoint selected by the
 * movement system.
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
 * [ticksUntilRoll] controls when the next resource roll occurs.
 *
 * [rollAttempts] counts how many resource rolls have already happened during
 * this action. It is used to place an upper bound on unlucky RNG streaks.
 *
 * The animation itself starts before this action begins counting down, meaning
 * the player always visibly swings the axe before a successful chop may occur.
 */
internal data class WoodcuttingAction(
    val target: WoodcuttingTarget,
    val approachPosition: WorldPosition,

    var axe: WoodcuttingAxe,

    var ticksUntilRoll: Int,

    var rollAttempts: Int =
        0,
)

internal val Player.woodcuttingState:
    WoodcuttingState
    get() =
        featureState.getOrPut(
            WoodcuttingState::class,
            ::WoodcuttingState,
        )