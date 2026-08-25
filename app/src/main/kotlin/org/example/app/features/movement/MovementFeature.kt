package org.example.app.features.movement

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.misc.user.MoveMinimapClick
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player

/**
 * Player walking/running vertical slice.
 */
class MovementFeature(
    private val movement:
        MovementService,
) : Feature {

    override val id: String =
        "movement"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {

            addListener<MoveGameClick> { packet ->
                movement.request(
                    player =
                        this,

                    x =
                        packet.x,

                    z =
                        packet.z,

                    keyCombination =
                        packet.keyCombination,
                )
            }

            addListener<MoveMinimapClick> { packet ->
                movement.request(
                    player =
                        this,

                    x =
                        packet.x,

                    z =
                        packet.z,

                    keyCombination =
                        packet.keyCombination,
                )
            }

            /*
             * If3Button is shared with other gameplay features, so observe the
             * run orb through the global listener.
             */
            addGlobalListener { player, message ->
                if (
                    message is
                    If3Button
                ) {
                    handleInterfaceButton(
                        player =
                            player,

                        packet =
                            message,
                    )
                }
            }
        }

        registrar.onCycleStart(
            priority =
                MOVEMENT_PRIORITY,
        ) { context ->
            for (
                player in
                context.players.snapshot()
            ) {
                if (
                    !player.isDisconnected
                ) {
                    movement.cycle(
                        player
                    )
                }
            }
        }

        /*
         * Run energy and the initial player-info movement mode must exist
         * before RSProt builds this player's information packet.
         */
        registrar.beforeInfoUpdate(
            priority =
                MOVEMENT_SYNC_PRIORITY,
        ) { _, player ->
            movement.initializeClientState(
                player
            )
        }
    }

    private fun handleInterfaceButton(
        player: Player,
        packet: If3Button,
    ) {
        if (
            packet.interfaceId !=
            ORBS_INTERFACE_ID ||
            packet.componentId !=
            RUN_BUTTON_COMPONENT_ID ||
            packet.op !=
            RUN_BUTTON_OPERATION
        ) {
            return
        }

        movement.toggleRunning(
            player
        )
    }

    private companion object {

        const val MOVEMENT_PRIORITY: Int =
            10

        const val MOVEMENT_SYNC_PRIORITY: Int =
            10

        /**
         * orbs:runbutton
         */
        const val ORBS_INTERFACE_ID: Int =
            160

        const val RUN_BUTTON_COMPONENT_ID: Int =
            28

        const val RUN_BUTTON_OPERATION: Int =
            1
    }
}