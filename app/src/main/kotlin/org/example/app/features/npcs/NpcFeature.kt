package org.example.app.features.npcs

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

/**
 * World NPC vertical slice.
 *
 * Current responsibilities:
 *
 * - allocate configured NPCs;
 * - retain authoritative runtime state;
 * - perform lightweight collision-aware local wandering;
 * - synchronize movement through RSProt NPC avatars.
 *
 * Interaction and combat remain separate subsequent slices.
 */
internal class NpcFeature(
    private val npcs: NpcService,
) : Feature {

    override val id: String =
        "npcs"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
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