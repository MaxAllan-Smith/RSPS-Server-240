package org.example.app.features.world

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenTop
import net.rsprot.protocol.game.outgoing.map.RebuildLoginV2
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import net.rsprot.protocol.game.outgoing.misc.client.ResetAnims
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpReset
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.example.app.core.player.Player

/**
 * Queues the one-time client state a player needs immediately after login:
 * the RebuildLoginV2 scene, the top-level interface, default varps and
 * default audio settings.
 *
 * Owned by [WorldBootstrapFeature] rather than exposed as a global object, so
 * this stays an ordinary constructor-injected collaborator instead of a
 * static service locator.
 */
internal class LoginBootstrapService {

    /** @return true when the initial login rebuild was queued this cycle. */
    fun beforeInfoUpdate(
        player: Player,
    ): Boolean {
        val state =
            player.featureState.getOrPut(
                WorldBootstrapState::class,
                ::WorldBootstrapState,
            )

        var rebuilt =
            false

        if (
            state.rebuildPending
        ) {
            queueLoginRebuild(
                player
            )

            state.rebuildPending =
                false

            rebuilt =
                true
        }

        if (
            state.clientStatePending
        ) {
            check(
                !state.rebuildPending
            ) {
                "Initial client state cannot be queued before the login rebuild."
            }

            queueInitialClientState(
                player
            )

            state.clientStatePending =
                false
        }

        return rebuilt
    }

    fun markMapBuildComplete(
        player: Player,
    ) {
        val state =
            player.featureState.getOrPut(
                WorldBootstrapState::class,
                ::WorldBootstrapState,
            )

        if (
            state.mapBuildComplete
        ) {
            return
        }

        state.mapBuildComplete =
            true

        println(
            "[Map] '${player.username}' finished building the game world at " +
                "${player.position.x}," +
                "${player.position.z}," +
                "${player.position.level}."
        )
    }

    private fun queueLoginRebuild(
        player: Player,
    ) {
        val position =
            player.position

        println(
            "[Login] Initializing GPI for '${player.username}' index=${player.index}"
        )

        player.session.queue(
            RebuildLoginV2(
                zoneX =
                    position.zoneX,

                zoneZ =
                    position.zoneZ,

                worldArea =
                    ROOT_WORLD,

                playerInfo =
                    player.infos.playerInfo,
            )
        )
    }

    private fun queueInitialClientState(
        player: Player,
    ) {
        val topLevelInterface =
            if (
                player.resizable
            ) {
                RESIZABLE_TOP_LEVEL_INTERFACE
            } else {
                FIXED_TOP_LEVEL_INTERFACE
            }

        player.session.queue(
            IfOpenTop(
                topLevelInterface
            )
        )

        player.session.queue(
            VarpReset
        )

        queueDefaultAudioSettings(
            player
        )

        player.session.queue(
            ChatFilterSettings(
                0,
                0,
            )
        )

        player.session.queue(
            HideNpcOps(
                false
            )
        )

        player.session.queue(
            HideLocOps(
                false
            )
        )

        player.session.queue(
            HideObjOps(
                false
            )
        )

        player.session.queue(
            ResetAnims
        )

        player.session.queue(
            MinimapToggle(
                0
            )
        )

        println(
            "[Login] Initial client state queued for '${player.username}' " +
                "topLevel=$topLevelInterface " +
                "resizable=${player.resizable}"
        )
    }

    /**
     * Initializes all current OSRS audio channels at maximum volume.
     *
     * Current controls:
     *
     * - master volume;
     * - music volume;
     * - jingle volume;
     * - sound-effect volume;
     * - area-sound volume.
     *
     * Zero means muted for these current client audio values.
     */
    private fun queueDefaultAudioSettings(
        player: Player,
    ) {
        /*
         * Master volume uses the sound-volume magnitude scale.
         */
        player.session.queue(
            VarpSmall(
                id =
                    MASTER_VOLUME_VARP,

                value =
                    FULL_EFFECT_VOLUME,
            )
        )

        /*
         * Music uses the larger 0..255 range.
         */
        player.session.queue(
            VarpLarge(
                id =
                    MUSIC_VOLUME_VARP,

                value =
                    FULL_MUSIC_VOLUME,
            )
        )

        player.session.queue(
            VarpSmall(
                id =
                    JINGLE_VOLUME_VARP,

                value =
                    FULL_EFFECT_VOLUME,
            )
        )

        player.session.queue(
            VarpSmall(
                id =
                    SOUND_EFFECT_VOLUME_VARP,

                value =
                    FULL_EFFECT_VOLUME,
            )
        )

        player.session.queue(
            VarpSmall(
                id =
                    AREA_SOUND_VOLUME_VARP,

                value =
                    FULL_EFFECT_VOLUME,
            )
        )

        println(
            "[Audio] Set Master=$FULL_EFFECT_VOLUME, " +
                "Music=$FULL_MUSIC_VOLUME, " +
                "Jingles=$FULL_EFFECT_VOLUME, " +
                "Sound Effects=$FULL_EFFECT_VOLUME, " +
                "Area Sounds=$FULL_EFFECT_VOLUME " +
                "for '${player.username}'."
        )
    }

    private companion object {
        const val ROOT_WORLD: Int =
            0

        const val FIXED_TOP_LEVEL_INTERFACE: Int =
            548

        const val RESIZABLE_TOP_LEVEL_INTERFACE: Int =
            161

        /**
         * Current OSRS audio-option varps.
         */
        const val JINGLE_VOLUME_VARP: Int =
            167

        const val MUSIC_VOLUME_VARP: Int =
            168

        const val SOUND_EFFECT_VOLUME_VARP: Int =
            169

        const val AREA_SOUND_VOLUME_VARP: Int =
            872

        /**
         * Current master-volume varp.
         *
         * RuneLite gameval:
         *
         * OPTION_MASTER_VOLUME = 3796
         */
        const val MASTER_VOLUME_VARP: Int =
            3796

        const val FULL_MUSIC_VOLUME: Int =
            255

        const val FULL_EFFECT_VOLUME: Int =
            127
    }
}