package org.example.app.features.inventory.state

import org.example.app.core.player.Player

internal class InventoryState {
    var synchronizedRevision: Int = -1

    var interfaceInitialized: Boolean = false
}

internal val Player.inventoryState: InventoryState
    get() =
        featureState.getOrPut(
            InventoryState::class,
            ::InventoryState,
        )