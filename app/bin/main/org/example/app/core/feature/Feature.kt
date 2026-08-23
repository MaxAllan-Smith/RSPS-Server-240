package org.example.app.core.feature

/**
 * A vertical game feature.
 *
 * A feature registers itself only through [FeatureRegistrar]. Core code never
 * imports concrete feature classes. This is the extension boundary that keeps
 * networking and the game loop stable as gameplay grows.
 */
interface Feature {
    val id: String

    fun install(registrar: FeatureRegistrar)
}
