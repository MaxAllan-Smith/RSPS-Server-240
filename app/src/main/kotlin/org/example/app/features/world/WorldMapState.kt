package org.example.app.features.world

import org.example.app.core.player.Player

/** Tracks the normal 104x104 client build area independently per player. */
internal data class WorldMapState(
    var baseZoneX: Int? = null,
    var baseZoneZ: Int? = null,
)

internal val Player.worldMapState: WorldMapState
    get() = featureState.getOrPut(WorldMapState::class, ::WorldMapState)
