package org.example.app.core.feature

import org.example.app.core.config.ServerConfig
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.items.ItemOnItemDispatcher
import org.example.app.core.world.GroundItemService
import org.example.app.core.world.WorldCollision
import org.example.app.core.world.WorldLocService
import org.example.app.core.world.collision.RoutePlanner

/**
 * Cross-feature application services supplied at composition time.
 *
 * Everything here is constructed once by [org.example.app.core.server.ServerApplication]
 * before any feature exists, so [org.example.app.features.FeatureCatalog] only
 * has to wire concrete [Feature]s together rather than also owning core
 * service lifecycles.
 */
data class FeatureDependencies(
    val config: ServerConfig,
    val itemDefinitions: ItemDefinitionRepository,
    val collision: WorldCollision,
    val routePlanner: RoutePlanner,
    val worldLocs: WorldLocService,
    val groundItems: GroundItemService,
    val itemOnItem: ItemOnItemDispatcher,
)