package org.example.app.features.combat.state

import org.example.app.core.player.Player

/** Tracks the last equipment revision sent to the client so equipment resync stays revision-gated. */
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