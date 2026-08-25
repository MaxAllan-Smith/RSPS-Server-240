package org.example.app.core.vars

import io.netty.buffer.ByteBuf
import org.openrs2.cache.Cache
import java.nio.file.Path

/**
 * Loads OSRS varbit definitions directly from the active cache.
 *
 * Varbits live in archive 2, group 14. The file id is the varbit id.
 */
internal object VarbitDefinitionLoader {

    private const val CONFIG_ARCHIVE: Int = 2

    private const val VARBIT_GROUP: Int = 14

    fun load(
        cacheDirectory: Path,
        id: Int,
    ): VarbitDefinition {
        require(id >= 0) {
            "Varbit id must be non-negative."
        }

        val cache =
            Cache.open(cacheDirectory)

        cache.use {
            val data =
                cache.read(
                    CONFIG_ARCHIVE,
                    VARBIT_GROUP,
                    id,
                )

            try {
                return decode(
                    id = id,
                    buffer = data,
                )
            } finally {
                data.release()
            }
        }
    }

    private fun decode(
        id: Int,
        buffer: ByteBuf,
    ): VarbitDefinition {
        var definition:
            VarbitDefinition? = null

        while (buffer.isReadable) {
            when (
                val opcode =
                    buffer.readUnsignedByte()
                        .toInt()
            ) {
                0 -> {
                    return definition
                        ?: error(
                            "Varbit $id contained no definition."
                        )
                }

                1 -> {
                    definition =
                        VarbitDefinition(
                            baseVarp =
                                buffer.readUnsignedShort(),
                            startBit =
                                buffer.readUnsignedByte()
                                    .toInt(),
                            endBit =
                                buffer.readUnsignedByte()
                                    .toInt(),
                        )
                }

                else -> {
                    error(
                        "Unsupported opcode $opcode " +
                            "while decoding varbit $id."
                    )
                }
            }
        }

        return definition
            ?: error(
                "Unexpected end of varbit $id."
            )
    }
}