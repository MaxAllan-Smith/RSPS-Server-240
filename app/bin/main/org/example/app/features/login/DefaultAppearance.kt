package org.example.app.features.login

import net.rsprot.protocol.game.outgoing.info.playerinfo.PlayerInfo

/**
 * New-account appearance policy for the login slice.
 */
internal object DefaultAppearance {
    private val identKits =
        intArrayOf(
            0,
            10,
            18,
            26,
            33,
            36,
            42,
        )

    fun apply(
        playerInfo: PlayerInfo,
        username: String,
    ) {
        val appearance = playerInfo.avatar.extendedInfo

        appearance.setName(username)
        appearance.setCombatLevel(3)
        appearance.setSkillLevel(0)
        appearance.setHidden(false)
        appearance.setBodyType(0)
        appearance.setPronoun(0)
        appearance.setSkullIcon(-1)
        appearance.setOverheadIcon(-1)

        repeat(5) { index ->
            appearance.setColour(index, 0)
        }

        identKits.forEachIndexed { index, identKit ->
            appearance.setIdentKit(index, identKit)
        }

        appearance.setBaseAnimationSet(
            808,
            823,
            819,
            820,
            821,
            822,
            824,
        )
    }
}
