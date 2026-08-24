package org.example.app.core.world.collision

import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration

/**
 * Downloads a pinned, pre-generated OSRS collision map and caches it locally.
 *
 * The source is Skretzo/shortest-path's weekly cache-derived collision map.
 * We pin the 2026-08-13 snapshot because it immediately follows the selected
 * OpenRS2 rev-240 cache (2026-08-12), avoiding silent changes from newer game
 * revisions.
 */
class RemoteCollisionMapProvider(
    private val file: Path,
    private val source: URI = DEFAULT_SOURCE,
) : CollisionMapProvider {

    private val client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    override fun open(): InputStream {
        if (!Files.isRegularFile(file)) {
            download()
        } else {
            println(
                "[Collision] Reusing collision map: ${file.toAbsolutePath()}"
            )
        }
        return Files.newInputStream(file)
    }

    private fun download() {
        Files.createDirectories(file.toAbsolutePath().parent)

        println("[Collision] Downloading pinned OSRS collision map...")

        val request =
            HttpRequest.newBuilder()
                .uri(source)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "RSPS_RSProt_Server collision-bootstrap")
                .GET()
                .build()

        val response =
            client.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream(),
            )

        check(response.statusCode() in 200 until 300) {
            "Collision map request failed with HTTP ${response.statusCode()}."
        }

        val temporary = file.resolveSibling("${file.fileName}.part")
        response.body().use { input ->
            Files.copy(
                input,
                temporary,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        Files.move(
            temporary,
            file,
            StandardCopyOption.REPLACE_EXISTING,
        )

        println("[Collision] Collision map ready: ${file.toAbsolutePath()}")
    }

    companion object {
        // Weekly collision map snapshot committed 2026-08-13.
        private val DEFAULT_SOURCE = URI.create(
            "https://raw.githubusercontent.com/Skretzo/shortest-path/" +
                "3208646f33c8f155d0262c5fc84f8e29f7599838/" +
                "src/main/resources/collision-map.zip"
        )
    }
}
