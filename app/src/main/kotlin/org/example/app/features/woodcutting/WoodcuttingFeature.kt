package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.movement.MovementService
import kotlin.math.abs
import kotlin.math.max

/**
 * Woodcutting interaction vertical slice.
 *
 * Current responsibilities:
 *
 * - identify supported trees;
 * - remember the selected tree;
 * - route toward the tree;
 * - detect interaction range;
 * - resolve the best usable axe.
 *
 * Chopping rolls, animations, resources, XP and tree depletion are
 * introduced in subsequent implementation steps.
 */
class WoodcuttingFeature(
    private val movement: MovementService,
) : Feature {

    private val axeService =
        WoodcuttingAxeService()

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
         * Movement currently runs at priority 10.
         *
         * Woodcutting executes afterwards so a player who reaches
         * interaction range during the current cycle can immediately
         * continue into interaction validation.
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
         * Do not generate another route if the player is already
         * standing beside the selected tree.
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
         * The interaction itself has reached the target, so any
         * remaining move-near route can be discarded.
         */
        movement.clear(
            player = player,
        )

        println(
            "[Woodcutting] '${player.username}' reached " +
                "${target.tree.name} " +
                "at ${target.position.x}," +
                "${target.position.z}," +
                "${target.position.level}."
        )

        validateAxe(
            player = player,
            target = target,
        )

        state.clear()
    }

    private fun validateAxe(
        player: Player,
        target: WoodcuttingTarget,
    ) {
        val selection =
            axeService.findBestUsable(
                player = player,
            )

        if (selection != null) {
            println(
                "[Woodcutting] '${player.username}' ready to chop " +
                    "${target.tree.name} using " +
                    "${selection.axe.name} " +
                    "from ${selection.source.description}."
            )

            return
        }

        val available =
            axeService.findBestAvailable(
                player = player,
            )

        if (available == null) {
            player.sendGameMessage(
                "You do not have an axe which you can use."
            )

            println(
                "[Woodcutting] '${player.username}' has no axe " +
                    "available for ${target.tree.name}."
            )

            return
        }

        val currentLevel =
            player.skills.currentLevel(
                Skill.WOODCUTTING
            )

        player.sendGameMessage(
            "You need a Woodcutting level of " +
                "${available.axe.woodcuttingLevel} " +
                "to use a ${available.axe.name.lowercase()}."
        )

        println(
            "[Woodcutting] '${player.username}' cannot use " +
                "${available.axe.name}: " +
                "woodcutting=$currentLevel, " +
                "required=${available.axe.woodcuttingLevel}."
        )
    }

    /**
     * The currently supported tree definitions occupy one tile.
     *
     * Chebyshev distance 1 allows horizontal, vertical or diagonal
     * interaction while excluding the blocked tree tile itself.
     *
     * Once generic loc dimensions are available this can be replaced
     * by a reusable loc-boundary interaction check.
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

        return max(
            deltaX,
            deltaZ,
        ) == ADJACENT_DISTANCE
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

        const val WOODCUTTING_PRIORITY: Int =
            20
    }
}