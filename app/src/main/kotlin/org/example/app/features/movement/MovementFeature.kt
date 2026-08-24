package org.example.app.features.movement

import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.misc.user.MoveMinimapClick
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * Player walking vertical slice.
 *
 * Incoming world/minimap clicks replace the player's active route.
 * Gameplay systems can use the same [MovementService] for interaction
 * routing, keeping all movement authoritative and collision-aware.
 */
class MovementFeature(
    private val movement: MovementService,
) : Feature {

    override val id: String =
        "movement"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<MoveGameClick> { packet ->
                movement.request(
                    player = this,
                    x = packet.x,
                    z = packet.z,
                    keyCombination =
                        packet.keyCombination,
                )
            }

            addListener<MoveMinimapClick> { packet ->
                movement.request(
                    player = this,
                    x = packet.x,
                    z = packet.z,
                    keyCombination =
                        packet.keyCombination,
                )
            }
        }

        registrar.onCycleStart(
            priority = MOVEMENT_PRIORITY,
        ) { context ->
            for (
                player in
                context.players.snapshot()
            ) {
                if (!player.isDisconnected) {
                    movement.cycle(
                        player
                    )
                }
            }
        }
    }

    private companion object {
        const val MOVEMENT_PRIORITY: Int =
            10
    }
}