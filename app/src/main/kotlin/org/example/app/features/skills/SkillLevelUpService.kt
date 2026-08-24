package org.example.app.features.skills

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetHide
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.core.skills.SkillChange
import org.example.app.core.skills.SkillExperience
import org.example.app.features.skills.unlocks.SkillUnlockService

internal class SkillLevelUpService(
    private val unlocks: SkillUnlockService,
) {

    fun show(
        player: Player,
        change: SkillChange,
    ) {
        if (!change.levelledUp) {
            return
        }

        handleUnlocks(
            player = player,
            change = change,
        )

        playFireworks(player)

        playSound(
            player = player,
            level = change.level,
        )

        openLevelUpDisplay(
            player = player,
            change = change,
        )
    }

    private fun handleUnlocks(
        player: Player,
        change: SkillChange,
    ) {
        val firstLevel =
            change.previousLevel + 1

        val lastLevel =
            change.level

        if (
            !unlocks.hasUnlocks(
                skill = change.skill,
                firstLevel = firstLevel,
                lastLevel = lastLevel,
            )
        ) {
            return
        }

        player.skillLevelUpState.record(change)

        player.vars.setVarbit(
            id = change.skill.levelUpFlashVarbitId,
            value = 1,
        )

        player.sendGameMessage(
            "You've unlocked new ${displayName(change.skill)} content. " +
                "Check your Skills tab to see what you've unlocked."
        )
    }

    private fun playFireworks(
        player: Player,
    ) {
        player.infos.playerInfo.avatar.extendedInfo.setSpotAnim(
            slot = LEVEL_UP_GRAPHIC_SLOT,
            id = LEVEL_UP_FIREWORKS_GRAPHIC,
            delay = 0,
            height = 0,
        )
    }

    private fun playSound(
        player: Player,
        level: Int,
    ) {
        player.session.queue(
            SynthSound(
                id = LEVEL_UP_START_SOUND,
                loops = 1,
                delay = 0,
            )
        )

        if (level == SkillExperience.MAX_LEVEL) {
            player.session.queue(
                SynthSound(
                    id = LEVEL_99_FINISH_SOUND,
                    loops = 1,
                    delay = LEVEL_99_FINISH_DELAY,
                )
            )

            return
        }

        player.session.queue(
            SynthSound(
                id = LEVEL_UP_FINISH_SOUND,
                loops = 1,
                delay = LEVEL_UP_FINISH_DELAY,
            )
        )
    }

    private fun openLevelUpDisplay(
        player: Player,
        change: SkillChange,
    ) {
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

    private fun title(
        change: SkillChange,
    ): String {
        val name =
            displayName(change.skill)

        val levels =
            change.gainedLevels

        if (levels == 1) {
            val article =
                if (
                    name.first()
                        .lowercaseChar() in VOWELS
                ) {
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

    private fun displayName(
        skill: Skill,
    ): String =
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

        const val LEVEL_UP_GRAPHIC_SLOT: Int = 0
        const val LEVEL_UP_FIREWORKS_GRAPHIC: Int = 199

        const val LEVEL_UP_START_SOUND: Int = 2396
        const val LEVEL_UP_FINISH_SOUND: Int = 2384
        const val LEVEL_99_FINISH_SOUND: Int = 2379

        const val LEVEL_UP_FINISH_DELAY: Int = 25
        const val LEVEL_99_FINISH_DELAY: Int = 35

        val VOWELS: Set<Char> =
            setOf(
                'a',
                'e',
                'i',
                'o',
                'u',
            )
    }
}