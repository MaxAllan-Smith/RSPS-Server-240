package org.example.app.features.combat

import org.example.app.core.player.Player

internal class CombatState {
    var initialized: Boolean = false

    var style: Int = 0

    var autoRetaliate: Boolean = true
}

internal val Player.combatState: CombatState
    get() =
        featureState.getOrPut(
            CombatState::class,
            ::CombatState,
        )