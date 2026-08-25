package org.example.app.core.persistence

import org.example.app.core.equipment.EquipmentSlot
import org.example.app.core.skills.Skill
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/** Owns the SQLite connection and schema migrations shared by every persistence/definition repository. */
class SqliteDatabase(
    val file: Path,
) {

    private val jdbcUrl =
        "jdbc:sqlite:${file.toAbsolutePath()}"

    init {
        Files.createDirectories(
            file.toAbsolutePath().parent
        )

        initializeSchema()
    }

    fun connection(): Connection {
        val connection =
            DriverManager.getConnection(
                jdbcUrl,
            )

        connection
            .createStatement()
            .use { statement ->
                statement.execute(
                    "PRAGMA foreign_keys = ON"
                )

                statement.execute(
                    "PRAGMA busy_timeout = 5000"
                )
            }

        return connection
    }

    private fun initializeSchema() {
        connection().use { connection ->
            connection
                .createStatement()
                .use { statement ->
                    statement.execute(
                        "PRAGMA journal_mode = WAL"
                    )

                    createPlayerSchema(
                        statement = statement,
                    )

                    createItemSchema(
                        statement = statement,
                    )
                }

            seedVerifiedGameplayItems(
                connection = connection,
            )
        }

        println(
            "[Database] SQLite ready: " +
                file.toAbsolutePath()
        )
    }

    private fun createPlayerSchema(
        statement: java.sql.Statement,
    ) {
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS players (
                username_key TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                position_x INTEGER NOT NULL,
                position_z INTEGER NOT NULL,
                position_level INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS player_skills (
                username_key TEXT NOT NULL,
                skill_id INTEGER NOT NULL,
                experience INTEGER NOT NULL,
                PRIMARY KEY (
                    username_key,
                    skill_id
                ),
                FOREIGN KEY (
                    username_key
                )
                REFERENCES players (
                    username_key
                )
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS player_inventory (
                username_key TEXT NOT NULL,
                slot INTEGER NOT NULL,
                item_id INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                PRIMARY KEY (
                    username_key,
                    slot
                ),
                FOREIGN KEY (
                    username_key
                )
                REFERENCES players (
                    username_key
                )
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS player_equipment (
                username_key TEXT NOT NULL,
                slot INTEGER NOT NULL,
                item_id INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                PRIMARY KEY (
                    username_key,
                    slot
                ),
                FOREIGN KEY (
                    username_key
                )
                REFERENCES players (
                    username_key
                )
                ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun createItemSchema(
        statement: java.sql.Statement,
    ) {
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS item_definitions (
                id INTEGER PRIMARY KEY,

                name TEXT,
                examine TEXT,
                members INTEGER,
                low_alch INTEGER,
                high_alch INTEGER,
                ge_limit INTEGER,
                value INTEGER,
                icon TEXT,

                equipment_slot INTEGER,
                weapon_category INTEGER,

                wiki_updated_at INTEGER,
                gameplay_updated_at INTEGER
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS item_requirements (
                item_id INTEGER NOT NULL,
                skill_id INTEGER NOT NULL,
                level INTEGER NOT NULL,

                PRIMARY KEY (
                    item_id,
                    skill_id
                ),

                FOREIGN KEY (
                    item_id
                )
                REFERENCES item_definitions (
                    id
                )
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS item_prices_latest (
                item_id INTEGER PRIMARY KEY,

                high_price INTEGER,
                high_time INTEGER,

                low_price INTEGER,
                low_time INTEGER,

                updated_at INTEGER NOT NULL,

                FOREIGN KEY (
                    item_id
                )
                REFERENCES item_definitions (
                    id
                )
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS item_price_averages (
                item_id INTEGER NOT NULL,
                interval TEXT NOT NULL,
                timestamp INTEGER NOT NULL,

                avg_high_price INTEGER,
                high_volume INTEGER,

                avg_low_price INTEGER,
                low_volume INTEGER,

                PRIMARY KEY (
                    item_id,
                    interval
                ),

                FOREIGN KEY (
                    item_id
                )
                REFERENCES item_definitions (
                    id
                )
                ON DELETE CASCADE
            )
            """.trimIndent()
        )

        statement.execute(
            """
            CREATE INDEX IF NOT EXISTS
                idx_item_definitions_name
            ON item_definitions (
                name
            )
            """.trimIndent()
        )
    }

    private fun seedVerifiedGameplayItems(
        connection: Connection,
    ) {
        connection.autoCommit = false

        try {
            seedGameplayItem(
                connection = connection,
                itemId = 1351,
                equipmentSlot =
                    EquipmentSlot.WEAPON.id,
                weaponCategory = 1,
            )

            seedRequirement(
                connection = connection,
                itemId = 1351,
                skillId = Skill.ATTACK.id,
                level = 1,
            )

            seedGameplayItem(
                connection = connection,
                itemId = 4151,
                equipmentSlot =
                    EquipmentSlot.WEAPON.id,
                weaponCategory = 20,
            )

            seedRequirement(
                connection = connection,
                itemId = 4151,
                skillId = Skill.ATTACK.id,
                level = 70,
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

    private fun seedGameplayItem(
        connection: Connection,
        itemId: Int,
        equipmentSlot: Int,
        weaponCategory: Int,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO item_definitions (
                id,
                equipment_slot,
                weapon_category,
                gameplay_updated_at
            )
            VALUES (?, ?, ?, ?)

            ON CONFLICT(id)
            DO UPDATE SET
                equipment_slot =
                    excluded.equipment_slot,
                weapon_category =
                    excluded.weapon_category,
                gameplay_updated_at =
                    excluded.gameplay_updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setInt(
                1,
                itemId,
            )

            statement.setInt(
                2,
                equipmentSlot,
            )

            statement.setInt(
                3,
                weaponCategory,
            )

            statement.setLong(
                4,
                System.currentTimeMillis(),
            )

            statement.executeUpdate()
        }
    }

    private fun seedRequirement(
        connection: Connection,
        itemId: Int,
        skillId: Int,
        level: Int,
    ) {
        connection.prepareStatement(
            """
            INSERT INTO item_requirements (
                item_id,
                skill_id,
                level
            )
            VALUES (?, ?, ?)

            ON CONFLICT(
                item_id,
                skill_id
            )
            DO UPDATE SET
                level = excluded.level
            """.trimIndent()
        ).use { statement ->
            statement.setInt(
                1,
                itemId,
            )

            statement.setInt(
                2,
                skillId,
            )

            statement.setInt(
                3,
                level,
            )

            statement.executeUpdate()
        }
    }
}