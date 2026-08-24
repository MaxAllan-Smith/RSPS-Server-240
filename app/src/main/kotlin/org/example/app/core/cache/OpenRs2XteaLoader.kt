package org.example.app.core.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class OpenRs2XteaLoader(
    private val cacheId: Int,
    private val dataDirectory: Path,
) {

    private val mapper =
        jacksonObjectMapper()

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(
                Duration.ofSeconds(
                    CONNECT_TIMEOUT_SECONDS
                )
            )
            .build()

    fun load(): OpenRs2XteaRepository {
        Files.createDirectories(
            dataDirectory
        )

        val file =
            dataDirectory.resolve(
                "openrs2-$cacheId-keys.json"
            )

        if (!Files.isRegularFile(file)) {
            download(
                destination = file,
            )
        } else {
            println(
                "[Cache] Reusing OpenRS2 XTEA keys: " +
                    file.toAbsolutePath()
            )
        }

        val keys =
            Files
                .newBufferedReader(file)
                .use {
                    mapper.readValue<
                        List<OpenRs2XteaKey>
                    >(it)
                }

        val repository =
            OpenRs2XteaRepository(
                keys
            )

        println(
            "[Cache] Loaded " +
                "${repository.size} map XTEA keys."
        )

        return repository
    }

    private fun download(
        destination: Path,
    ) {
        println(
            "[Cache] Downloading OpenRS2 XTEA keys " +
                "for cache id=$cacheId..."
        )

        val request =
            HttpRequest
                .newBuilder()
                .uri(
                    URI.create(
                        "$ARCHIVE_BASE_URL/" +
                            "$CACHE_SCOPE/" +
                            "$cacheId/keys.json"
                    )
                )
                .timeout(
                    Duration.ofSeconds(
                        REQUEST_TIMEOUT_SECONDS
                    )
                )
                .GET()
                .build()

        val response =
            client.send(
                request,
                HttpResponse
                    .BodyHandlers
                    .ofByteArray(),
            )

        check(
            response.statusCode() in
                200 until 300
        ) {
            "OpenRS2 XTEA request failed with " +
                "HTTP ${response.statusCode()}."
        }

        val temporary =
            destination.resolveSibling(
                "${destination.fileName}.part"
            )

        Files.write(
            temporary,
            response.body(),
        )

        Files.move(
            temporary,
            destination,
            java.nio.file.StandardCopyOption
                .REPLACE_EXISTING,
        )

        println(
            "[Cache] XTEA keys ready: " +
                destination.toAbsolutePath()
        )
    }

    private companion object {
        const val ARCHIVE_BASE_URL: String =
            "https://archive.openrs2.org/caches"

        const val CACHE_SCOPE: String =
            "runescape"

        const val CONNECT_TIMEOUT_SECONDS: Long =
            10L

        const val REQUEST_TIMEOUT_SECONDS: Long =
            60L
    }
}