package org.example.app.features.world

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenTop
import net.rsprot.protocol.game.outgoing.map.RebuildLoginV2
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import org.example.app.core.player.Player

internal object WorldBootstrapper {
    fun beforeInfoUpdate(player: Player) {
        val state =
            player.featureState.getOrPut(
                WorldBootstrapState::class,
                ::WorldBootstrapState,
            )

        if (state.rebuildPending) {
            queueLoginRebuild(player)
            state.rebuildPending = false
        }

        if (state.clientStatePending) {
            check(!state.rebuildPending) {
                "Initial client state cannot be queued before the login rebuild."
            }

            queueInitialClientState(player)
            state.clientStatePending = false
        }
    }

    fun markMapBuildComplete(player: Player) {
        val state =
            player.featureState.getOrPut(
                WorldBootstrapState::class,
                ::WorldBootstrapState,
            )

        if (state.mapBuildComplete) {
            return
        }

        state.mapBuildComplete = true

        println(
            "[Map] '${player.username}' finished building " +
                "the game world at " +
                "${player.position.x},${player.position.z},${player.position.level}."
        )
    }

    private fun queueLoginRebuild(player: Player) {
        val position = player.position

        println(
            "[Login] Initializing GPI for " +
                "'${player.username}' index=${player.index}"
        )

        player.session.queue(
            RebuildLoginV2(
                zoneX = position.zoneX,
                zoneZ = position.zoneZ,
                worldArea = ROOT_WORLD,
                playerInfo = player.infos.playerInfo,
            )
        )
    }

    private fun queueInitialClientState(player: Player) {
        val topLevelInterface =
            if (player.resizable) {
                RESIZABLE_TOP_LEVEL_INTERFACE
            } else {
                FIXED_TOP_LEVEL_INTERFACE
            }

        player.session.queue(IfOpenTop(topLevelInterface))
        player.session.queue(VarpReset)
        player.session.queue(ChatFilterSettings(0, 0))
        player.session.queue(HideNpcOps(false))
        player.session.queue(HideLocOps(false))
        player.session.queue(HideObjOps(false))
        player.session.queue(ResetAnims)
        player.session.queue(MinimapToggle(0))

        println(
            "[Login] Initial client state queued for " +
                "'${player.username}' " +
                "topLevel=$topLevelInterface " +
                "resizable=${player.resizable}"
        )
    }

    private const val ROOT_WORLD = 0
    private const val FIXED_TOP_LEVEL_INTERFACE = 548
    private const val RESIZABLE_TOP_LEVEL_INTERFACE = 161
}
