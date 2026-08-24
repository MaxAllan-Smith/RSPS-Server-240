package org.example.app.features.skills

import net.rsprot.protocol.game.incoming.misc.user.ClientCheat
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import org.example.app.core.engine.GameContext
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.features.skills.unlocks.SkillUnlockLoader
import org.example.app.features.skills.unlocks.SkillUnlockRepository

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

    private var unlockRepository:
        SkillUnlockRepository? = null

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
        ) { context, player ->
            loadUnlockRepository(context)

            skillService.syncInitial(player)
        }
    }

    private fun loadUnlockRepository(
        context: GameContext,
    ): SkillUnlockRepository {
        val existing =
            unlockRepository

        if (existing != null) {
            return existing
        }

        val loaded =
            SkillUnlockLoader.load(
                context.cacheDirectory
            )

        unlockRepository = loaded

        println(
            "[Skills] Loaded ${loaded.unlockLevelCount} " +
                "unlock levels across ${loaded.skillCount} skills."
        )

        return loaded
    }

    private companion object {
        const val SKILLS_PRIORITY: Int = 90
    }
}