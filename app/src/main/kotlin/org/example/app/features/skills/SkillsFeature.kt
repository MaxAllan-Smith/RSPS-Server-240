package org.example.app.features.skills

import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import org.example.app.core.engine.GameContext
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.features.skills.commands.SkillCommandHandler
import org.example.app.features.skills.levelup.SkillLevelUpHandler
import org.example.app.features.skills.levelup.SkillLevelUpService
import org.example.app.features.skills.unlocks.SkillUnlockLoader
import org.example.app.features.skills.unlocks.SkillUnlockService

/**
 * Generic skill synchronization and interface feature.
 *
 * Gameplay features are free to mutate Player.skills directly. This feature
 * remains responsible for transmitting those changes to the client.
 */
internal class SkillsFeature : Feature {

    private val skillUnlockService =
        SkillUnlockService()

    private val skillLevelUpService =
        SkillLevelUpService(
            unlocks =
                skillUnlockService,
        )

    private val skillService =
        SkillService(
            levelUpService =
                skillLevelUpService,
        )

    private val skillCommandHandler =
        SkillCommandHandler(
            skillService =
                skillService,
        )

    private val skillLevelUpHandler =
        SkillLevelUpHandler()

    override val id: String =
        "skills"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.command(
            skillCommandHandler::handle
        )

        registrar.packets {
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
            initializeUnlocks(
                context
            )

            skillService.syncInitial(
                player
            )

            /*
             * Gameplay XP awarded earlier in this game cycle is transmitted
             * here, before the player's info update is queued.
             */
            skillService
                .processPendingChanges(
                    player
                )
        }
    }

    private fun initializeUnlocks(
        context: GameContext,
    ) {
        if (
            skillUnlockService
                .isInitialized
        ) {
            return
        }

        val repository =
            SkillUnlockLoader.load(
                context.cacheDirectory
            )

        skillUnlockService.initialize(
            repository
        )

        println(
            "[Skills] Loaded ${repository.unlockLevelCount} " +
                "unlock levels across ${repository.skillCount} skills."
        )
    }

    private companion object {
        const val SKILLS_PRIORITY: Int =
            90
    }
}