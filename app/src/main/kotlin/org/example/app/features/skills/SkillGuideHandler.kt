package org.example.app.features.skills

import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill

internal class SkillGuideHandler {

    fun handle(
        player: Player,
        packet: If3Button,
    ) {
        if (packet.interfaceId != STATS_INTERFACE) {
            return
        }

        if (packet.op != OPEN_GUIDE_OP) {
            return
        }

        val skill =
            Skill.fromStatsComponent(packet.componentId)
                ?: return

        open(
            player = player,
            skill = skill,
        )
    }

    private fun open(
        player: Player,
        skill: Skill,
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
                        DEFAULT_SECTION,
                        0,
                        0,
                    ),
            )
        )

        println(
            "[Skills] Opened ${skill.name.lowercase()} " +
                "skill guide for '${player.username}'."
        )
    }

    private companion object {
        const val STATS_INTERFACE: Int = 320
        const val OPEN_GUIDE_OP: Int = 2

        const val TOP_LEVEL_INTERFACE: Int = 161
        const val FLOATER_COMPONENT: Int = 18
        const val OVERLAY_TYPE: Int = 1

        const val SKILL_GUIDE_INTERFACE: Int = 860
        const val TABS_COMPONENT: Int = 7
        const val MAX_TAB_CHILD: Int = 200

        const val SKILL_GUIDE_INIT_SCRIPT: Int = 1902
        const val DEFAULT_SECTION: Int = 0

        const val OP1_EVENT: Int = 1
    }
}