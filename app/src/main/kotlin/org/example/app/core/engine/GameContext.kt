package org.example.app.core.engine

import net.rsprot.protocol.api.NetworkService
import org.example.app.core.persistence.PlayerPersistenceRepository
import org.example.app.core.player.Player
import org.example.app.core.player.PlayerManager
import org.example.app.core.vars.VarbitDefinitionRepository
import java.nio.file.Path

/**
 * Runtime services exposed to feature hooks.
 *
 * Add generic server capabilities here only when they are truly cross-cutting.
 * Feature-to-feature APIs should remain in their owning feature packages.
 */
data class GameContext(
    val networkService: NetworkService<Player>,
    val players: PlayerManager,
    val varbits: VarbitDefinitionRepository,
    val persistence: PlayerPersistenceRepository,
    val cacheDirectory: Path,
)