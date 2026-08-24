package org.example.app.features.world

import net.rsprot.protocol.game.incoming.misc.client.MapBuildComplete
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * Owns client world bootstrap, normal scene rebuilding and shared dynamic
 * world-loc runtime state.
 *
 * Login still uses RebuildLoginV2. Movement-driven scene transitions use
 * RebuildNormalV2 through WorldMapService.
 *
 * Dynamic world locs are synchronized after either form of scene setup.
 */
class WorldBootstrapFeature(
    private val worldLocs:
        WorldLocService,
) : Feature {

    private val mapService =
        WorldMapService()

    override val id: String =
        "world-bootstrap"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<MapBuildComplete> { _ ->
                WorldBootstrapper
                    .markMapBuildComplete(
                        this
                    )
            }
        }

        /*
         * World timers advance before movement and gameplay interactions.
         */
        registrar.onCycleStart(
            priority =
                WORLD_STATE_PRIORITY,
        ) { context ->
            worldLocs.cycle(
                context
            )
        }

        registrar.beforeInfoUpdate {
                _,
                player,
            ->
            val loginRebuildQueued =
                WorldBootstrapper
                    .beforeInfoUpdate(
                        player
                    )

            if (loginRebuildQueued) {
                mapService.initialize(
                    player
                )
            } else {
                mapService.synchronize(
                    player
                )
            }

            /*
             * A login/rebuild loads static cache locs first. Runtime overrides
             * are applied afterwards so depleted trees remain depleted.
             */
            worldLocs.synchronize(
                player
            )
        }
    }

    private companion object {
        const val WORLD_STATE_PRIORITY: Int =
            0
    }
}