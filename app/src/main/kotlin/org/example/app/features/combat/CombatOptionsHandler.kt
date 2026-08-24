package org.example.app.features.combat

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player

internal class CombatOptionsHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (
            packet.interfaceId != COMBAT_INTERFACE ||
            packet.op != BUTTON_OP
        ) {
            return
        }

        when (packet.componentId) {
            STYLE_0_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = 0,
                )

            STYLE_1_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = 1,
                )

            STYLE_2_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = 2,
                )

            STYLE_3_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = 3,
                )

            AUTO_RETALIATE_COMPONENT ->
                toggleAutoRetaliate(player)
        }
    }

    private fun setCombatStyle(
        player: Player,
        style: Int,
    ) {
        player.vars.setVarp(
            id = COMBAT_STYLE_VARP,
            value = style,
        )

        println(
            "[Combat] '${player.username}' selected " +
                "combat style $style."
        )
    }

    private fun toggleAutoRetaliate(
        player: Player,
    ) {
        val disabled =
            player.vars.getVarp(
                AUTO_RETALIATE_VARP
            )

        val updated =
            if (disabled == 0) {
                1
            } else {
                0
            }

        player.vars.setVarp(
            id = AUTO_RETALIATE_VARP,
            value = updated,
        )

        println(
            "[Combat] '${player.username}' set auto retaliate " +
                "to ${if (updated == 0) "on" else "off"}."
        )
    }

    private companion object {
        const val COMBAT_INTERFACE: Int = 593
        const val BUTTON_OP: Int = 1

        const val STYLE_0_COMPONENT: Int = 6
        const val STYLE_1_COMPONENT: Int = 10
        const val STYLE_2_COMPONENT: Int = 14
        const val STYLE_3_COMPONENT: Int = 18

        const val AUTO_RETALIATE_COMPONENT: Int = 32

        const val COMBAT_STYLE_VARP: Int = 43
        const val AUTO_RETALIATE_VARP: Int = 172
    }
}