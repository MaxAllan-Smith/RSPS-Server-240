package org.example.app.features.combat.weapon

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.player.Player
import org.example.app.features.combat.model.CombatWeaponCategory
import org.example.app.features.combat.state.combatState
import org.example.app.features.combat.ui.CombatInterfaceService

internal class CombatEquipmentService(
    private val weaponRepository: CombatWeaponRepository,
    private val interfaceService: CombatInterfaceService,
) {

    fun synchronize(
        player: Player,
    ) {
        val equippedWeapon =
            player.equipment[
                EquipmentSlot.WEAPON
            ]

        val category =
            equippedWeapon
                ?.let { weapon ->
                    weaponRepository
                        .find(weapon.id)
                        ?.category
                }
                ?: CombatWeaponCategory.UNARMED

        if (
            player.combatState.weaponCategory ==
                category
        ) {
            return
        }

        interfaceService.setWeaponCategory(
            player = player,
            category = category,
        )

        if (equippedWeapon == null) {
            println(
                "[Combat] '${player.username}' detected no " +
                    "equipped weapon; using unarmed."
            )
        } else {
            val definition =
                weaponRepository.find(
                    equippedWeapon.id,
                )

            if (definition == null) {
                println(
                    "[Combat] '${player.username}' equipped " +
                        "unsupported weapon item=${equippedWeapon.id}; " +
                        "using unarmed."
                )
            } else {
                println(
                    "[Combat] '${player.username}' equipped " +
                        "weapon item=${equippedWeapon.id}, " +
                        "category=${definition.category.id}."
                )
            }
        }
    }
}