package org.example.app.core.player

import net.rsprot.protocol.api.Session
import net.rsprot.protocol.game.outgoing.info.Infos
import org.example.app.core.feature.FeatureStateStore
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stable player/session shell shared by all features.
 *
 * Feature-specific state belongs in [featureState], not as fields here.
 */
class Player(
    val username: String,
    val index: Int,
    val session: Session<Player>,
    val infos: Infos,
    var position: WorldPosition,
    val resizable: Boolean,
) {
    private val disconnected = AtomicBoolean(false)

    val featureState = FeatureStateStore()

    fun markDisconnected(): Boolean {
        return disconnected.compareAndSet(false, true)
    }

    val isDisconnected: Boolean
        get() = disconnected.get()

    override fun toString(): String {
        return "Player(" +
            "username=$username, " +
            "index=$index, " +
            "position=$position, " +
            "resizable=$resizable" +
            ")"
    }
}
