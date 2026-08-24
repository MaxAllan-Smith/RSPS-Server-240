package org.example.app.features.skills.commands

import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.skills.SkillService

internal class SkillCommandHandler(
    private val skillService: SkillService,
) {

    fun handle(
        player: Player,
        command: String,
        arguments: List<String>,
    ): Boolean {
        if (command != COMMAND) {
            return false
        }

        if (arguments.size != 2) {
            player.sendGameMessage(
                "Usage: ::xp <skill> <amount>"
            )
            return true
        }

        val skill =
            Skill.entries.firstOrNull {
                it.name.equals(
                    other = arguments[0],
                    ignoreCase = true,
                )
            }

        if (skill == null) {
            player.sendGameMessage(
                "Unknown skill '${arguments[0]}'."
            )
            return true
        }

        val amount =
            arguments[1].toIntOrNull()

        if (amount == null || amount < 0) {
            player.sendGameMessage(
                "Invalid XP amount '${arguments[1]}'."
            )
            return true
        }

        val change =
            skillService.addExperience(
                player = player,
                skill = skill,
                amount = amount,
            )

        player.sendGameMessage(
            "Added ${change.gainedExperience} " +
                "${skill.displayName} XP."
        )

        if (change.levelledUp) {
            player.sendGameMessage(
                "Congratulations, you've advanced your " +
                    "${skill.displayName} level. " +
                    "You are now level ${change.level}."
            )
        }

        println(
            "[Skills] Added ${change.gainedExperience} " +
                "${skill.name.lowercase()} XP to '${player.username}'."
        )

        if (change.levelledUp) {
            println(
                "[Skills] '${player.username}' levelled " +
                    "${skill.name.lowercase()} " +
                    "${change.previousLevel} -> ${change.level}."
            )
        }

        return true
    }

    private val Skill.displayName: String
        get() =
            name
                .lowercase()
                .replaceFirstChar(Char::uppercase)

    private companion object {
        const val COMMAND: String = "xp"
    }
}