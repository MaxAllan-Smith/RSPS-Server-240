package org.example.app.features.skills

import net.rsprot.protocol.game.outgoing.misc.player.UpdateStatV2
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill

internal class SkillService {

    fun syncInitial(player: Player) {
        val state = player.skillSyncState
        if (state.initialSyncSent) return

        for (skill in Skill.entries) {
            player.session.queue(
                UpdateStatV2(
                    skill.id,
                    player.skills.currentLevel(skill),
                    player.skills.baseLevel(skill),
                    player.skills.experience(skill),
                )
            )
        }

        state.initialSyncSent = true

        println(
            "[Skills] Synchronized ${Skill.entries.size} skills " +
                "for '${player.username}'."
        )
    }
}