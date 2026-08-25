package org.example.app.core.items

import org.example.app.core.equipment.EquipmentDefinition
import org.example.app.core.equipment.EquipmentSkillRequirement
import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.equipment.WeaponDefinition
import org.example.app.core.persistence.SqliteDatabase
import org.example.app.core.skills.Skill
import java.sql.Connection

/** Loads item definitions (equipment slot, skill requirements, weapon category) from the SQLite database. */
class SqliteItemDefinitionSource(
    private val database: SqliteDatabase,
) : ItemDefinitionSource {

    override fun load(): Iterable<ItemDefinition> {
        database.connection().use { connection ->
            val requirements =
                loadRequirements(
                    connection = connection,
                )

            return loadItems(
                connection = connection,
                requirements = requirements,
            )
        }
    }

    private fun loadItems(
        connection: Connection,
        requirements:
            Map<Int, List<EquipmentSkillRequirement>>,
    ): List<ItemDefinition> {
        val definitions =
            ArrayList<ItemDefinition>()

        connection.prepareStatement(
            """
            SELECT
                id,
                equipment_slot,
                weapon_category
            FROM item_definitions
            ORDER BY id
            """.trimIndent()
        ).use { statement ->
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val itemId =
                        result.getInt(
                            "id"
                        )

                    val slotId =
                        result.nullableInt(
                            "equipment_slot"
                        )

                    val categoryId =
                        result.nullableInt(
                            "weapon_category"
                        )

                    val equipment =
                        slotId?.let { id ->
                            EquipmentDefinition(
                                slot =
                                    equipmentSlot(
                                        id
                                    ),
                                skillRequirements =
                                    requirements[itemId]
                                        .orEmpty(),
                            )
                        }

                    val weapon =
                        categoryId?.let { id ->
                            WeaponDefinition(
                                categoryId = id,
                            )
                        }

                    definitions +=
                        ItemDefinition(
                            id = itemId,
                            equipment = equipment,
                            weapon = weapon,
                        )
                }
            }
        }

        println(
            "[Items] Loaded ${definitions.size} " +
                "item definitions from SQLite."
        )

        return definitions
    }

    private fun loadRequirements(
        connection: Connection,
    ): Map<Int, List<EquipmentSkillRequirement>> {
        val requirements =
            LinkedHashMap<
                Int,
                MutableList<EquipmentSkillRequirement>
            >()

        connection.prepareStatement(
            """
            SELECT
                item_id,
                skill_id,
                level
            FROM item_requirements
            ORDER BY
                item_id,
                skill_id
            """.trimIndent()
        ).use { statement ->
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val itemId =
                        result.getInt(
                            "item_id"
                        )

                    val skill =
                        skill(
                            result.getInt(
                                "skill_id"
                            )
                        )

                    requirements
                        .getOrPut(itemId) {
                            ArrayList()
                        }
                        .add(
                            EquipmentSkillRequirement(
                                skill = skill,
                                level =
                                    result.getInt(
                                        "level"
                                    ),
                            )
                        )
                }
            }
        }

        return requirements
    }

    private fun equipmentSlot(
        id: Int,
    ): EquipmentSlot =
        EquipmentSlot.entries
            .firstOrNull {
                it.id == id
            }
            ?: error(
                "Unknown equipment slot id $id."
            )

    private fun skill(
        id: Int,
    ): Skill =
        Skill.entries
            .firstOrNull {
                it.id == id
            }
            ?: error(
                "Unknown skill id $id."
            )

    private fun java.sql.ResultSet.nullableInt(
        column: String,
    ): Int? {
        val value =
            getInt(column)

        return if (wasNull()) {
            null
        } else {
            value
        }
    }
}