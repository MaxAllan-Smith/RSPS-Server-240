package org.example.app.features.combat.state

import org.example.app.core.player.Player

internal class CombatEquipmentSyncState {
    var synchronizedRevision: Int = -1
}

internal val Player.combatEquipmentSyncState:
    CombatEquipmentSyncState
    get() =
        featureState.getOrPut(
            CombatEquipmentSyncState::class,
            ::CombatEquipmentSyncState,
        )