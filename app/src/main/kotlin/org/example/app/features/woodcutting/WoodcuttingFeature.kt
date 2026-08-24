package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.engine.GameContext
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.MovementService
import org.example.app.features.world.WorldLocService

/**
 * Woodcutting vertical slice.
 *
 * Handles:
 *
 * - tree selection;
 * - routing;
 * - shared resource lifetime tracking;
 * - active chopping actions.
 */
class WoodcuttingFeature(
    private val movement:
        MovementService,

    private val worldLocs:
        WorldLocService,

    private val experience:
        ExperienceService,
) : Feature {

    private val axeService =
        WoodcuttingAxeService()

    private val resources =
        WoodcuttingResourceService()

    private val actionService =
        WoodcuttingActionService(
            axeService =
                axeService,

            worldLocs =
                worldLocs,

            resources =
                resources,

            experience =
                experience,
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

        registrar.onCycleStart(
            priority =
                WOODCUTTING_PRIORITY,
        ) { context ->
            processCycle(
                context
            )
        }
    }

    private fun processCycle(
        context: GameContext,
    ) {
        /*
         * Shared timer resources must only advance once per world game cycle,
         * even when several players chop the same tree.
         */
        resources.beginCycle()

        try {
            for (
                player in
                context.players.snapshot()
            ) {
                if (
                    player.isDisconnected
                ) {
                    continue
                }

                processPlayer(
                    context = context,
                    player = player,
                )
            }
        } finally {
            resources.endCycle()
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

        if (
            worldLocs.isOverridden(
                position =
                    position,

                shape =
                    tree.locShape,
            )
        ) {
            return
        }

        val state =
            player.woodcuttingState

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
                player =
                    player,

                x =
                    position.x,

                z =
                    position.z,

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
                    "${tree.name}."
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
            player
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