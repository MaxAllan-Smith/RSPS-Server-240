package org.example.app.core.protocol

import net.rsprot.protocol.game.outgoing.info.util.PacketResult
import net.rsprot.protocol.game.outgoing.info.util.isEmpty
import net.rsprot.protocol.game.outgoing.info.util.safeReleaseOrThrow
import net.rsprot.protocol.message.OutgoingGameMessage
import org.example.app.core.player.Player
import org.example.app.core.player.PlayerManager

/**
 * Generic RSProt information-protocol output pass.
 *
 * This contains no gameplay rules. It translates the information snapshots
 * already built by RSProt into the required packet sequence for each session.
 */
class RsProtInfoSynchronizer(
    private val players: PlayerManager,
) {
    fun queue(player: Player) {
        val infoPackets = player.infos.getPackets()
        val root = infoPackets.rootWorldInfoPackets

        player.session.queue(root.activeWorld)
        player.session.queue(root.npcUpdateOrigin)

        if (!queueResult(player, "world-entity-info", root.worldEntityInfo)) {
            return
        }

        if (!queueResult(player, "player-info", root.playerInfo)) {
            return
        }

        if (root.npcInfo.isEmpty()) {
            root.npcInfo.safeReleaseOrThrow()
        } else if (!queueResult(player, "npc-info", root.npcInfo)) {
            return
        }

        check(infoPackets.activeWorlds.isEmpty()) {
            "Dynamic world entities are not implemented yet."
        }

        // RSProt requires restoring root world after dynamic/root info output.
        player.session.queue(root.activeWorld)
    }

    private fun <T : OutgoingGameMessage> queueResult(
        player: Player,
        name: String,
        result: PacketResult<T>,
    ): Boolean {
        val packet = result.getOrNull()

        if (packet != null) {
            player.session.queue(packet)
            return true
        }

        val cause = result.exceptionOrNull()

        System.err.println(
            "[Engine] RSProt $name failed for ${player.username}: $cause"
        )

        players.disconnect(player, "RSProt $name failure")
        return false
    }
}
