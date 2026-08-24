package org.example.app.features.world

import net.rsprot.protocol.game.incoming.misc.client.MapBuildComplete
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * Owns login world bootstrap and subsequent normal map rebuilds.
 *
 * Login still uses RebuildLoginV2. After movement crosses the safe center of
 * the 104x104 scene, WorldMapService emits RebuildNormalV2 and recenters the
 * RSProt root build area.
 */
class WorldBootstrapFeature : Feature {
    private val mapService = WorldMapService()

    override val id: String = "world-bootstrap"

    override fun install(registrar: FeatureRegistrar) {
        registrar.packets {
            addListener<MapBuildComplete> { _ ->
                WorldBootstrapper.markMapBuildComplete(this)
            }
        }

        registrar.beforeInfoUpdate { _, player ->
            val loginRebuildQueued =
                WorldBootstrapper.beforeInfoUpdate(player)

            if (loginRebuildQueued) {
                mapService.initialize(player)
            } else {
                mapService.synchronize(player)
            }
        }
    }
}
