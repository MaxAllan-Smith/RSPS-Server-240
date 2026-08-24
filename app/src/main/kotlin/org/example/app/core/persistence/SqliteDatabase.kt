package org.example.app.core.persistence

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

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
        }

        println(
            "[Database] SQLite ready: " +
                file.toAbsolutePath()
        )
    }
}