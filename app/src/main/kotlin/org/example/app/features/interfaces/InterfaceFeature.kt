package org.example.app.features.interfaces

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.features.interfaces.gameframe.GameframeService
import org.example.app.features.interfaces.journal.JournalTabHandler
import org.example.app.features.interfaces.logout.LogoutHandler
import org.example.app.features.interfaces.social.SocialTabHandler
import org.example.app.features.interfaces.worldswitcher.WorldSwitcherHandler

/**
 * Generic game-frame chrome: the journal, social, logout and world-switcher
 * tabs, plus the initial interface layout mount.
 *
 * Gameplay-specific interfaces (combat style/equipment, the skill guide,
 * ...) register their own [FeatureRegistrar.onInterfaceButton] handlers from
 * their own owning feature rather than being wired in here.
 */
internal class InterfaceFeature(
    private val gameframeService:
        GameframeService =
        GameframeService(),
) : Feature {

    private val journalTabHandler =
        JournalTabHandler(
            gameframeService =
                gameframeService,
        )

    private val socialTabHandler =
        SocialTabHandler(
            gameframeService =
                gameframeService,
        )

    private val logoutHandler =
        LogoutHandler()

    private val worldSwitcherHandler =
        WorldSwitcherHandler(
            gameframeService =
                gameframeService,
        )

    override val id: String =
        "interfaces"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.onInterfaceButton(
            priority = INTERFACE_PRIORITY,
        ) { player, packet ->
            journalTabHandler.handle(
                player = player,
                packet = packet,
            )

            socialTabHandler.handle(
                player = player,
                packet = packet,
            )

            logoutHandler.handle(
                player = player,
                packet = packet,
            )

            worldSwitcherHandler.handle(
                player = player,
                packet = packet,
            )
        }

        registrar.beforeInfoUpdate(
            priority = INTERFACE_PRIORITY,
        ) { _, player ->
            gameframeService
                .mountInitialLayout(
                    player
                )
        }
    }

    private companion object {
        const val INTERFACE_PRIORITY: Int =
            100
    }
}
