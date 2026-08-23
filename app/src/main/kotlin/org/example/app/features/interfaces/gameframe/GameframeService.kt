package org.example.app.features.interfaces.gameframe

import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import org.example.app.core.player.Player
import org.example.app.features.interfaces.journal.JournalTab
import org.example.app.features.interfaces.social.SocialView

internal class GameframeService {

    fun mountInitialLayout(player: Player) {
        if (!player.resizable) return

        val state = player.gameframeState

        player.vars.setVarbit(
            id = CHATBOX_UNLOCKED_VARBIT,
            value = 1,
        )

        player.vars.setVarbit(
            id = JOURNAL_TAB_VARBIT,
            value = JOURNAL_QUEST_TAB,
        )

        mountMinimap(player, state)
        mountGameframeTabs(player, state)
        mountJournal(player, state)
        mountChatbox(player, state)
    }

    fun selectJournalTab(
        player: Player,
        tab: JournalTab,
    ) {
        player.vars.setVarbit(
            id = JOURNAL_TAB_VARBIT,
            value = tab.varbitValue,
        )

        mount(
            player = player,
            parentInterface = GameframeLayout.Interface.JOURNAL,
            slot = GameframeLayout.JournalSlot.CONTENT,
            interfaceId = tab.interfaceId,
        )

        println(
            "[Interfaces] Selected journal tab " +
                "${tab.name.lowercase()} (${tab.interfaceId}) " +
                "for '${player.username}'."
        )
    }

    fun selectSocialView(
        player: Player,
        view: SocialView,
    ) {
        mount(
            player = player,
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.FRIENDS_LIST,
            interfaceId = view.interfaceId,
        )

        println(
            "[Interfaces] Selected social view " +
                "${view.name.lowercase()} (${view.interfaceId}) " +
                "for '${player.username}'."
        )
    }

    private fun mountMinimap(
        player: Player,
        state: GameframeState,
    ) {
        if (state.minimapMounted) return

        mount(
            player = player,
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.MINIMAP_ORBS,
            interfaceId = GameframeLayout.Interface.MINIMAP,
        )

        state.minimapMounted = true

        logMounted(
            player = player,
            name = "minimap/orbs",
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.MINIMAP_ORBS,
            interfaceId = GameframeLayout.Interface.MINIMAP,
        )
    }

    private fun mountGameframeTabs(
        player: Player,
        state: GameframeState,
    ) {
        for (tab in GameframeTab.entries) {
            if (tab in state.mountedTabs) continue

            mount(
                player = player,
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = tab.slot,
                interfaceId = tab.interfaceId,
            )

            state.mountedTabs += tab

            logMounted(
                player = player,
                name = tab.name.lowercase(),
                parentInterface = GameframeLayout.TopLevel.RESIZABLE,
                slot = tab.slot,
                interfaceId = tab.interfaceId,
            )
        }
    }

    private fun mountJournal(
        player: Player,
        state: GameframeState,
    ) {
        if (state.journalMounted) return

        mount(
            player = player,
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.JOURNAL,
            interfaceId = GameframeLayout.Interface.JOURNAL,
        )

        mount(
            player = player,
            parentInterface = GameframeLayout.Interface.JOURNAL,
            slot = GameframeLayout.JournalSlot.CONTENT,
            interfaceId = GameframeLayout.Interface.QUEST_LIST,
        )

        state.journalMounted = true

        logMounted(
            player = player,
            name = "journal",
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.JOURNAL,
            interfaceId = GameframeLayout.Interface.JOURNAL,
        )

        logMounted(
            player = player,
            name = "quest list",
            parentInterface = GameframeLayout.Interface.JOURNAL,
            slot = GameframeLayout.JournalSlot.CONTENT,
            interfaceId = GameframeLayout.Interface.QUEST_LIST,
        )
    }

    private fun mountChatbox(
        player: Player,
        state: GameframeState,
    ) {
        if (state.chatboxMounted) return

        mount(
            player = player,
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.CHATBOX,
            interfaceId = GameframeLayout.Interface.CHATBOX,
        )

        state.chatboxMounted = true

        logMounted(
            player = player,
            name = "chatbox",
            parentInterface = GameframeLayout.TopLevel.RESIZABLE,
            slot = GameframeLayout.Slot.CHATBOX,
            interfaceId = GameframeLayout.Interface.CHATBOX,
        )
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

        const val JOURNAL_TAB_VARBIT: Int = 8168
        const val JOURNAL_QUEST_TAB: Int = 1
    }
}