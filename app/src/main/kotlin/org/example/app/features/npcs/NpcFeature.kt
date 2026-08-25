package org.example.app.features.npcs

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * World NPC vertical slice.
 *
 * This first version is intentionally synchronization-only:
 *
 * - allocate configured NPCs;
 * - retain authoritative runtime instances;
 * - advance RSProt NPC avatar state each cycle.
 *
 * Interaction, walking, combat, death and respawning are subsequent slices.
 */
internal class NpcFeature(
    private val npcs:
        NpcService =
            NpcService(),
) : Feature {

    override val id: String =
        "npcs"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        /*
         * NPC avatar state must be prepared before RSProt computes entity
         * information for the cycle.
         *
         * Login currently runs at -1000 and movement at 10, so this keeps NPC
         * world state early in the cycle without interfering with either.
         */
        registrar.onCycleStart(
            priority =
                NPC_CYCLE_PRIORITY,
        ) { context ->
            npcs.beginCycle(
                context
            )
        }
    }

    private companion object {

        const val NPC_CYCLE_PRIORITY: Int =
            -900
    }
}