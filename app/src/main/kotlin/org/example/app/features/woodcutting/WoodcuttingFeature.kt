package org.example.app.features.woodcutting

import net.rsprot.protocol.game.incoming.locs.OpLocV2
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player

/**
 * Handles player interactions with woodcutting trees.
 *
 * The feature currently establishes the verified interaction boundary.
 * Actual chopping is introduced incrementally once interaction handling
 * has been confirmed against the revision-240 client.
 */
class WoodcuttingFeature : Feature {

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
    }

    private fun handleTreeInteraction(
        player: Player,
        packet: OpLocV2,
    ) {
        /*
         * Chop is the first location option on the trees we've
         * verified. Other location options should remain available
         * for unrelated systems.
         */
        if (packet.op != CHOP_OPTION) {
            return
        }

        val tree =
            WoodcuttingTree.find(
                locId = packet.id,
            )
                ?: return

        println(
            "[Woodcutting] '${player.username}' selected " +
                "${tree.name} " +
                "id=${packet.id} " +
                "at ${packet.x},${packet.z}," +
                "${player.position.level}."
        )
    }

    private companion object {
        const val CHOP_OPTION: Int =
            1
    }
}