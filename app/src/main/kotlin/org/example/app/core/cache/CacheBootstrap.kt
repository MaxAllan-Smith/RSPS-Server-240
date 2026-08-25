package org.example.app.core.cache

import java.io.BufferedInputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator
import java.util.Properties
import java.util.zip.ZipInputStream

/** Downloads and caches one pinned OpenRS2 cache snapshot on disk so the server is deterministic across restarts. */
data class PreparedCache(
    val directory: Path,
    val metadata: OpenRs2CacheEntry,
)

class CacheBootstrap(
    private val archiveClient: OpenRs2ArchiveClient,
    private val cacheRoot: Path,
    private val cacheDirectory: Path,
) {
    fun prepare(target: CacheTarget): PreparedCache {
        Files.createDirectories(cacheRoot)

        val selected =
            archiveClient.resolve(target)

        if (isCurrent(selected)) {
            println(
                "[Cache] Reusing local cache: " +
                    cacheDirectory.toAbsolutePath()
            )

            return PreparedCache(
                directory = cacheDirectory,
                metadata = selected,
            )
        }

        println(
            "[Cache] Local cache missing or stale; downloading OpenRS2 cache."
        )

        val zipFile =
            cacheRoot.resolve(
                "openrs2-${selected.id}.disk.zip.part"
            )

        val stagingDirectory =
            cacheRoot.resolve(
                "${cacheDirectory.fileName}.staging"
            )

        deleteRecursively(stagingDirectory)
        Files.deleteIfExists(zipFile)

        try {
            archiveClient.downloadDiskZip(
                selected,
                zipFile,
            )

            Files.createDirectories(stagingDirectory)

            println("[Cache] Extracting cache...")

            extractDiskZip(
                zipFile,
                stagingDirectory,
            )

            val dat2 =
                stagingDirectory.resolve(
                    "main_file_cache.dat2"
                )

            check(Files.isRegularFile(dat2)) {
                "Downloaded OpenRS2 archive did not contain " +
                    "main_file_cache.dat2"
            }

            deleteRecursively(cacheDirectory)

            try {
                Files.move(
                    stagingDirectory,
                    cacheDirectory,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    stagingDirectory,
                    cacheDirectory,
                )
            }

            writeMetadata(
                selected,
                target,
            )

            println(
                "[Cache] Ready: ${cacheDirectory.toAbsolutePath()}"
            )

            return PreparedCache(
                directory = cacheDirectory,
                metadata = selected,
            )
        } finally {
            Files.deleteIfExists(zipFile)

            if (Files.exists(stagingDirectory)) {
                deleteRecursively(stagingDirectory)
            }
        }
    }

    private fun isCurrent(
        selected: OpenRs2CacheEntry,
    ): Boolean {
        val dat2 =
            cacheDirectory.resolve(
                "main_file_cache.dat2"
            )

        if (!Files.isRegularFile(dat2)) {
            return false
        }

        val metadataFile =
            cacheDirectory.resolve(METADATA_FILE)

        if (!Files.isRegularFile(metadataFile)) {
            return false
        }

        val properties =
            Properties()

        Files.newBufferedReader(metadataFile).use {
            properties.load(it)
        }

        val installedId =
            properties
                .getProperty("openrs2.id")
                ?.toIntOrNull()

        return installedId == selected.id
    }

    private fun writeMetadata(
        selected: OpenRs2CacheEntry,
        target: CacheTarget,
    ) {
        val properties =
            Properties().apply {
                setProperty(
                    "openrs2.id",
                    selected.id.toString(),
                )

                setProperty(
                    "openrs2.scope",
                    selected.scope,
                )

                setProperty(
                    "openrs2.timestamp",
                    selected.timestamp.orEmpty(),
                )

                setProperty(
                    "protocol.major",
                    target.major.toString(),
                )

                setProperty(
                    "client.minor",
                    target.minor?.toString().orEmpty(),
                )
            }

        val metadataFile =
            cacheDirectory.resolve(METADATA_FILE)

        Files.newBufferedWriter(metadataFile).use {
            properties.store(
                it,
                "Automatically generated OpenRS2 cache metadata",
            )
        }
    }

    private fun extractDiskZip(
        zipFile: Path,
        destination: Path,
    ) {
        ZipInputStream(
            BufferedInputStream(
                Files.newInputStream(zipFile)
            )
        ).use { zip ->
            while (true) {
                val entry =
                    zip.nextEntry ?: break

                val name =
                    entry.name.replace('\\', '/')

                /*
                 * OpenRS2 disk.zip stores everything below cache/.
                 */
                if (!name.startsWith("cache/")) {
                    zip.closeEntry()
                    continue
                }

                val relativeName =
                    name.removePrefix("cache/")
                        .trimStart('/')

                if (relativeName.isEmpty()) {
                    zip.closeEntry()
                    continue
                }

                val output =
                    destination
                        .resolve(relativeName)
                        .normalize()

                check(
                    output.startsWith(
                        destination.normalize()
                    )
                ) {
                    "Unsafe ZIP entry: ${entry.name}"
                }

                if (entry.isDirectory) {
                    Files.createDirectories(output)
                } else {
                    Files.createDirectories(output.parent)

                    Files.newOutputStream(output).use {
                        zip.copyTo(it)
                    }
                }

                zip.closeEntry()
            }
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach {
                    Files.deleteIfExists(it)
                }
        }
    }

    private companion object {
        private const val METADATA_FILE =
            ".openrs2-cache.properties"
    }
}