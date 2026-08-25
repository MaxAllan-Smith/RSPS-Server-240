package org.example.app.core.feature

import org.example.app.core.config.ServerConfig
import org.example.app.core.items.ItemDefinitionRepository
import org.example.app.core.world.WorldCollision

/**
 * Cross-feature application services supplied at composition time.
 */
data class FeatureDependencies(
    val config: ServerConfig,
    val itemDefinitions: ItemDefinitionRepository,
    val collision: WorldCollision,
)