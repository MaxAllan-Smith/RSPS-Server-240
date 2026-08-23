package org.example.app.features.interfaces

import org.example.app.core.player.Player

internal class GameframeState(
    var chatboxMounted: Boolean = false,
)

internal val Player.gameframeState: GameframeState
    get() =
        featureState.getOrPut(
            GameframeState::class,
            ::GameframeState,
        )