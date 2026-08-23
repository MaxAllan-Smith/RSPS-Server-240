package org.example.app.features.skills

import net.rsprot.protocol.game.outgoing.misc.player.UpdateStatV2
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill
import org.example.app.core.skills.SkillChange

internal class SkillService {

    fun syncInitial(player: Player) {
        val state = player.skillSyncState
        if (state.initialSyncSent) return

        for (skill in Skill.entries) {
            sync(
                player = player,
                skill = skill,
            )
        }

        state.initialSyncSent = true

        println(
            "[Skills] Synchronized ${Skill.entries.size} skills " +
                "for '${player.username}'."
        )
    }

    fun addExperience(
        player: Player,
        skill: Skill,
        amount: Int,
    ): SkillChange {
        val change =
            player.skills.addExperience(
                skill = skill,
                amount = amount,
            )

        sync(
            player = player,
            skill = skill,
        )

        return change
    }

    private fun sync(
        player: Player,
        skill: Skill,
    ) {
        player.session.queue(
            UpdateStatV2(
                skill.id,
                player.skills.currentLevel(skill),
                player.skills.baseLevel(skill),
                player.skills.experience(skill),
            )
        )
    }
}