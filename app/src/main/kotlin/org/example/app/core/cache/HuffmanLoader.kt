package org.example.app.core.cache

import net.rsprot.compression.HuffmanCodec
import net.rsprot.compression.provider.DefaultHuffmanCodecProvider
import net.rsprot.compression.provider.HuffmanCodecProvider
import org.openrs2.cache.Cache
import java.nio.file.Path

/** Loads the cache's Huffman compression table used by RSProt for chat/message compression. */
object HuffmanLoader {
    private const val BINARY_ARCHIVE = 10
    private const val HUFFMAN_GROUP = "huffman"

    fun load(
        cacheDirectory: Path,
    ): HuffmanCodecProvider {
        println("[Cache] Loading Huffman table...")

        val cache =
            Cache.open(cacheDirectory)

        cache.use { cache ->
            val data =
                cache.read(
                    BINARY_ARCHIVE,
                    HUFFMAN_GROUP,
                    0,
                )

            try {
                val codec =
                    HuffmanCodec.create(data)

                println(
                    "[Cache] Huffman table loaded."
                )

                return DefaultHuffmanCodecProvider(
                    codec
                )
            } finally {
                data.release()
            }
        }
    }
}