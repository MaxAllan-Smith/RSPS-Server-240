package org.example.app.features.woodcutting

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.player.Player
import org.example.app.core.skills.Skill

/**
 * Resolves the best Woodcutting axe currently available to a player.
 *
 * Equipped tools are preferred when two otherwise-equivalent axes are
 * available. Inventory tools remain perfectly valid for Woodcutting.
 */
internal class WoodcuttingAxeService {

    fun findBestUsable(
        player: Player,
    ): WoodcuttingAxeSelection? {
        val woodcuttingLevel =
            player.skills.currentLevel(
                Skill.WOODCUTTING
            )

        val candidates =
            buildList {
                equippedAxe(
                    player = player,
                )?.let(::add)

                inventoryAxes(
                    player = player,
                ).forEach(::add)
            }

        return candidates
            .asSequence()
            .filter {
                woodcuttingLevel >=
                    it.axe.woodcuttingLevel
            }
            .maxWithOrNull(
                compareBy<WoodcuttingAxeSelection> {
                    it.axe.priority
                }.thenByDescending {
                    /*
                     * When two candidates have equal priority, prefer
                     * the equipped axe.
                     */
                    it.source ==
                        WoodcuttingAxeSource.EQUIPPED
                }
            )
    }

    fun findBestAvailable(
        player: Player,
    ): WoodcuttingAxeSelection? {
        val candidates =
            buildList {
                equippedAxe(
                    player = player,
                )?.let(::add)

                inventoryAxes(
                    player = player,
                ).forEach(::add)
            }

        return candidates.maxWithOrNull(
            compareBy<WoodcuttingAxeSelection> {
                it.axe.priority
            }.thenByDescending {
                it.source ==
                    WoodcuttingAxeSource.EQUIPPED
            }
        )
    }

    private fun equippedAxe(
        player: Player,
    ): WoodcuttingAxeSelection? {
        val weapon =
            player.equipment[
                EquipmentSlot.WEAPON
            ]
                ?: return null

        val axe =
            WoodcuttingAxe.find(
                itemId = weapon.id,
            )
                ?: return null

        return WoodcuttingAxeSelection(
            axe = axe,
            source =
                WoodcuttingAxeSource.EQUIPPED,
        )
    }

    private fun inventoryAxes(
        player: Player,
    ): List<WoodcuttingAxeSelection> =
        buildList {
            for (
                slot in
                0 until
                    org.example.app.core.inventory
                        .PlayerInventory.CAPACITY
            ) {
                val item =
                    player.inventory[slot]
                        ?: continue

                val axe =
                    WoodcuttingAxe.find(
                        itemId = item.id,
                    )
                        ?: continue

                add(
                    WoodcuttingAxeSelection(
                        axe = axe,
                        source =
                            WoodcuttingAxeSource.INVENTORY,
                    )
                )
            }
        }
}

internal data class WoodcuttingAxeSelection(
    val axe: WoodcuttingAxe,
    val source: WoodcuttingAxeSource,
)

internal enum class WoodcuttingAxeSource(
    val description: String,
) {
    EQUIPPED(
        description = "equipped",
    ),

    INVENTORY(
        description = "inventory",
    ),
}