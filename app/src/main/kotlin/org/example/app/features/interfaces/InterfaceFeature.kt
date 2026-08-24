package org.example.app.features.interfaces

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.features.combat.ui.CombatEquipmentHandler
import org.example.app.features.combat.ui.CombatInventoryHandler
import org.example.app.features.combat.ui.CombatOptionsHandler
import org.example.app.features.interfaces.gameframe.GameframeService
import org.example.app.features.interfaces.journal.JournalTabHandler
import org.example.app.features.interfaces.logout.LogoutHandler
import org.example.app.features.interfaces.social.SocialTabHandler
import org.example.app.features.interfaces.worldswitcher.WorldSwitcherHandler
import org.example.app.features.skills.guide.SkillGuideHandler

internal class InterfaceFeature(
    private val gameframeService: GameframeService =
        GameframeService(),
) : Feature {

    private val journalTabHandler =
        JournalTabHandler(
            gameframeService = gameframeService,
        )

    private val socialTabHandler =
        SocialTabHandler(
            gameframeService = gameframeService,
        )

    private val logoutHandler =
        LogoutHandler()

    private val worldSwitcherHandler =
        WorldSwitcherHandler(
            gameframeService = gameframeService,
        )

    private val skillGuideHandler =
        SkillGuideHandler()

    private val combatOptionsHandler =
        CombatOptionsHandler()

    private val combatInventoryHandler =
        CombatInventoryHandler()

    private val combatEquipmentHandler =
        CombatEquipmentHandler()

    override val id: String =
        "interfaces"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.packets {
            addListener<If3Button> { packet ->
                journalTabHandler.handle(
                    player = this,
                    packet = packet,
                )

                socialTabHandler.handle(
                    player = this,
                    packet = packet,
                )

                logoutHandler.handle(
                    player = this,
                    packet = packet,
                )

                worldSwitcherHandler.handle(
                    player = this,
                    packet = packet,
                )

                skillGuideHandler.handle(
                    player = this,
                    packet = packet,
                )

                combatOptionsHandler.handle(
                    player = this,
                    packet = packet,
                )

                combatInventoryHandler.handle(
                    player = this,
                    packet = packet,
                )

                combatEquipmentHandler.handle(
                    player = this,
                    packet = packet,
                )
            }
        }

        registrar.beforeInfoUpdate(
            priority = INTERFACE_PRIORITY,
        ) { _, player ->
            gameframeService.mountInitialLayout(player)
        }
    }

    private companion object {
        const val INTERFACE_PRIORITY: Int =
            100
    }
}