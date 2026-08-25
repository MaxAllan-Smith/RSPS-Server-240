package org.example.app.features.itemuse

import org.example.app.core.player.Player

/**
 * One validated inventory item-on-item interaction.
 *
 * The item ids and slots contained here have already been verified against the
 * player's authoritative inventory before a registered handler receives them.
 */
internal data class ItemOnItemInteraction(
    val player: Player,
    val selectedSlot: Int,
    val selectedItemId: Int,
    val targetSlot: Int,
    val targetItemId: Int,
)