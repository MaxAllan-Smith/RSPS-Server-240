package org.example.app.features.world

import net.rsprot.protocol.game.outgoing.map.RebuildNormalV2
import org.example.app.core.player.Player

/** Keeps the client's normal map build area centered as the player travels. */
internal class WorldMapService {
    fun initialize(player: Player) {
        recenterState(player)
    }

    fun synchronize(player: Player) {
        val state = player.worldMapState
        val baseZoneX = state.baseZoneX ?: return
        val baseZoneZ = state.baseZoneZ ?: return

        val localX = player.position.x - (baseZoneX shl 3)
        val localZ = player.position.z - (baseZoneZ shl 3)

        if (
            localX in SAFE_LOCAL_MIN until SAFE_LOCAL_MAX &&
            localZ in SAFE_LOCAL_MIN until SAFE_LOCAL_MAX
        ) {
            return
        }

        val position = player.position

        player.infos.updateRootBuildAreaCenteredOnPlayer(
            position.x,
            position.z,
        )

        player.session.queue(
            RebuildNormalV2(
                zoneX = position.zoneX,
                zoneZ = position.zoneZ,
                worldArea = ROOT_WORLD,
            )
        )

        recenterState(player)

        println(
            "[Map] Rebuilt world around '${player.username}' at " +
                "${position.x},${position.z},${position.level}."
        )
    }

    private fun recenterState(player: Player) {
        player.worldMapState.apply {
            baseZoneX = player.position.zoneX - BUILD_AREA_RADIUS_ZONES
            baseZoneZ = player.position.zoneZ - BUILD_AREA_RADIUS_ZONES
        }
    }

    private companion object {
        const val ROOT_WORLD = 0
        const val BUILD_AREA_RADIUS_ZONES = 6
        const val SAFE_LOCAL_MIN = 16
        const val SAFE_LOCAL_MAX = 88
    }
}
