package org.example.app.core.vars

/**
 * Describes a varbit packed inside a backing varp.
 *
 * A varbit does not have its own server packet. Its value occupies a
 * specific range of bits within a 32-bit varp value.
 */
data class VarbitDefinition(
    val baseVarp: Int,
    val startBit: Int,
    val endBit: Int,
) {

    init {
        require(baseVarp >= 0) {
            "baseVarp must be non-negative."
        }

        require(startBit in 0..31) {
            "startBit must be in 0..31."
        }

        require(endBit in startBit..31) {
            "endBit must be in startBit..31."
        }
    }

    val bitCount: Int
        get() =
            endBit - startBit + 1

    val valueMask: Int
        get() =
            if (bitCount == Int.SIZE_BITS) {
                -1
            } else {
                (1 shl bitCount) - 1
            }
}