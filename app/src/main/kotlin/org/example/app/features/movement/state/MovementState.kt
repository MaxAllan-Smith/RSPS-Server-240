package org.example.app.features.movement.state

import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition

/**
 * Per-player transient movement state.
 */
internal class MovementState {

    val steps =
        ArrayDeque<WorldPosition>()

    /**
     * Persistent run-orb toggle.
     */
    var runEnabled: Boolean =
        false

    /**
     * Modifier from the most recent manual movement request.
     *
     * Revision 240:
     *
     * 0 = ordinary request
     * 1 = Ctrl held
     * 2 = Ctrl + Shift
     */
    var requestedKeyCombination: Int =
        0

    /**
     * Run energy in hundredths of one percent.
     *
     * 10,000 = 100%.
     */
    var runEnergy: Int =
        MAX_RUN_ENERGY

    /**
     * The last energy value sent to the client.
     *
     * This prevents sending UpdateRunEnergy when nothing changed.
     */
    var synchronizedRunEnergy: Int =
        -1

    /**
     * Initial player-info movement state has been initialized.
     */
    var clientStateInitialized: Boolean =
        false

    companion object {

        const val MAX_RUN_ENERGY: Int =
            10_000

        const val MIN_RUN_ENERGY: Int =
            0
    }
}

internal val Player.movementState:
    MovementState
    get() =
        featureState.getOrPut(
            MovementState::class,
            ::MovementState,
        )