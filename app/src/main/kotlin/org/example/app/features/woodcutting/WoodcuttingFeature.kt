package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.engine.GameContext
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.MovementService
import org.example.app.features.world.WorldLocService

/**
 * Woodcutting vertical slice.
 *
 * Responsibilities:
 *
 * - consume Chop-down loc interactions;
 * - identify supported trees;
 * - reject currently depleted runtime locs;
 * - route to a collision-safe interaction tile;
 * - hand control to WoodcuttingActionService;
 * - advance active actions once per game cycle.
 */
class WoodcuttingFeature(
    private val movement:
        MovementService,

    private val worldLocs:
        WorldLocService,
) : Feature {

    private val axeService =
        WoodcuttingAxeService()

    private val actionService =
        WoodcuttingActionService(
            axeService =
                axeService,

            worldLocs =
                worldLocs,
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
         * Movement runs at priority 10.
         *
         * Woodcutting executes afterwards so arrival/interruption caused by
         * movement in the same cycle is seen immediately.
         */
        registrar.onCycleStart(
            priority =
                WOODCUTTING_PRIORITY,
        ) { context ->
            for (
                player in
                context.players.snapshot()
            ) {
                if (
                    !player.isDisconnected
                ) {
                    processPlayer(
                        context = context,
                        player = player,
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
                locId =
                    packet.id,
            )
                ?: return

        val position =
            WorldPosition(
                x =
                    packet.x,
                z =
                    packet.z,
                level =
                    player.position.level,
            )

        /*
         * The client normally cannot send Chop-down for our stump because the
         * stump has no enabled ops, but the server still validates runtime
         * state rather than trusting the client.
         */
        if (
            worldLocs.isOverridden(
                position =
                    position,
                shape =
                    tree.locShape,
            )
        ) {
            println(
                "[Woodcutting] '${player.username}' selected a depleted " +
                    "${tree.name} at " +
                    "${position.x}," +
                    "${position.z}," +
                    "${position.level}."
            )

            return
        }

        val state =
            player.woodcuttingState

        /*
         * Selecting another tree replaces any previous walking or chopping
         * interaction.
         */
        actionService.cancel(
            player = player,
            state = state,
        )

        val target =
            WoodcuttingTarget(
                tree =
                    tree,
                locId =
                    packet.id,
                position =
                    position,
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
                x =
                    target.position.x,
                z =
                    target.position.z,
                maximumRadius =
                    MAXIMUM_INTERACTION_RADIUS,
                keyCombination =
                    if (
                        packet.controlKey
                    ) {
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
        context: GameContext,
        player: Player,
    ) {
        val state =
            player.woodcuttingState

        if (
            state.action != null
        ) {
            actionService.cycle(
                context = context,
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