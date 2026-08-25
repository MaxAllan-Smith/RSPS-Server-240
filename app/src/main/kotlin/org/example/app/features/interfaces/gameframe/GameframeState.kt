package org.example.app.features.interfaces.gameframe

import org.example.app.core.player.Player

/** Per-player record of which game-frame chrome has already been mounted. */
internal class GameframeState(
    var xpDropsMounted: Boolean = false,
    var minimapMounted: Boolean = false,
    var journalMounted: Boolean = false,
    var chatboxMounted: Boolean = false,
    val mountedTabs: MutableSet<GameframeTab> = mutableSetOf(),
)

internal val Player.gameframeState: GameframeState
    get() =
        featureState.getOrPut(
            GameframeState::class,
            ::GameframeState,
        )