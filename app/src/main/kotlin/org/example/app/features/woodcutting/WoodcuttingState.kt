package org.example.app.features.woodcutting

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Transient per-player Woodcutting state.
 *
 * Active skilling interactions must never be persisted across logout.
 */
internal class WoodcuttingState {

    var target:
        WoodcuttingTarget? =
        null

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
 * Active chopping loop.
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