package org.example.app.core.vars

import net.rsprot.protocol.game.outgoing.varp.VarpLarge
import net.rsprot.protocol.game.outgoing.varp.VarpSmall
import org.example.app.core.player.Player

/**
 * Per-player RuneScape variable state.
 *
 * Varbits are logical bit ranges packed inside backing varps.
 * Only varps are transmitted to the client.
 */
class PlayerVars(
    private val player: Player,
    private val definitions: VarbitDefinitionRepository,
) {

    private val varps =
        HashMap<Int, Int>()

    fun getVarp(
        id: Int,
    ): Int {
        require(id >= 0) {
            "Varp id must be non-negative."
        }

        return varps[id] ?: 0
    }

    fun setVarp(
        id: Int,
        value: Int,
    ) {
        require(id >= 0) {
            "Varp id must be non-negative."
        }

        val current =
            getVarp(id)

        if (current == value) {
            return
        }

        varps[id] =
            value

        syncVarp(
            id = id,
            value = value,
        )
    }

    fun getVarbit(
        id: Int,
    ): Int {
        val definition =
            definitions.get(id)

        val backingValue =
            getVarp(
                definition.baseVarp
            )

        return (
            backingValue ushr
                definition.startBit
            ) and definition.valueMask
    }

    fun setVarbit(
        id: Int,
        value: Int,
    ) {
        val definition =
            definitions.get(id)

        validateVarbitValue(
            id = id,
            value = value,
            definition = definition,
        )

        val current =
            getVarp(
                definition.baseVarp
            )

        val shiftedMask =
            definition.valueMask shl
                definition.startBit

        val cleared =
            current and
                shiftedMask.inv()

        val shiftedValue =
            (value and definition.valueMask) shl
                definition.startBit

        val updated =
            cleared or shiftedValue

        setVarp(
            id = definition.baseVarp,
            value = updated,
        )
    }

    private fun validateVarbitValue(
        id: Int,
        value: Int,
        definition: VarbitDefinition,
    ) {
        /*
         * A 32-bit definition occupies the entire Int, so every Int value is
         * representable. Normal OSRS varbits are smaller than this.
         */
        if (
            definition.bitCount ==
            Int.SIZE_BITS
        ) {
            return
        }

        require(
            value in 0..definition.valueMask
        ) {
            "Value $value does not fit in " +
                "varbit $id (${definition.bitCount} bits)."
        }
    }

    private fun syncVarp(
        id: Int,
        value: Int,
    ) {
        if (
            value in Byte.MIN_VALUE..Byte.MAX_VALUE
        ) {
            player.session.queue(
                VarpSmall(
                    id = id,
                    value = value,
                )
            )
        } else {
            player.session.queue(
                VarpLarge(
                    id = id,
                    value = value,
                )
            )
        }
    }
}