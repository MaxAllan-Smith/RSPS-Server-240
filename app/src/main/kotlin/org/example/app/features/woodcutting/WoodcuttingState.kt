package org.example.app.features.woodcutting

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Per-player transient woodcutting interaction state.
 *
 * This is intentionally not persisted. Selecting a tree is an active
 * interaction rather than durable player progress.
 */
internal class WoodcuttingState {

    var target: WoodcuttingTarget? =
        null

    fun clear() {
        target = null
    }
}

/**
 * The tree interaction the player is currently attempting to reach.
 */
internal data class WoodcuttingTarget(
    val tree: WoodcuttingTree,
    val locId: Int,
    val position: WorldPosition,
)

internal val Player.woodcuttingState:
    WoodcuttingState
    get() =
        featureState.getOrPut(
            WoodcuttingState::class,
            ::WoodcuttingState,
        )