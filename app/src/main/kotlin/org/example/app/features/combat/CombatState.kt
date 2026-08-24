package org.example.app.features.combat

import org.example.app.core.player.Player

internal class CombatState {
    var initialized: Boolean = false
}

internal val Player.combatState: CombatState
    get() =
        featureState.getOrPut(
            CombatState::class,
            ::CombatState,
        )