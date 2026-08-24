package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.MovementService
import kotlin.math.abs
import kotlin.math.max

/**
 * Woodcutting interaction vertical slice.
 *
 * This stage handles:
 *
 * - identifying supported trees;
 * - remembering the selected tree;
 * - routing through the shared movement system;
 * - detecting when interaction range is reached.
 *
 * Chopping rolls, animations, logs, XP and depletion are intentionally
 * implemented in later stages.
 */
class WoodcuttingFeature(
    private val movement: MovementService,
) : Feature {

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
         * Woodcutting checks interaction range afterwards so a player
         * reaching the tree during this cycle is recognized immediately.
         */
        registrar.onCycleStart(
            priority = WOODCUTTING_PRIORITY,
        ) { context ->
            for (
                player in
                context.players.snapshot()
            ) {
                if (!player.isDisconnected) {
                    processTarget(
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
        if (packet.op != CHOP_OPTION) {
            return
        }

        val tree =
            WoodcuttingTree.find(
                locId = packet.id,
            )
                ?: return

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

        player.woodcuttingState.target =
            target

        println(
            "[Woodcutting] '${player.username}' selected " +
                "${tree.name} " +
                "id=${packet.id} " +
                "at ${packet.x},${packet.z}," +
                "${player.position.level}."
        )

        /*
         * If already beside the tree there is no reason to create a
         * route. The cycle hook will immediately recognize interaction
         * range.
         */
        if (
            isInInteractionRange(
                player = player,
                target = target,
            )
        ) {
            return
        }

        val routed =
            movement.request(
                player = player,
                x = target.position.x,
                z = target.position.z,
                keyCombination =
                    if (packet.controlKey) {
                        CONTROL_KEY
                    } else {
                        NO_KEYS
                    },
            )

        if (!routed) {
            player.woodcuttingState
                .clear()

            println(
                "[Woodcutting] '${player.username}' could not reach " +
                    "${tree.name} " +
                    "at ${packet.x},${packet.z}," +
                    "${player.position.level}."
            )
        }
    }

    private fun processTarget(
        player: Player,
    ) {
        val state =
            player.woodcuttingState

        val target =
            state.target
                ?: return

        if (
            !isInInteractionRange(
                player = player,
                target = target,
            )
        ) {
            return
        }

        /*
         * Stop consuming any redundant move-near route tiles once the
         * player has reached valid interaction distance.
         */
        movement.clear(
            player
        )

        state.clear()

        println(
            "[Woodcutting] '${player.username}' reached " +
                "${target.tree.name} " +
                "at ${target.position.x}," +
                "${target.position.z}," +
                "${target.position.level}."
        )
    }

    /**
     * The currently verified trees are one-tile locations.
     *
     * A player may interact from any horizontally, vertically or
     * diagonally adjacent tile, but may not occupy the tree tile itself.
     *
     * When we later import complete loc dimensions from the cache this
     * becomes a generic loc-boundary distance check instead.
     */
    private fun isInInteractionRange(
        player: Player,
        target: WoodcuttingTarget,
    ): Boolean {
        if (
            player.position.level !=
            target.position.level
        ) {
            return false
        }

        val deltaX =
            abs(
                player.position.x -
                    target.position.x
            )

        val deltaZ =
            abs(
                player.position.z -
                    target.position.z
            )

        val distance =
            max(
                deltaX,
                deltaZ,
            )

        return distance == ADJACENT_DISTANCE
    }

    private companion object {
        const val CHOP_OPTION: Int =
            1

        const val ADJACENT_DISTANCE: Int =
            1

        const val NO_KEYS: Int =
            0

        const val CONTROL_KEY: Int =
            1

        /*
         * Movement's cycle hook runs at priority 10. This must execute
         * afterwards so we inspect the player's newly-updated position.
         */
        const val WOODCUTTING_PRIORITY: Int =
            20
    }
}