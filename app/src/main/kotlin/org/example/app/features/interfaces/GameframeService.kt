package org.example.app.features.interfaces

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import org.example.app.core.player.Player

// Initializes the persistent revision-240 resizable gameframe interfaces.
internal class GameframeService {

    fun mountInitialLayout(player: Player) {
        if (!player.resizable) return

        val state = player.gameframeState

        // Unlock normal chatbox input.
        player.vars.setVarbit(
            id = CHATBOX_UNLOCKED_VARBIT,
            value = 1,
        )

        // 161:33 -> minimap/orbs (160)
        if (!state.minimapMounted) {
            mount(
                player = player,
                slot = GameframeLayout.Slot.MINIMAP_ORBS,
                interfaceId = GameframeLayout.Interface.MINIMAP,
            )

            state.minimapMounted = true
            logMounted(player, "minimap/orbs", GameframeLayout.Slot.MINIMAP_ORBS, GameframeLayout.Interface.MINIMAP)
        }

        // 161:76 -> combat (593)
        if (!state.combatMounted) {
            mount(
                player = player,
                slot = GameframeLayout.Slot.COMBAT,
                interfaceId = GameframeLayout.Interface.COMBAT,
            )

            state.combatMounted = true
            logMounted(player, "combat", GameframeLayout.Slot.COMBAT, GameframeLayout.Interface.COMBAT)
        }

        // 161:77 -> skills (320)
        if (!state.skillsMounted) {
            mount(
                player = player,
                slot = GameframeLayout.Slot.SKILLS,
                interfaceId = GameframeLayout.Interface.SKILLS,
            )

            state.skillsMounted = true
            logMounted(player, "skills", GameframeLayout.Slot.SKILLS, GameframeLayout.Interface.SKILLS)
        }

        // 161:78 -> quests (399)
        if (!state.questsMounted) {
            mount(
                player = player,
                slot = GameframeLayout.Slot.QUESTS,
                interfaceId = GameframeLayout.Interface.QUESTS,
            )

            state.questsMounted = true
            logMounted(player, "quests", GameframeLayout.Slot.QUESTS, GameframeLayout.Interface.QUESTS)
        }

        // 161:79 -> inventory (149)
        if (!state.inventoryMounted) {
            mount(
                player = player,
                slot = GameframeLayout.Slot.INVENTORY,
                interfaceId = GameframeLayout.Interface.INVENTORY,
            )

            state.inventoryMounted = true
            logMounted(player, "inventory", GameframeLayout.Slot.INVENTORY, GameframeLayout.Interface.INVENTORY)
        }

        // 161:96 -> chatbox (162)
        if (!state.chatboxMounted) {
            mount(
                player = player,
                slot = GameframeLayout.Slot.CHATBOX,
                interfaceId = GameframeLayout.Interface.CHATBOX,
            )

            state.chatboxMounted = true
            logMounted(player, "chatbox", GameframeLayout.Slot.CHATBOX, GameframeLayout.Interface.CHATBOX)
        }
    }

    private fun mount(
        player: Player,
        slot: Int,
        interfaceId: Int,
    ) {
        player.session.queue(
            IfOpenSub(
                destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                destinationComponentId = slot,
                interfaceId = interfaceId,
                type = OVERLAY_TYPE,
            )
        )
    }

    private fun logMounted(
        player: Player,
        name: String,
        slot: Int,
        interfaceId: Int,
    ) {
        println(
            "[Interfaces] Mounted $name $interfaceId at " +
                "${GameframeLayout.TopLevel.RESIZABLE}:$slot " +
                "for '${player.username}'."
        )
    }

    private companion object {
        const val OVERLAY_TYPE: Int = 1
        const val CHATBOX_UNLOCKED_VARBIT: Int = 8119
    }
}