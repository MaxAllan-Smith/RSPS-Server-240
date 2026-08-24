package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.MovementService

/**
 * Woodcutting vertical slice.
 *
 * Responsibilities:
 *
 * - consume Chop-down loc interactions;
 * - identify supported tree definitions;
 * - route the player to a collision-safe interaction tile;
 * - hand control to WoodcuttingActionService after arrival;
 * - advance active Woodcutting actions once per game cycle.
 */
class WoodcuttingFeature(
    private val movement:
        MovementService,
) : Feature {

    private val axeService =
        WoodcuttingAxeService()

    private val actionService =
        WoodcuttingActionService(
            axeService =
                axeService,
        )

    override val id: String =
        "woodcutting"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<OpLocV2> { packet ->
                handleTreeInteraction(
                    player = this,
                    packet = packet,
                )
            }
        }

        /*
         * Movement runs first at priority 10.
         *
         * Woodcutting executes afterwards so arrival or interruption caused by
         * movement during this cycle is observed immediately.
         */
        registrar.onCycleStart(
            priority =
                WOODCUTTING_PRIORITY,
        ) { context ->
            for (
                player in
                context.players.snapshot()
            ) {
                if (!player.isDisconnected) {
                    processPlayer(
                        player
                    )
                }
            }
        }
    }

    private fun handleTreeInteraction(
        player: Player,
        packet: OpLocV2,
    ) {
        if (
            packet.op !=
            CHOP_OPTION
        ) {
            return
        }

        val tree =
            WoodcuttingTree.find(
                locId = packet.id,
            )
                ?: return

        val state =
            player.woodcuttingState

        /*
         * Selecting another tree replaces any previous walking/chopping
         * interaction.
         */
        actionService.cancel(
            player = player,
            state = state,
        )

        val target =
            WoodcuttingTarget(
                tree = tree,
                locId = packet.id,
                position =
                    WorldPosition(
                        x = packet.x,
                        z = packet.z,
                        level =
                            player.position.level,
                    ),
            )

        state.target =
            target

        println(
            "[Woodcutting] '${player.username}' selected " +
                "${tree.name} " +
                "id=${packet.id} " +
                "at ${packet.x}," +
                "${packet.z}," +
                "${player.position.level}."
        )

        val approach =
            movement.requestNear(
                player = player,
                x = target.position.x,
                z = target.position.z,
                maximumRadius =
                    MAXIMUM_INTERACTION_RADIUS,
                keyCombination =
                    if (packet.controlKey) {
                        CONTROL_KEY
                    } else {
                        NO_KEYS
                    },
            )

        if (approach == null) {
            state.clear()

            println(
                "[Woodcutting] '${player.username}' could not reach " +
                    "${tree.name} " +
                    "at ${packet.x}," +
                    "${packet.z}," +
                    "${player.position.level}."
            )

            return
        }

        target.approachPosition =
            approach
    }

    private fun processPlayer(
        player: Player,
    ) {
        val state =
            player.woodcuttingState

        /*
         * Once chopping has begun the action service owns this player's
         * Woodcutting state until success, interruption or cancellation.
         */
        if (state.action != null) {
            actionService.cycle(
                player = player,
                state = state,
            )

            return
        }

        val target =
            state.target
                ?: return

        val approach =
            target.approachPosition
                ?: return

        if (
            player.position !=
            approach
        ) {
            return
        }

        movement.clear(
            player = player,
        )

        println(
            "[Woodcutting] '${player.username}' reached " +
                "${target.tree.name} " +
                "at ${target.position.x}," +
                "${target.position.z}," +
                "${target.position.level} " +
                "from ${approach.x}," +
                "${approach.z}."
        )

        actionService.start(
            player = player,
            state = state,
            target = target,
        )
    }

    private companion object {
        const val CHOP_OPTION: Int =
            1

        const val MAXIMUM_INTERACTION_RADIUS: Int =
            3

        const val NO_KEYS: Int =
            0

        const val CONTROL_KEY: Int =
            1

        const val WOODCUTTING_PRIORITY: Int =
            20
    }
}