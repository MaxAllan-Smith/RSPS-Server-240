package org.example.app.features.combat.state

import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatStyle
import org.example.app.features.combat.model.CombatWeaponCategory

/** Per-player combat UI state: selected style and current weapon category. */
internal class CombatState {
    var initialized: Boolean = false

    var weaponCategory: CombatWeaponCategory =
        CombatWeaponCategory.UNARMED

    var style: CombatStyle =
        CombatStyle.STYLE_0

    var autoRetaliate: Boolean = true
}

internal val Player.combatState: CombatState
    get() =
        featureState.getOrPut(
            CombatState::class,
            ::CombatState,
        )
