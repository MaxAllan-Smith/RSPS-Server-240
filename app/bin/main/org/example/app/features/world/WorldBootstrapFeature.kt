package org.example.app.features.world

import net.rsprot.protocol.game.incoming.misc.client.MapBuildComplete
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * Initial world/login-scene vertical slice.
 *
 * It owns the first RebuildLoginV2, initial gameframe/client-state packets and
 * MAP_BUILD_COMPLETE handling. Nothing in the core player model knows these
 * states exist.
 */
class WorldBootstrapFeature : Feature {
    override val id: String = "world-bootstrap"

    override fun install(registrar: FeatureRegistrar) {
        registrar.packets {
            addListener<MapBuildComplete> { _ ->
                WorldBootstrapper.markMapBuildComplete(this)
            }
        }

        registrar.beforeInfoUpdate { _, player ->
            WorldBootstrapper.beforeInfoUpdate(player)
        }
    }
}
