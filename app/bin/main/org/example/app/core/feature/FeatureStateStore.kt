package org.example.app.core.feature

import kotlin.reflect.KClass

/**
 * Per-player storage for feature-owned state.
 *
 * Core Player deliberately does not know about combat state, login bootstrap
 * state, trading state, inventory state, etc. Each vertical slice owns its own
 * state type and stores it here.
 *
 * Access is expected from the single game/communication thread. Incoming
 * RSProt packet consumers are invoked when that thread calls
 * Session.processIncomingPackets(player).
 */
class FeatureStateStore {
    private val values = HashMap<KClass<*>, Any>()

    fun <T : Any> get(type: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values[type] as T?
    }

    fun <T : Any> getOrPut(
        type: KClass<T>,
        factory: () -> T,
    ): T {
        get(type)?.let { return it }

        val value = factory()
        values[type] = value
        return value
    }

    fun <T : Any> remove(type: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values.remove(type) as T?
    }
}
