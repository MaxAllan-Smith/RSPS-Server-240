package org.example.app.features

import org.example.app.core.feature.Feature
import org.example.app.features.chat.ChatFeature
import org.example.app.features.interfaces.InterfaceFeature
import org.example.app.features.login.LoginFeature
import org.example.app.features.world.WorldBootstrapFeature

/**
 * The only project-level feature composition list.
 *
 * Adding/removing a feature changes this file, not core networking, the game
 * engine, or server bootstrap internals.
 */
object FeatureCatalog {
    val all: List<Feature> =
        listOf(
            LoginFeature(),
            WorldBootstrapFeature(),
            InterfaceFeature(),
            ChatFeature()
        )
}
