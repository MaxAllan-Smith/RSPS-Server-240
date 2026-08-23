package org.example.app.player

import net.rsprot.protocol.api.Session
import net.rsprot.protocol.game.outgoing.info.Infos
import java.util.concurrent.atomic.AtomicBoolean

class Player(
    val username: String,
    val index: Int,
    val session: Session<Player>,
    val infos: Infos,
    var position: WorldPosition,
    val resizable: Boolean,
) {

    private val disconnected =
        AtomicBoolean(false)

    /*
     * A newly allocated PlayerInfo has not yet had its initial GPI
     * state established.
     *
     * GameEngine changes this to false immediately after constructing
     * and queueing RebuildLoginV2.
     */
    var needsLoginRebuild: Boolean =
        true

    /*
     * The official client also needs its gameframe and initial state
     * packets after the rebuild.
     */
    var needsInitialClientState: Boolean =
        true

    /*
     * Set when the client sends MAP_BUILD_COMPLETE.
     *
     * When this becomes true, we know the client's scene/map has
     * actually completed loading.
     */
    var mapBuildComplete: Boolean =
        false

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
        return buildString {
            append("Player(")

            append("username=")
            append(username)

            append(", index=")
            append(index)

            append(", position=")
            append(position)

            append(", resizable=")
            append(resizable)

            append(')')
        }
    }
}