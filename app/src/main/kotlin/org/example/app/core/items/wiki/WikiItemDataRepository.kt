package org.example.app.core.items.wiki

import org.example.app.core.persistence.SqliteDatabase
import java.sql.Connection

/** Persists the latest synced wiki item price/name mapping to SQLite. */
class WikiItemDataRepository(
    private val database: SqliteDatabase,
) {

    fun updateMapping(
        items: List<WikiItemMapping>,
    ) {
        database.connection().use { connection ->
            connection.autoCommit = false

            try {
                connection.prepareStatement(
                    """
                    INSERT INTO item_definitions (
                        id,
                        name,
                        examine,
                        members,
                        low_alch,
                        high_alch,
                        ge_limit,
                        value,
                        icon,
                        wiki_updated_at
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    )

                    ON CONFLICT(id)
                    DO UPDATE SET
                        name =
                            excluded.name,
                        examine =
                            excluded.examine,
                        members =
                            excluded.members,
                        low_alch =
                            excluded.low_alch,
                        high_alch =
                            excluded.high_alch,
                        ge_limit =
                            excluded.ge_limit,
                        value =
                            excluded.value,
                        icon =
                            excluded.icon,
                        wiki_updated_at =
                            excluded.wiki_updated_at
                    """.trimIndent()
                ).use { statement ->
                    val updatedAt =
                        System.currentTimeMillis()

                    for (item in items) {
                        statement.setInt(
                            1,
                            item.id,
                        )

                        statement.setString(
                            2,
                            item.name,
                        )

                        statement.setString(
                            3,
                            item.examine,
                        )

                        statement.setNullableBoolean(
                            index = 4,
                            value = item.members,
                        )

                        statement.setNullableInt(
                            index = 5,
                            value = item.lowalch,
                        )

                        statement.setNullableInt(
                            index = 6,
                            value = item.highalch,
                        )

                        statement.setNullableInt(
                            index = 7,
                            value = item.limit,
                        )

                        statement.setNullableInt(
                            index = 8,
                            value = item.value,
                        )

                        statement.setString(
                            9,
                            item.icon,
                        )

                        statement.setLong(
                            10,
                            updatedAt,
                        )

                        statement.addBatch()
                    }

                    statement.executeBatch()
                }

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

    fun updateLatest(
        response: WikiLatestResponse,
    ) {
        database.connection().use { connection ->
            connection.autoCommit = false

            try {
                val updatedAt =
                    System.currentTimeMillis()

                connection.prepareStatement(
                    """
                    INSERT INTO item_definitions (
                        id
                    )
                    VALUES (?)

                    ON CONFLICT(id)
                    DO NOTHING
                    """.trimIndent()
                ).use { itemStatement ->
                    connection.prepareStatement(
                        """
                        INSERT INTO item_prices_latest (
                            item_id,
                            high_price,
                            high_time,
                            low_price,
                            low_time,
                            updated_at
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?
                        )

                        ON CONFLICT(item_id)
                        DO UPDATE SET
                            high_price =
                                excluded.high_price,
                            high_time =
                                excluded.high_time,
                            low_price =
                                excluded.low_price,
                            low_time =
                                excluded.low_time,
                            updated_at =
                                excluded.updated_at
                        """.trimIndent()
                    ).use { priceStatement ->
                        for (
                            (encodedItemId, price) in
                            response.data
                        ) {
                            val itemId =
                                encodedItemId
                                    .toIntOrNull()
                                    ?: continue

                            ensureItem(
                                statement =
                                    itemStatement,
                                itemId = itemId,
                            )

                            priceStatement.setInt(
                                1,
                                itemId,
                            )

                            priceStatement
                                .setNullableInt(
                                    index = 2,
                                    value = price.high,
                                )

                            priceStatement
                                .setNullableLong(
                                    index = 3,
                                    value =
                                        price.highTime,
                                )

                            priceStatement
                                .setNullableInt(
                                    index = 4,
                                    value = price.low,
                                )

                            priceStatement
                                .setNullableLong(
                                    index = 5,
                                    value =
                                        price.lowTime,
                                )

                            priceStatement.setLong(
                                6,
                                updatedAt,
                            )

                            priceStatement.addBatch()
                        }

                        itemStatement.executeBatch()
                        priceStatement.executeBatch()
                    }
                }

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

    fun updateAverage(
        interval: String,
        response: WikiAverageResponse,
    ) {
        require(
            interval == INTERVAL_5M ||
                interval == INTERVAL_1H
        ) {
            "Unsupported price interval '$interval'."
        }

        database.connection().use { connection ->
            connection.autoCommit = false

            try {
                connection.prepareStatement(
                    """
                    INSERT INTO item_definitions (
                        id
                    )
                    VALUES (?)

                    ON CONFLICT(id)
                    DO NOTHING
                    """.trimIndent()
                ).use { itemStatement ->
                    connection.prepareStatement(
                        """
                        INSERT INTO item_price_averages (
                            item_id,
                            interval,
                            timestamp,
                            avg_high_price,
                            high_volume,
                            avg_low_price,
                            low_volume
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?
                        )

                        ON CONFLICT(
                            item_id,
                            interval
                        )
                        DO UPDATE SET
                            timestamp =
                                excluded.timestamp,
                            avg_high_price =
                                excluded.avg_high_price,
                            high_volume =
                                excluded.high_volume,
                            avg_low_price =
                                excluded.avg_low_price,
                            low_volume =
                                excluded.low_volume
                        """.trimIndent()
                    ).use { averageStatement ->
                        for (
                            (encodedItemId, price) in
                            response.data
                        ) {
                            val itemId =
                                encodedItemId
                                    .toIntOrNull()
                                    ?: continue

                            ensureItem(
                                statement =
                                    itemStatement,
                                itemId = itemId,
                            )

                            averageStatement.setInt(
                                1,
                                itemId,
                            )

                            averageStatement.setString(
                                2,
                                interval,
                            )

                            averageStatement.setLong(
                                3,
                                response.timestamp,
                            )

                            averageStatement
                                .setNullableInt(
                                    index = 4,
                                    value =
                                        price.avgHighPrice,
                                )

                            averageStatement
                                .setNullableInt(
                                    index = 5,
                                    value =
                                        price.highPriceVolume,
                                )

                            averageStatement
                                .setNullableInt(
                                    index = 6,
                                    value =
                                        price.avgLowPrice,
                                )

                            averageStatement
                                .setNullableInt(
                                    index = 7,
                                    value =
                                        price.lowPriceVolume,
                                )

                            averageStatement.addBatch()
                        }

                        itemStatement.executeBatch()
                        averageStatement.executeBatch()
                    }
                }

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

    private fun ensureItem(
        statement:
            java.sql.PreparedStatement,
        itemId: Int,
    ) {
        statement.setInt(
            1,
            itemId,
        )

        statement.addBatch()
    }

    private fun java.sql.PreparedStatement
        .setNullableBoolean(
            index: Int,
            value: Boolean?,
        ) {
        if (value == null) {
            setNull(
                index,
                java.sql.Types.INTEGER,
            )
        } else {
            setInt(
                index,
                if (value) 1 else 0,
            )
        }
    }

    private fun java.sql.PreparedStatement
        .setNullableInt(
            index: Int,
            value: Int?,
        ) {
        if (value == null) {
            setNull(
                index,
                java.sql.Types.INTEGER,
            )
        } else {
            setInt(
                index,
                value,
            )
        }
    }

    private fun java.sql.PreparedStatement
        .setNullableLong(
            index: Int,
            value: Long?,
        ) {
        if (value == null) {
            setNull(
                index,
                java.sql.Types.INTEGER,
            )
        } else {
            setLong(
                index,
                value,
            )
        }
    }

    companion object {
        const val INTERVAL_5M: String =
            "5m"

        const val INTERVAL_1H: String =
            "1h"
    }
}