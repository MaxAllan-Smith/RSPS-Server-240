package org.example.app.features.woodcutting

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Per-player transient Woodcutting state.
 *
 * Active interactions are deliberately not persisted.
 */
internal class WoodcuttingState {

    var target:
        WoodcuttingTarget? =
        null

    fun clear() {
        target = null
    }
}

/**
 * Tree currently selected by the player.
 *
 * [approachPosition] is the exact collision-safe tile selected by the
 * movement system for this interaction.
 */
internal data class WoodcuttingTarget(
    val tree: WoodcuttingTree,
    val locId: Int,
    val position: WorldPosition,
    var approachPosition: WorldPosition? = null,
)

internal val Player.woodcuttingState:
    WoodcuttingState
    get() =
        featureState.getOrPut(
            WoodcuttingState::class,
            ::WoodcuttingState,
        )