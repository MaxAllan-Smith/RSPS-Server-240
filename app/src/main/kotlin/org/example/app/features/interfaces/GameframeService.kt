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
            player.session.queue(
                IfOpenSub(
                    destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                    destinationComponentId = GameframeLayout.Slot.MINIMAP_ORBS,
                    interfaceId = GameframeLayout.Interface.MINIMAP,
                    type = OVERLAY_TYPE,
                )
            )

            state.minimapMounted = true

            println(
                "[Interfaces] Mounted minimap/orbs " +
                    "${GameframeLayout.Interface.MINIMAP} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.MINIMAP_ORBS} for '${player.username}'."
            )
        }

        if (!state.combatMounted) {
            player.session.queue(
                IfOpenSub(
                    destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                    destinationComponentId = GameframeLayout.Slot.COMBAT,
                    interfaceId = GameframeLayout.Interface.COMBAT,
                    type = OVERLAY_TYPE
                )
            )
            
            state.combatMounted = true
            
            println(
                "[Interfaces] Mounted combat " +
                    "${GameframeLayout.Interface.COMBAT} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.COMBAT} for '${player.username}'."
            )
        }

        // 161:77 -> skills (320)
        if (!state.skillsMounted) {
            player.session.queue(
                IfOpenSub(
                    destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                    destinationComponentId = GameframeLayout.Slot.SKILLS,
                    interfaceId = GameframeLayout.Interface.SKILLS,
                    type = OVERLAY_TYPE,
                )
            )

            state.skillsMounted = true

            println(
                "[Interfaces] Mounted skills " +
                    "${GameframeLayout.Interface.SKILLS} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.SKILLS} for '${player.username}'."
            )
        }

        // 161:79 -> inventory (149)
        if (!state.inventoryMounted) {
            player.session.queue(
                IfOpenSub(
                    destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                    destinationComponentId = GameframeLayout.Slot.INVENTORY,
                    interfaceId = GameframeLayout.Interface.INVENTORY,
                    type = OVERLAY_TYPE,
                )
            )

            state.inventoryMounted = true

            println(
                "[Interfaces] Mounted inventory " +
                    "${GameframeLayout.Interface.INVENTORY} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.INVENTORY} for '${player.username}'."
            )
        }

        // 161:96 -> chatbox (162)
        if (!state.chatboxMounted) {
            player.session.queue(
                IfOpenSub(
                    destinationInterfaceId = GameframeLayout.TopLevel.RESIZABLE,
                    destinationComponentId = GameframeLayout.Slot.CHATBOX,
                    interfaceId = GameframeLayout.Interface.CHATBOX,
                    type = OVERLAY_TYPE,
                )
            )

            state.chatboxMounted = true

            println(
                "[Interfaces] Mounted chatbox " +
                    "${GameframeLayout.Interface.CHATBOX} at " +
                    "${GameframeLayout.TopLevel.RESIZABLE}:" +
                    "${GameframeLayout.Slot.CHATBOX} for '${player.username}'."
            )
        }
    }

    private companion object {
        const val OVERLAY_TYPE: Int = 1
        const val CHATBOX_UNLOCKED_VARBIT: Int = 8119
    }
}