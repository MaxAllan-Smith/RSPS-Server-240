package org.example.app.features.interfaces

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class InterfaceFeature(
    private val gameframeService: GameframeService =
        GameframeService(),
) : Feature {

    override val id: String =
        "interfaces"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.beforeInfoUpdate(
            priority = INTERFACE_PRIORITY,
        ) { _, player ->
            gameframeService.mountInitialLayout(
                player
            )
        }
    }

    private companion object {
        const val INTERFACE_PRIORITY: Int = 100
    }
}