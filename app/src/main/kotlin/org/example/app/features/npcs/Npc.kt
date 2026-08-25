package org.example.app.features.npcs

import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcAvatar

/**
 * Authoritative runtime NPC.
 *
 * The RSProt avatar is the protocol-facing representation of this NPC.
 * Gameplay state will be added here incrementally when interaction and combat
 * are implemented.
 */
internal data class Npc(
    val index: Int,
    val spawn: NpcSpawn,
    val avatar: NpcAvatar,
)