package org.example.app.features.interfaces

import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

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
            }
        }

        registrar.beforeInfoUpdate(
            priority = INTERFACE_PRIORITY,
        ) { _, player ->
            gameframeService.mountInitialLayout(player)
        }
    }

    private companion object {
        const val INTERFACE_PRIORITY: Int = 100
    }
}