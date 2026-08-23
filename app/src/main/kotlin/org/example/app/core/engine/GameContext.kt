package org.example.app.core.engine

import net.rsprot.protocol.api.NetworkService
import org.example.app.core.player.Player
import org.example.app.core.player.PlayerManager

/**
 * Runtime services exposed to feature hooks.
 *
 * Add generic server capabilities here only when they are truly cross-cutting.
 * Feature-to-feature APIs should remain in their owning feature packages.
 */
data class GameContext(
    val networkService: NetworkService<Player>,
    val players: PlayerManager,
)
