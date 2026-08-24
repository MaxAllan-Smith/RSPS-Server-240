package org.example.app.features.skills.guide

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.outgoing.interfaces.IfCloseSub
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill
import org.example.app.features.skills.levelup.skillLevelUpState

internal class SkillGuideHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        when (packet.interfaceId) {
            STATS_INTERFACE ->
                handleSkillClick(
                    player = player,
                    packet = packet,
                )

            SKILL_GUIDE_INTERFACE ->
                handleGuideClick(
                    player = player,
                    packet = packet,
                )
        }
    }

    private fun handleSkillClick(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.op != OPEN_GUIDE_OP) {
            return
        }

        val skill =
            Skill.fromStatsComponent(packet.componentId)
                ?: return

        val unlocks =
            player.skillLevelUpState.consume(skill)

        if (unlocks != null) {
            player.vars.setVarbit(
                id = skill.levelUpFlashVarbitId,
                value = 0,
            )

            open(
                player = player,
                skill = skill,
                section = LEVEL_UP_UNLOCKS_SECTION,
                firstLevel = unlocks.firstLevel,
                lastLevel = unlocks.lastLevel,
            )

            println(
                "[Skills] Opened ${skill.name.lowercase()} " +
                    "level-up unlocks ${unlocks.firstLevel}.." +
                    "${unlocks.lastLevel} for '${player.username}'."
            )

            return
        }

        open(
            player = player,
            skill = skill,
            section = OVERVIEW_SECTION,
        )

        println(
            "[Skills] Opened ${skill.name.lowercase()} " +
                "skill guide for '${player.username}'."
        )
    }

    private fun handleGuideClick(
        player: Player,
        packet: If3Button,
    ) {
        if (
            packet.componentId != CLOSE_COMPONENT ||
            packet.op != CLOSE_OP
        ) {
            return
        }

        close(player)
    }

    private fun open(
        player: Player,
        skill: Skill,
        section: Int,
        firstLevel: Int = 0,
        lastLevel: Int = 0,
    ) {
        player.session.queue(
            IfOpenSub(
                destinationInterfaceId = TOP_LEVEL_INTERFACE,
                destinationComponentId = FLOATER_COMPONENT,
                interfaceId = SKILL_GUIDE_INTERFACE,
                type = OVERLAY_TYPE,
            )
        )

        player.session.queue(
            IfSetEventsV2(
                interfaceId = SKILL_GUIDE_INTERFACE,
                componentId = TABS_COMPONENT,
                start = 0,
                end = MAX_TAB_CHILD,
                events1 = 0,
                events2 = OP1_EVENT,
            )
        )

        player.session.queue(
            RunClientScript(
                id = SKILL_GUIDE_INIT_SCRIPT,
                values =
                    listOf(
                        skill.skillGuideId,
                        section,
                        firstLevel,
                        lastLevel,
                    ),
            )
        )
    }

    private fun close(player: Player) {
        player.session.queue(
            IfCloseSub(
                interfaceId = TOP_LEVEL_INTERFACE,
                componentId = FLOATER_COMPONENT,
            )
        )

        println(
            "[Skills] Closed skill guide " +
                "for '${player.username}'."
        )
    }

    private companion object {
        const val STATS_INTERFACE: Int = 320
        const val OPEN_GUIDE_OP: Int = 2

        const val TOP_LEVEL_INTERFACE: Int = 161
        const val FLOATER_COMPONENT: Int = 18
        const val OVERLAY_TYPE: Int = 1

        const val SKILL_GUIDE_INTERFACE: Int = 860
        const val CLOSE_COMPONENT: Int = 4
        const val CLOSE_OP: Int = 1

        const val TABS_COMPONENT: Int = 7
        const val MAX_TAB_CHILD: Int = 200

        const val SKILL_GUIDE_INIT_SCRIPT: Int = 1902

        const val OVERVIEW_SECTION: Int = 0
        const val LEVEL_UP_UNLOCKS_SECTION: Int = 31

        const val OP1_EVENT: Int = 1
    }
}