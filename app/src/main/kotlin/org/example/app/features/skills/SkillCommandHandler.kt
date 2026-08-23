package org.example.app.features.skills

import net.rsprot.protocol.game.incoming.misc.user.ClientCheat
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill

internal class SkillCommandHandler(
    private val skillService: SkillService,
) {

    fun handle(
        player: Player,
        packet: ClientCheat,
    ) {
        val parts =
            packet.command
                .trim()
                .split(" ")
                .filter(String::isNotBlank)

        if (parts.firstOrNull()?.lowercase() != COMMAND) {
            return
        }

        if (parts.size != 3) {
            println("[Skills] Usage: ::xp <skill> <amount>")
            return
        }

        val skill =
            Skill.entries.firstOrNull {
                it.name.equals(
                    other = parts[1],
                    ignoreCase = true,
                )
            }

        if (skill == null) {
            println("[Skills] Unknown skill '${parts[1]}'.")
            return
        }

        val amount =
            parts[2].toIntOrNull()

        if (amount == null || amount < 0) {
            println("[Skills] Invalid XP amount '${parts[2]}'.")
            return
        }

        val change =
            skillService.addExperience(
                player = player,
                skill = skill,
                amount = amount,
            )

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
    }

    private companion object {
        const val COMMAND: String = "xp"
    }
}