package org.example.app.features.npcs

import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcAvatar
import org.example.app.core.player.WorldPosition

/**
 * Authoritative runtime NPC.
 */
internal data class Npc(
    val index: Int,
    val spawn: NpcSpawn,
    val avatar: NpcAvatar,
) {

    /**
     * Current authoritative server position.
     */
    var position: WorldPosition =
        spawn.position

    /**
     * Number of game cycles before another random wander attempt.
     */
    var wanderDelay: Int =
        0
}