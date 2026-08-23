package org.example.app.features.skills

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetHide
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill
import org.example.app.core.skills.SkillChange

internal class SkillLevelUpService {

    fun show(
        player: Player,
        change: SkillChange,
    ) {
        if (!change.levelledUp) return

        player.session.queue(
            IfOpenSub(
                destinationInterfaceId = CHATBOX_INTERFACE,
                destinationComponentId = CHAT_MODAL_COMPONENT,
                interfaceId = LEVEL_UP_INTERFACE,
                type = MODAL_TYPE,
            )
        )

        for (skill in Skill.entries) {
            player.session.queue(
                IfSetHide(
                    interfaceId = LEVEL_UP_INTERFACE,
                    componentId = skill.levelUpComponentId,
                    hidden = skill != change.skill,
                )
            )
        }

        player.session.queue(
            IfSetText(
                interfaceId = LEVEL_UP_INTERFACE,
                componentId = TITLE_COMPONENT,
                text = title(change),
            )
        )

        player.session.queue(
            IfSetText(
                interfaceId = LEVEL_UP_INTERFACE,
                componentId = LEVEL_COMPONENT,
                text =
                    "Your ${displayName(change.skill)} level " +
                        "is now ${change.level}.",
            )
        )

        player.session.queue(
            IfSetText(
                interfaceId = LEVEL_UP_INTERFACE,
                componentId = CONTINUE_COMPONENT,
                text = "Click here to continue",
            )
        )
    }

    private fun title(change: SkillChange): String {
        val name =
            displayName(change.skill)

        val levels =
            change.gainedLevels

        if (levels == 1) {
            val article =
                if (name.first().lowercaseChar() in VOWELS) {
                    "an"
                } else {
                    "a"
                }

            return "<col=000080>Congratulations, you just advanced " +
                "$article $name level."
        }

        return "<col=000080>Congratulations, you just advanced " +
            "$levels $name levels."
    }

    private fun displayName(skill: Skill): String =
        skill.name
            .lowercase()
            .replaceFirstChar(Char::uppercase)

    private companion object {
        const val CHATBOX_INTERFACE: Int = 162
        const val CHAT_MODAL_COMPONENT: Int = 567

        const val LEVEL_UP_INTERFACE: Int = 233
        const val TITLE_COMPONENT: Int = 1
        const val LEVEL_COMPONENT: Int = 2
        const val CONTINUE_COMPONENT: Int = 3

        const val MODAL_TYPE: Int = 0

        val VOWELS: Set<Char> =
            setOf('a', 'e', 'i', 'o', 'u')
    }
}