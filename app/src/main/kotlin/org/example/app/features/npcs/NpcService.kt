package org.example.app.features.npcs

import org.example.app.core.engine.GameContext

/**
 * Process-world authoritative NPC registry.
 *
 * RSProt owns the protocol representation while this service owns the
 * gameplay/runtime representation and stable NPC indices.
 */
internal class NpcService {

    private val npcs:
        MutableList<Npc> =
        mutableListOf()

    private var initialized:
        Boolean =
        false

    /**
     * Called at the beginning of every game cycle.
     *
     * On the first cycle the configured NPCs are allocated into RSProt.
     *
     * On later cycles each avatar is post-updated before any future NPC
     * mutations occur. This establishes RSProt's previous-cycle coordinate
     * and extended-info state correctly.
     */
    fun beginCycle(
        context: GameContext,
    ) {
        if (!initialized) {
            initialize(context)

            initialized =
                true

            return
        }

        for (npc in npcs) {
            npc.avatar.postUpdate()
        }
    }

    private fun initialize(
        context: GameContext,
    ) {
        var nextIndex =
            FIRST_NPC_INDEX

        for (spawn in LumbridgeNpcSpawns.all) {
            val avatar =
                context
                    .networkService
                    .npcAvatarFactory
                    .alloc(
                        index =
                            nextIndex,

                        id =
                            spawn.id,

                        level =
                            spawn.position.level,

                        x =
                            spawn.position.x,

                        z =
                            spawn.position.z,

                        direction =
                            spawn.direction,
                    )

            val npc =
                Npc(
                    index =
                        nextIndex,

                    spawn =
                        spawn,

                    avatar =
                        avatar,
                )

            npcs.add(npc)

            println(
                "[NPCs] Spawned index=${npc.index} " +
                    "id=${spawn.id} " +
                    "name='${spawn.name}' " +
                    "at ${spawn.position.x}," +
                    "${spawn.position.z}," +
                    "${spawn.position.level}."
            )

            nextIndex++
        }

        println(
            "[NPCs] Spawned ${npcs.size} Lumbridge NPCs."
        )
    }

    private companion object {

        /**
         * NPC indices use their own protocol namespace, separate from player
         * indices.
         */
        const val FIRST_NPC_INDEX: Int =
            1
    }
}