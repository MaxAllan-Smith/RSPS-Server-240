package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.movement.MovementService

/**
 * Woodcutting interaction vertical slice.
 *
 * Current responsibilities:
 *
 * - identify supported trees;
 * - route to a collision-safe interaction tile;
 * - detect arrival;
 * - resolve a usable axe.
 *
 * Chopping actions, rewards and depletion are implemented separately.
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

        registrar.onCycleStart(
            priority =
                WOODCUTTING_PRIORITY,
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
            player.woodcuttingState
                .clear()

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

        /*
         * The player may already be standing on the selected interaction
         * tile. processTarget handles that on the same/next cycle.
         */
    }

    private fun processTarget(
        player: Player,
    ) {
        val state =
            player.woodcuttingState

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

    private companion object {
        const val CHOP_OPTION: Int =
            1

        /*
         * Three tiles is deliberately a small interaction search window.
         *
         * It handles larger static loc footprints without allowing
         * interactions from arbitrary distances.
         */
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