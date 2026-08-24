package org.example.app.core.engine

import net.rsprot.protocol.api.NetworkService
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.persistence.PlayerPersistenceRepository
import org.example.app.core.player.Player
import org.example.app.core.player.PlayerManager
import org.example.app.core.vars.VarbitDefinitionRepository
import org.example.app.core.world.WorldCollision
import java.nio.file.Path

/**
 * Stable runtime services available to feature hooks.
 *
 * Only cross-cutting server capabilities belong here. Feature-owned state lives
 * in Player.featureState and feature-to-feature details remain outside core.
 */
data class GameContext(
    val networkService: NetworkService<Player>,
    val players: PlayerManager,
    val varbits: VarbitDefinitionRepository,
    val persistence: PlayerPersistenceRepository,
    val itemDefinitions: ItemDefinitionRepository,
    val collision: WorldCollision,
    val cacheDirectory: Path,
)
