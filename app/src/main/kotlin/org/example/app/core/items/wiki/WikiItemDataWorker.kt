package org.example.app.core.items.wiki

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Background scheduler that periodically refreshes wiki item data via [WikiItemDataClient] into [WikiItemDataRepository]. */
class WikiItemDataWorker(
    private val client: WikiItemDataClient,
    private val repository: WikiItemDataRepository,
) : AutoCloseable {

    private val started =
        AtomicBoolean(false)

    private val executor:
        ScheduledExecutorService =
        Executors
            .newSingleThreadScheduledExecutor { task ->
                Thread(
                    task,
                    "wiki-item-data-worker",
                ).apply {
                    isDaemon = true
                }
            }

    fun start() {
        if (
            !started.compareAndSet(
                false,
                true,
            )
        ) {
            return
        }

        executor.scheduleWithFixedDelay(
            ::refreshMappingSafely,
            0L,
            MAPPING_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )

        executor.scheduleWithFixedDelay(
            ::refreshPricesSafely,
            PRICE_INITIAL_DELAY_SECONDS,
            PRICE_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )

        println(
            "[Items] Wiki item-data worker started."
        )
    }

    private fun refreshMappingSafely() {
        try {
            val mapping =
                client.mapping()

            repository.updateMapping(
                mapping
            )

            println(
                "[Items] Wiki mapping synchronized " +
                    "${mapping.size} items."
            )
        } catch (t: Throwable) {
            logFailure(
                operation = "mapping",
                throwable = t,
            )
        }
    }

    private fun refreshPricesSafely() {
        try {
            val latest =
                client.latest()

            repository.updateLatest(
                latest
            )

            val averages5m =
                client.averages5m()

            repository.updateAverage(
                interval =
                    WikiItemDataRepository
                        .INTERVAL_5M,
                response = averages5m,
            )

            val averages1h =
                client.averages1h()

            repository.updateAverage(
                interval =
                    WikiItemDataRepository
                        .INTERVAL_1H,
                response = averages1h,
            )

            println(
                "[Items] Wiki prices synchronized " +
                    "latest=${latest.data.size}, " +
                    "5m=${averages5m.data.size}, " +
                    "1h=${averages1h.data.size}."
            )
        } catch (t: Throwable) {
            logFailure(
                operation = "prices",
                throwable = t,
            )
        }
    }

    private fun logFailure(
        operation: String,
        throwable: Throwable,
    ) {
        System.err.println(
            "[Items] Wiki $operation synchronization failed: " +
                "${throwable.message}"
        )

        throwable.printStackTrace()
    }

    override fun close() {
        if (
            !started.compareAndSet(
                true,
                false,
            )
        ) {
            return
        }

        executor.shutdownNow()

        println(
            "[Items] Wiki item-data worker stopped."
        )
    }

    private companion object {
        const val PRICE_INITIAL_DELAY_SECONDS: Long =
            5L

        const val PRICE_INTERVAL_SECONDS: Long =
            60L

        const val MAPPING_INTERVAL_HOURS: Long =
            6L
    }
}