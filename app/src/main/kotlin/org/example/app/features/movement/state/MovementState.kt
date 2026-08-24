package org.example.app.features.movement.state

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

internal class MovementState {
    val steps = ArrayDeque<WorldPosition>()
    var requestedKeyCombination: Int = 0
}

internal val Player.movementState: MovementState
    get() = featureState.getOrPut(MovementState::class, ::MovementState)
