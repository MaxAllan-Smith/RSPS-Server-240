package org.example.app.core.cache

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.Locale

/** HTTP client for the OpenRS2 archive API, used to resolve and download a specific cache snapshot by revision/date window. */
data class CacheTarget(
    val major: Int,
    val minor: Int?,
    val windowStart: Instant,
    val windowEndExclusive: Instant,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRs2Build(
    val major: Int = 0,
    val minor: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRs2CacheEntry(
    val id: Int = 0,
    val scope: String = "",
    val game: String = "",
    val environment: String = "",
    val language: String = "",
    val builds: List<OpenRs2Build> = emptyList(),
    val timestamp: String? = null,
    val sources: List<String> = emptyList(),

    @JsonProperty("valid_indexes")
    val validIndexes: Int? = null,

    val indexes: Int? = null,

    @JsonProperty("valid_groups")
    val validGroups: Int? = null,

    val groups: Int? = null,

    @JsonProperty("disk_store_valid")
    val diskStoreValid: Boolean? = null,

    val size: Long? = null,
) {
    val instant: Instant
        get() = timestamp?.let(Instant::parse) ?: Instant.EPOCH

    fun isComplete(): Boolean {
        return diskStoreValid == true &&
            indexes != null &&
            validIndexes == indexes &&
            groups != null &&
            validGroups == groups
    }
}

class OpenRs2ArchiveClient {
    private val mapper = jacksonObjectMapper()

    private val httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

    fun resolve(target: CacheTarget): OpenRs2CacheEntry {
        println("[OpenRS2] Fetching cache catalogue...")

        val allCaches = fetchCaches()

        val candidates =
            allCaches.filter { cache ->
                cache.scope == "runescape" &&
                    cache.game == "oldschool" &&
                    cache.environment == "live" &&
                    cache.language == "en" &&
                    cache.sources.contains("Jagex") &&
                    cache.isComplete() &&
                    cache.builds.any { it.major == target.major } &&
                    !cache.instant.isBefore(target.windowStart) &&
                    cache.instant.isBefore(target.windowEndExclusive)
            }

        /*
         * OpenRS2 supports minor build metadata in general. Prefer an exact
         * minor match if the OSRS entry happens to provide one.
         */
        val exactMinor =
            if (target.minor != null) {
                candidates.filter { cache ->
                    cache.builds.any {
                        it.major == target.major &&
                            it.minor == target.minor
                    }
                }
            } else {
                emptyList()
            }

        val pool =
            exactMinor.ifEmpty {
                if (target.minor != null) {
                    println(
                        "[OpenRS2] No explicit ${target.major}.${target.minor} " +
                                "minor tag found; resolving build ${target.major} " +
                                "inside the requested patch window."
                    )
                }

                candidates
            }

        val selected =
            pool.maxByOrNull(OpenRs2CacheEntry::instant)
                ?: error(
                    "Could not find a complete official OpenRS2 cache for " +
                        "build ${target.major} in " +
                        "${target.windowStart} .. ${target.windowEndExclusive}"
                )

        println(
            "[OpenRS2] Selected cache id=${selected.id}, " +
                "build=${target.major}, timestamp=${selected.timestamp}"
        )

        selected.size?.let {
            println(
                "[OpenRS2] Unpacked group data: ${formatBytes(it)}"
            )
        }

        return selected
    }

    fun downloadDiskZip(
        cache: OpenRs2CacheEntry,
        destination: Path,
    ) {
        Files.createDirectories(destination.parent)

        val uri =
            URI.create(
                "$BASE_URL/caches/${cache.scope}/${cache.id}/disk.zip"
            )

        println("[OpenRS2] Downloading:")
        println("[OpenRS2] $uri")

        val request =
            HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(15))
                .header("User-Agent", "RSPS-RSProt-Server/1.0")
                .GET()
                .build()

        val response =
            httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream(),
            )

        check(response.statusCode() == 200) {
            "OpenRS2 returned HTTP ${response.statusCode()} for $uri"
        }

        val contentLength =
            response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1L)

        response.body().use { input ->
            Files.newOutputStream(
                destination,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { output ->
                val buffer = ByteArray(1024 * 1024)

                var copied = 0L
                var nextReport = 10

                while (true) {
                    val count = input.read(buffer)

                    if (count < 0) {
                        break
                    }

                    output.write(buffer, 0, count)
                    copied += count

                    if (contentLength > 0) {
                        val percent =
                            ((copied * 100L) / contentLength).toInt()

                        if (percent >= nextReport) {
                            println(
                                "[OpenRS2] Download $percent% " +
                                    "(${formatBytes(copied)} / " +
                                    "${formatBytes(contentLength)})"
                            )

                            nextReport += 10
                        }
                    }
                }

                println(
                    "[OpenRS2] Download complete: ${formatBytes(copied)}"
                )
            }
        }
    }

    private fun fetchCaches(): List<OpenRs2CacheEntry> {
        val uri =
            URI.create("$BASE_URL/caches.json")

        val request =
            HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "RSPS-RSProt-Server/1.0")
                .GET()
                .build()

        val response =
            httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(),
            )

        check(response.statusCode() == 200) {
            "OpenRS2 returned HTTP ${response.statusCode()} for $uri"
        }

        return mapper.readValue(response.body())
    }

    private fun formatBytes(bytes: Long): String {
        return String.format(
            Locale.ROOT,
            "%.1f MiB",
            bytes / (1024.0 * 1024.0),
        )
    }

    private companion object {
        private const val BASE_URL =
            "https://archive.openrs2.org"
    }
}