package org.example.app.features.movement

import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.misc.user.MoveMinimapClick
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.world.WorldCollision

/**
 * Player walking vertical slice.
 *
 * Incoming clicks replace the current route immediately. RSMod validates the
 * route against server collision; the game cycle then consumes one tile.
 */
class MovementFeature(
    collision: WorldCollision,
) : Feature {
    private val movement =
        MovementService(
            planner = RoutePlanner(collision),
        )

    override val id: String = "movement"

    override fun install(registrar: FeatureRegistrar) {
        registrar.packets {
            addListener<MoveGameClick> { packet ->
                movement.request(
                    player = this,
                    x = packet.x,
                    z = packet.z,
                    keyCombination = packet.keyCombination,
                )
            }

            addListener<MoveMinimapClick> { packet ->
                movement.request(
                    player = this,
                    x = packet.x,
                    z = packet.z,
                    keyCombination = packet.keyCombination,
                )
            }
        }

        registrar.onCycleStart(priority = MOVEMENT_PRIORITY) { context ->
            for (player in context.players.snapshot()) {
                if (!player.isDisconnected) {
                    movement.cycle(player)
                }
            }
        }
    }

    private companion object {
        const val MOVEMENT_PRIORITY = 10
    }
}
