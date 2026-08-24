package org.example.app.core.persistence

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.inventory.PlayerInventory
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.normalizedUsername
import org.example.app.core.skills.Skill
import java.sql.Connection
import java.sql.ResultSet

class PlayerPersistenceRepository(
    private val database: SqliteDatabase,
) {

    fun load(
        username: String,
    ): PlayerSaveData? {
        val usernameKey =
            username.normalizedUsername()

        database.connection().use { connection ->
            val position =
                loadPosition(
                    connection = connection,
                    usernameKey = usernameKey,
                )
                    ?: return null

            return PlayerSaveData(
                position = position,
                skillExperience =
                    loadSkills(
                        connection = connection,
                        usernameKey = usernameKey,
                    ),
                inventory =
                    loadInventory(
                        connection = connection,
                        usernameKey = usernameKey,
                    ),
                equipment =
                    loadEquipment(
                        connection = connection,
                        usernameKey = usernameKey,
                    ),
            )
        }
    }

    fun save(
        player: Player,
    ) {
        val usernameKey =
            player.username.normalizedUsername()

        database.connection().use { connection ->
            connection.autoCommit = false

            try {
                savePlayer(
                    connection = connection,
                    player = player,
                    usernameKey = usernameKey,
                )

                replaceSkills(
                    connection = connection,
                    player = player,
                    usernameKey = usernameKey,
                )

                replaceInventory(
                    connection = connection,
                    player = player,
                    usernameKey = usernameKey,
                )

                replaceEquipment(
                    connection = connection,
                    player = player,
                    usernameKey = usernameKey,
                )

                connection.commit()
            } catch (t: Throwable) {
                runCatching {
                    connection.rollback()
                }

                throw t
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun loadPosition(
        connection: Connection,
        usernameKey: String,
    ): WorldPosition? {
        connection.prepareStatement(
            """
            SELECT
                position_x,
                position_z,
                position_level
            FROM players
            WHERE username_key = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                usernameKey,
            )

            statement.executeQuery().use { result ->
                if (!result.next()) {
                    return null
                }

                return WorldPosition(
                    x =
                        result.getInt(
                            "position_x"
                        ),
                    z =
                        result.getInt(
                            "position_z"
                        ),
                    level =
                        result.getInt(
                            "position_level"
                        ),
                )
            }
        }
    }

    private fun loadSkills(
        connection: Connection,
        usernameKey: String,
    ): Map<Skill, Int> {
        val skills =
            LinkedHashMap<Skill, Int>()

        connection.prepareStatement(
            """
            SELECT
                skill_id,
                experience
            FROM player_skills
            WHERE username_key = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                usernameKey,
            )

            statement.executeQuery().use { result ->
                while (result.next()) {
                    val skill =
                        skillById(
                            result.getInt(
                                "skill_id"
                            )
                        )
                            ?: continue

                    skills[skill] =
                        result.getInt(
                            "experience"
                        )
                }
            }
        }

        return skills
    }

    private fun loadInventory(
        connection: Connection,
        usernameKey: String,
    ): Map<Int, ItemStack> {
        val inventory =
            LinkedHashMap<Int, ItemStack>()

        connection.prepareStatement(
            """
            SELECT
                slot,
                item_id,
                amount
            FROM player_inventory
            WHERE username_key = ?
            ORDER BY slot
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                usernameKey,
            )

            statement.executeQuery().use { result ->
                while (result.next()) {
                    val slot =
                        result.getInt(
                            "slot"
                        )

                    if (
                        slot !in
                            0 until PlayerInventory.CAPACITY
                    ) {
                        continue
                    }

                    inventory[slot] =
                        result.itemStack()
                }
            }
        }

        return inventory
    }

    private fun loadEquipment(
        connection: Connection,
        usernameKey: String,
    ): Map<EquipmentSlot, ItemStack> {
        val equipment =
            LinkedHashMap<
                EquipmentSlot,
                ItemStack
            >()

        connection.prepareStatement(
            """
            SELECT
                slot,
                item_id,
                amount
            FROM player_equipment
            WHERE username_key = ?
            ORDER BY slot
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                usernameKey,
            )

            statement.executeQuery().use { result ->
                while (result.next()) {
                    val slot =
                        equipmentSlotById(
                            result.getInt(
                                "slot"
                            )
                        )
                            ?: continue

                    equipment[slot] =
                        result.itemStack()
                }
            }
        }

        return equipment
    }

    private fun savePlayer(
        connection: Connection,
        player: Player,
        usernameKey: String,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO players (
                username_key,
                display_name,
                position_x,
                position_z,
                position_level,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(username_key)
            DO UPDATE SET
                display_name = excluded.display_name,
                position_x = excluded.position_x,
                position_z = excluded.position_z,
                position_level = excluded.position_level,
                updated_at = excluded.updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                usernameKey,
            )

            statement.setString(
                2,
                player.username,
            )

            statement.setInt(
                3,
                player.position.x,
            )

            statement.setInt(
                4,
                player.position.z,
            )

            statement.setInt(
                5,
                player.position.level,
            )

            statement.setLong(
                6,
                System.currentTimeMillis(),
            )

            statement.executeUpdate()
        }
    }

    private fun replaceSkills(
        connection: Connection,
        player: Player,
        usernameKey: String,
    ) {
        deleteChildren(
            connection = connection,
            table = "player_skills",
            usernameKey = usernameKey,
        )

        connection.prepareStatement(
            """
            INSERT INTO player_skills (
                username_key,
                skill_id,
                experience
            )
            VALUES (?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            for (skill in Skill.entries) {
                statement.setString(
                    1,
                    usernameKey,
                )

                statement.setInt(
                    2,
                    skill.id,
                )

                statement.setInt(
                    3,
                    player.skills.experience(
                        skill
                    ),
                )

                statement.addBatch()
            }

            statement.executeBatch()
        }
    }

    private fun replaceInventory(
        connection: Connection,
        player: Player,
        usernameKey: String,
    ) {
        deleteChildren(
            connection = connection,
            table = "player_inventory",
            usernameKey = usernameKey,
        )

        connection.prepareStatement(
            """
            INSERT INTO player_inventory (
                username_key,
                slot,
                item_id,
                amount
            )
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            for (
                slot in
                0 until PlayerInventory.CAPACITY
            ) {
                val item =
                    player.inventory[slot]
                        ?: continue

                statement.setString(
                    1,
                    usernameKey,
                )

                statement.setInt(
                    2,
                    slot,
                )

                statement.setInt(
                    3,
                    item.id,
                )

                statement.setInt(
                    4,
                    item.amount,
                )

                statement.addBatch()
            }

            statement.executeBatch()
        }
    }

    private fun replaceEquipment(
        connection: Connection,
        player: Player,
        usernameKey: String,
    ) {
        deleteChildren(
            connection = connection,
            table = "player_equipment",
            usernameKey = usernameKey,
        )

        connection.prepareStatement(
            """
            INSERT INTO player_equipment (
                username_key,
                slot,
                item_id,
                amount
            )
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            for (slot in EquipmentSlot.entries) {
                val item =
                    player.equipment[slot]
                        ?: continue

                statement.setString(
                    1,
                    usernameKey,
                )

                statement.setInt(
                    2,
                    slot.id,
                )

                statement.setInt(
                    3,
                    item.id,
                )

                statement.setInt(
                    4,
                    item.amount,
                )

                statement.addBatch()
            }

            statement.executeBatch()
        }
    }

    private fun deleteChildren(
        connection: Connection,
        table: String,
        usernameKey: String,
    ) {
        connection.prepareStatement(
            "DELETE FROM $table WHERE username_key = ?"
        ).use { statement ->
            statement.setString(
                1,
                usernameKey,
            )

            statement.executeUpdate()
        }
    }

    private fun skillById(
        id: Int,
    ): Skill? =
        Skill.entries.firstOrNull {
            it.id == id
        }

    private fun equipmentSlotById(
        id: Int,
    ): EquipmentSlot? =
        EquipmentSlot.entries.firstOrNull {
            it.id == id
        }

    private fun ResultSet.itemStack(): ItemStack =
        ItemStack(
            id =
                getInt(
                    "item_id"
                ),
            amount =
                getInt(
                    "amount"
                ),
        )
}