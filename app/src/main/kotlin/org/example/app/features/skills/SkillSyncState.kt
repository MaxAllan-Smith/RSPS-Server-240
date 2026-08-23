package org.example.app.features.skills

import org.example.app.core.player.Player

internal class SkillSyncState(
    var initialSyncSent: Boolean = false,
)

internal val Player.skillSyncState: SkillSyncState
    get() =
        featureState.getOrPut(
            SkillSyncState::class,
            ::SkillSyncState,
        )