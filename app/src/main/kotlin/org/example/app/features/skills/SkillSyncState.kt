package org.example.app.features.skills

import org.example.app.core.player.Player

/** Tracks whether a player's initial full skills sync has already been sent. */
internal class SkillSyncState(
    var initialSyncSent: Boolean = false,
)

internal val Player.skillSyncState: SkillSyncState
    get() =
        featureState.getOrPut(
            SkillSyncState::class,
            ::SkillSyncState,
        )