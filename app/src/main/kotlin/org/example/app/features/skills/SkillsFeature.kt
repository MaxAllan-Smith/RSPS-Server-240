package org.example.app.features.skills

import net.rsprot.protocol.game.incoming.misc.user.ClientCheat
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class SkillsFeature(
    private val skillService: SkillService =
        SkillService(),
) : Feature {

    private val skillCommandHandler =
        SkillCommandHandler(
            skillService = skillService,
        )

    private val skillLevelUpHandler =
        SkillLevelUpHandler()

    override val id: String =
        "skills"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<ClientCheat> { packet ->
                skillCommandHandler.handle(
                    player = this,
                    packet = packet,
                )
            }

            addListener<ResumePauseButton> { packet ->
                skillLevelUpHandler.handle(
                    player = this,
                    packet = packet,
                )
            }
        }

        registrar.beforeInfoUpdate(
            priority = SKILLS_PRIORITY,
        ) { _, player ->
            skillService.syncInitial(player)
        }
    }

    private companion object {
        const val SKILLS_PRIORITY: Int = 90
    }
}