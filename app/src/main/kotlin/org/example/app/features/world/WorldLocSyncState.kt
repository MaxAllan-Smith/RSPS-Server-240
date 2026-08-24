package org.example.app.features.world

import org.example.app.core.player.Player

/**
 * Tracks which dynamic world-loc revision a player has seen.
 *
 * Dynamic locs are global runtime state, while synchronization state is
 * naturally player-specific and therefore belongs in Player.featureState.
 */
internal data class WorldLocSyncState(
    var synchronizedRevision: Long = -1L,
    var synchronizedBaseZoneX: Int? = null,
    var synchronizedBaseZoneZ: Int? = null,
)

internal val Player.worldLocSyncState:
    WorldLocSyncState
    get() =
        featureState.getOrPut(
            WorldLocSyncState::class,
            ::WorldLocSyncState,
        )