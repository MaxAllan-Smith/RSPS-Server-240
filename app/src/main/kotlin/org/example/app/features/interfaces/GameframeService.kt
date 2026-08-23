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

        // Select the Quest List journal sub-tab.
        player.vars.setVarbit(
            id = JOURNAL_TAB_VARBIT,
            value = JOURNAL_QUEST_TAB,
        )

        // 161:33 -> minimap/orbs (160)
        if (!state.minimapMounted) {
            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = GameframeLayout.Slot.MINIMAP_ORBS,
                interfaceId = GameframeLayout.Interface.MINIMAP,
            )

            state.minimapMounted = true
            logMounted(player, "minimap/orbs", 161, 33, 160)
        }

        // 161:76 -> combat (593)
        if (!state.combatMounted) {
            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = GameframeLayout.Slot.COMBAT,
                interfaceId = GameframeLayout.Interface.COMBAT,
            )

            state.combatMounted = true
            logMounted(player, "combat", 161, 76, 593)
        }

        // 161:77 -> skills (320)
        if (!state.skillsMounted) {
            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = GameframeLayout.Slot.SKILLS,
                interfaceId = GameframeLayout.Interface.SKILLS,
            )

            state.skillsMounted = true
            logMounted(player, "skills", 161, 77, 320)
        }

        if (!state.journalMounted) {
            // 161:78 -> side journal shell (629)
            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = GameframeLayout.Slot.JOURNAL,
                interfaceId = GameframeLayout.Interface.JOURNAL,
            )

            // 629:43 -> Quest List (399)
            mount(
                player = player,
                parentInterface = GameframeLayout.Interface.JOURNAL,
                slot = GameframeLayout.JournalSlot.CONTENT,
                interfaceId = GameframeLayout.Interface.QUEST_LIST,
            )

            state.journalMounted = true

            logMounted(player, "journal", 161, 78, 629)
            logMounted(player, "quest list", 629, 43, 399)
        }

        // 161:79 -> inventory (149)
        if (!state.inventoryMounted) {
            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = GameframeLayout.Slot.INVENTORY,
                interfaceId = GameframeLayout.Interface.INVENTORY,
            )

            state.inventoryMounted = true
            logMounted(player, "inventory", 161, 79, 149)
        }

        // 161:96 -> chatbox (162)
        if (!state.chatboxMounted) {
            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = GameframeLayout.Slot.CHATBOX,
                interfaceId = GameframeLayout.Interface.CHATBOX,
            )

            state.chatboxMounted = true
            logMounted(player, "chatbox", 161, 96, 162)
        }
    }

    private fun mount(
        player: Player,
        parentInterface: Int,
        slot: Int,
        interfaceId: Int,
    ) {
        player.session.queue(
            IfOpenSub(
                destinationInterfaceId = parentInterface,
                destinationComponentId = slot,
                interfaceId = interfaceId,
                type = OVERLAY_TYPE,
            )
        )
    }

    private fun logMounted(
        player: Player,
        name: String,
        parentInterface: Int,
        slot: Int,
        interfaceId: Int,
    ) {
        println(
            "[Interfaces] Mounted $name $interfaceId at " +
                "$parentInterface:$slot for '${player.username}'."
        )
    }

    private companion object {
        const val OVERLAY_TYPE: Int = 1

        const val CHATBOX_UNLOCKED_VARBIT: Int = 8119

        // side_journal selected tab.
        const val JOURNAL_TAB_VARBIT: Int = 8168
        const val JOURNAL_QUEST_TAB: Int = 1
    }
}