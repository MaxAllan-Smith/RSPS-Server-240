package org.example.app.features.combat.ui

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatStyle
import org.example.app.features.combat.state.combatState

/** Handles attack-style selection clicks on the combat options interface. */
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
                    style = CombatStyle.STYLE_0,
                )

            STYLE_1_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = CombatStyle.STYLE_1,
                )

            STYLE_2_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = CombatStyle.STYLE_2,
                )

            STYLE_3_COMPONENT ->
                setCombatStyle(
                    player = player,
                    style = CombatStyle.STYLE_3,
                )

            AUTO_RETALIATE_COMPONENT ->
                toggleAutoRetaliate(player)
        }
    }

    private fun setCombatStyle(
        player: Player,
        style: CombatStyle,
    ) {
        player.combatState.style =
            style

        player.vars.setVarp(
            id = COMBAT_STYLE_VARP,
            value = style.id,
        )

        println(
            "[Combat] '${player.username}' selected " +
                "combat style ${style.id}."
        )
    }

    private fun toggleAutoRetaliate(
        player: Player,
    ) {
        val enabled =
            !player.combatState.autoRetaliate

        player.combatState.autoRetaliate =
            enabled

        player.vars.setVarp(
            id = AUTO_RETALIATE_VARP,
            value =
                if (enabled) {
                    0
                } else {
                    1
                },
        )

        println(
            "[Combat] '${player.username}' set auto retaliate " +
                "to ${if (enabled) "on" else "off"}."
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
