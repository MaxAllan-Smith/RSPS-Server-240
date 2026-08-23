package org.example.app.features.world

/**
 * State owned exclusively by the initial-world vertical slice.
 */
internal data class WorldBootstrapState(
    var rebuildPending: Boolean = true,
    var clientStatePending: Boolean = true,
    var mapBuildComplete: Boolean = false,
)
