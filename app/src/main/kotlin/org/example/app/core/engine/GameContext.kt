package org.example.app.core.engine

import net.rsprot.protocol.api.NetworkService
import org.example.app.core.cache.OpenRs2XteaRepository
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.persistence.PlayerPersistenceRepository
import org.example.app.core.player.Player
import org.example.app.core.player.PlayerManager
import org.example.app.core.vars.VarbitDefinitionRepository
import org.example.app.core.world.WorldCollision
import java.nio.file.Path

/**
 * Runtime services exposed to feature hooks.
 *
 * Add generic server capabilities here only when they are truly
 * cross-cutting.
 */
data class GameContext(
    val networkService: NetworkService<Player>,
    val players: PlayerManager,
    val varbits: VarbitDefinitionRepository,
    val persistence: PlayerPersistenceRepository,
    val itemDefinitions: ItemDefinitionRepository,
    val cacheDirectory: Path,
    val collision: WorldCollision,
    val xteas: OpenRs2XteaRepository
)