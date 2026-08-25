package org.example.app.features.skills.unlocks

import io.netty.buffer.ByteBuf
import org.openrs2.cache.Cache
import java.nio.file.Path

/** Loads the per-skill level-up unlock text table from the cache. */
internal object SkillUnlockLoader {

    fun load(cacheDirectory: Path): SkillUnlockRepository {
        val levelsBySkill =
            mutableMapOf<Int, MutableSet<Int>>()

        Cache.open(cacheDirectory).use { cache ->
            for (file in cache.list(CONFIG_ARCHIVE, DB_ROW_GROUP)) {
                val buffer =
                    cache.read(
                        CONFIG_ARCHIVE,
                        DB_ROW_GROUP,
                        file.id,
                    )

                try {
                    val row =
                        decodeRow(buffer)

                    if (row.tableId != SKILL_FEATURES_TABLE) {
                        continue
                    }

                    for ((skillId, level, subsection) in row.skillEntries) {
                        if (subsection == NO_SUBSECTION) {
                            continue
                        }

                        levelsBySkill
                            .getOrPut(skillId, ::mutableSetOf)
                            .add(level)
                    }
                } finally {
                    buffer.release()
                }
            }
        }

        return SkillUnlockRepository(
            levelsBySkill =
                levelsBySkill.mapValues {
                    it.value.toSet()
                },
        )
    }

    private fun decodeRow(
        buffer: ByteBuf,
    ): DbRow {
        var tableId = -1

        val skillEntries =
            mutableListOf<SkillFeatureEntry>()

        while (buffer.isReadable) {
            when (
                val opcode =
                    buffer.readUnsignedByte()
                        .toInt()
            ) {
                0 ->
                    break

                3 ->
                    decodeColumns(
                        buffer = buffer,
                        skillEntries = skillEntries,
                    )

                4 ->
                    tableId =
                        buffer.readVarInt2()

                else ->
                    error(
                        "Unsupported DB row opcode $opcode."
                    )
            }
        }

        return DbRow(
            tableId = tableId,
            skillEntries = skillEntries,
        )
    }

    private fun decodeColumns(
        buffer: ByteBuf,
        skillEntries: MutableList<SkillFeatureEntry>,
    ) {
        buffer.readUnsignedByte()

        while (true) {
            val columnId =
                buffer.readUnsignedByte()
                    .toInt()

            if (columnId == END_COLUMNS) {
                return
            }

            val types =
                IntArray(
                    buffer.readUnsignedByte()
                        .toInt()
                ) {
                    buffer.readUnsignedShortSmart()
                }

            val fieldCount =
                buffer.readUnsignedShortSmart()

            repeat(fieldCount) {
                val values =
                    IntArray(types.size)

                for (index in types.indices) {
                    val type =
                        types[index]

                    if (type == STRING_TYPE) {
                        buffer.skipString()
                        continue
                    }

                    values[index] =
                        buffer.readInt()
                }

                if (
                    columnId == SKILL_COLUMN &&
                    values.size == SKILL_TUPLE_SIZE
                ) {
                    skillEntries +=
                        SkillFeatureEntry(
                            skillId = values[0],
                            level = values[1],
                            subsection = values[2],
                        )
                }
            }
        }
    }

    private fun ByteBuf.readUnsignedShortSmart(): Int {
        val peek =
            getUnsignedByte(readerIndex())
                .toInt()

        return if (peek < 128) {
            readUnsignedByte()
                .toInt()
        } else {
            readUnsignedShort() - 0x8000
        }
    }

    private fun ByteBuf.readVarInt2(): Int {
        var value = 0
        var bits = 0

        while (true) {
            val byte =
                readUnsignedByte()
                    .toInt()

            value =
                value or
                    ((byte and 0x7F) shl bits)

            if (byte <= 127) {
                return value
            }

            bits += 7
        }
    }

    private fun ByteBuf.skipString() {
        while (readUnsignedByte().toInt() != 0) {
            // Strings are not needed for unlock detection.
        }
    }

    private data class DbRow(
        val tableId: Int,
        val skillEntries: List<SkillFeatureEntry>,
    )

    private data class SkillFeatureEntry(
        val skillId: Int,
        val level: Int,
        val subsection: Int,
    )

    private const val CONFIG_ARCHIVE: Int = 2
    private const val DB_ROW_GROUP: Int = 38

    private const val SKILL_FEATURES_TABLE: Int = 213
    private const val SKILL_COLUMN: Int = 3
    private const val SKILL_TUPLE_SIZE: Int = 3

    private const val STRING_TYPE: Int = 36

    private const val NO_SUBSECTION: Int = -1
    private const val END_COLUMNS: Int = 255
}