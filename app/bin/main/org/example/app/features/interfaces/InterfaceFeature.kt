package org.example.app.features.interfaces

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class InterfaceFeature : Feature {

    override val id: String =
        "interfaces"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        // Gameframe/interface bootstrap will be registered here.
    }
}