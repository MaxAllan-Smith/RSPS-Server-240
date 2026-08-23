package org.example.app.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.rsprot.protocol.api.js5.Js5GroupProvider
import net.rsprot.protocol.api.js5.Js5Service
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store
import org.openrs2.cache.VersionTrailer
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class RsProtJs5Provider private constructor(
    private val store: Store,
) : Js5GroupProvider,
    AutoCloseable {

    private data class CachedResponse(
        val owned: ByteBuf,
        val exposed: ByteBuf,
    )

    /*
     * DiskStore itself is not thread-safe.
     *
     * RSProt may query the provider both while serving a group and while
     * calculating prefetch sizes, so cache misses serialize their disk access.
     */
    private val storeLock = Any()

    /*
     * Responses are cached lazily rather than loading ~180 MiB of cache
     * content into memory at startup.
     */
    private val responses =
        ConcurrentHashMap<Long, CachedResponse>()

    init {
        synchronized(storeLock) {
            responses[key(Store.ARCHIVESET, Store.ARCHIVESET)] =
                buildMasterResponse()
        }

        println("[JS5] Master index prepared.")
    }

    override fun provide(
        archive: Int,
        group: Int,
    ): ByteBuf? {
        val key = key(archive, group)

        responses[key]?.let {
            return it.exposed
        }

        synchronized(storeLock) {
            responses[key]?.let {
                return it.exposed
            }

            if (!store.exists(archive, group)) {
                return null
            }

            val response =
                buildGroupResponse(
                    archive,
                    group,
                )

            responses[key] = response

            return response.exposed
        }
    }

    private fun buildGroupResponse(
        archive: Int,
        group: Int,
    ): CachedResponse {
        val data =
            store.read(
                archive,
                group,
            )

        try {
            /*
             * Normal cache groups have a two-byte version trailer in
             * DiskStore. It isn't transmitted as part of a normal JS5
             * group response.
             *
             * Archive 255 reference tables are treated differently,
             * matching RSMod's RSProt adapter.
             */
            if (archive != Store.ARCHIVESET) {
                VersionTrailer.strip(data)
            }

            return prepare(
                archive,
                group,
                data,
            )
        } finally {
            data.release()
        }
    }

    private fun buildMasterResponse(): CachedResponse {
        val masterIndex =
            Js5MasterIndex.create(store)

        masterIndex.format =
            MasterIndexFormat.VERSIONED

        val raw =
            Unpooled.buffer()

        try {
            masterIndex.write(raw)

            val compressed =
                Js5Compression.compress(
                    raw,
                    Js5CompressionType.UNCOMPRESSED,
                )

            try {
                return prepare(
                    Store.ARCHIVESET,
                    Store.ARCHIVESET,
                    compressed,
                )
            } finally {
                compressed.release()
            }
        } finally {
            raw.release()
        }
    }

    private fun prepare(
        archive: Int,
        group: Int,
        payload: ByteBuf,
    ): CachedResponse {
        val readableBytes =
            payload.readableBytes()

        /*
         * Extra space accounts for JS5 headers and 512-byte continuation
         * markers. ByteBuf can expand if necessary.
         */
        val output =
            Unpooled.buffer(
                readableBytes +
                    16 +
                    (readableBytes / 512)
            )

        try {
            Js5Service.prepareJs5Buffer(
                archive,
                group,
                payload.slice(),
                output,
            )

            /*
             * RSProt can call provide() just to inspect readableBytes()
             * during prefetch calculation. Returning an unreleasable view
             * means callers can't accidentally destroy our cached response.
             */
            val exposed =
                Unpooled.unreleasableBuffer(
                    output.asReadOnly()
                )

            return CachedResponse(
                owned = output,
                exposed = exposed,
            )
        } catch (t: Throwable) {
            output.release()
            throw t
        }
    }

    override fun close() {
        synchronized(storeLock) {
            for ((buffer) in responses.values) {

                if (buffer.refCnt() > 0) {
                    buffer.release(
                        buffer.refCnt()
                    )
                }
            }

            responses.clear()

            store.close()
        }
    }

    private fun key(
        archive: Int,
        group: Int,
    ): Long {
        return (archive.toLong() shl 32) or
            (group.toLong() and 0xFFFFFFFFL)
    }

    companion object {
        fun open(
            cacheDirectory: Path,
        ): RsProtJs5Provider {
            return RsProtJs5Provider(
                Store.open(cacheDirectory)
            )
        }
    }
}