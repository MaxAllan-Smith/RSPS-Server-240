package org.example.app.core.player

import net.rsprot.protocol.api.Session
import net.rsprot.protocol.game.outgoing.info.Infos
import org.example.app.core.feature.FeatureStateStore
import org.example.app.core.vars.PlayerVars
import org.example.app.core.vars.VarbitDefinitionRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stable player/session shell shared by all features.
 *
 * Feature-specific state belongs in [featureState], not as fields here.
 *
 * Generic RuneScape player capabilities, such as vars/varbits, belong directly
 * on the player because they are shared infrastructure used by many features.
 */
class Player(
    val username: String,
    val index: Int,
    val session: Session<Player>,
    val infos: Infos,
    var position: WorldPosition,
    val resizable: Boolean,
    varbitDefinitions: VarbitDefinitionRepository,
) {
    private val disconnected =
        AtomicBoolean(false)

    val featureState =
        FeatureStateStore()

    val vars =
        PlayerVars(
            player = this,
            definitions = varbitDefinitions,
        )

    fun markDisconnected(): Boolean {
        return disconnected.compareAndSet(
            false,
            true,
        )
    }

    val isDisconnected: Boolean
        get() =
            disconnected.get()

    override fun toString(): String {
        return "Player(" +
            "username=$username, " +
            "index=$index, " +
            "position=$position, " +
            "resizable=$resizable" +
            ")"
    }
}