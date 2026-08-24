package org.example.app.core.world.collision

import org.example.app.core.world.WorldCollision
import org.rsmod.routefinder.flag.CollisionFlag
import java.util.zip.ZipInputStream
import kotlin.math.ceil

/**
 * Adapts the compact two-bit collision format used by the Shortest Path map
 * into RSMod's CollisionFlagMap.
 *
 * Each 64x64 map square stores two passability bits per tile: north and east.
 * South/west are represented by the neighbouring tile. Only cache-backed map
 * squares are allocated, so missing/void world space remains blocked by
 * CollisionFlagMap.DEFAULT_COLLISION_FLAG.
 */
class PrecomputedCollisionLoader(
    private val provider: CollisionMapProvider,
) {

    fun loadInto(world: WorldCollision) {
        check(!world.isLoaded) {
            "World collision has already been loaded."
        }

        println("[Collision] Hydrating RSMod collision map...")

        val regions = loadRegions()

        // Pass one allocates only real map/plane data. This keeps void blocked.
        for (region in regions) {
            for (level in 0 until region.planeCount) {
                allocateRegion(world, region.x, region.z, level)
            }
        }

        // Pass two projects the north/east edge representation into RSMod flags.
        var blockedTiles = 0
        for (region in regions) {
            for (level in 0 until region.planeCount) {
                for (localX in 0 until REGION_SIZE) {
                    for (localZ in 0 until REGION_SIZE) {
                        val x = region.x * REGION_SIZE + localX
                        val z = region.z * REGION_SIZE + localZ

                        val north = region.open(localX, localZ, level, NORTH_FLAG)
                        val east = region.open(localX, localZ, level, EAST_FLAG)
                        val south =
                            if (localZ > 0) {
                                region.open(localX, localZ - 1, level, NORTH_FLAG)
                            } else {
                                openAdjacent(x, z - 1, level, NORTH_FLAG)
                            }

                        val west =
                            if (localX > 0) {
                                region.open(localX - 1, localZ, level, EAST_FLAG)
                            } else {
                                openAdjacent(x - 1, z, level, EAST_FLAG)
                            }

                        if (!north && !east && !south && !west) {
                            world.flags.add(x, z, level, CollisionFlag.LOC)
                            blockedTiles++
                            continue
                        }

                        if (!north) {
                            addBoundary(
                                world = world,
                                x = x,
                                z = z,
                                level = level,
                                ownFlag = CollisionFlag.WALL_NORTH,
                                otherX = x,
                                otherZ = z + 1,
                                otherFlag = CollisionFlag.WALL_SOUTH,
                            )
                        }

                        if (!east) {
                            addBoundary(
                                world = world,
                                x = x,
                                z = z,
                                level = level,
                                ownFlag = CollisionFlag.WALL_EAST,
                                otherX = x + 1,
                                otherZ = z,
                                otherFlag = CollisionFlag.WALL_WEST,
                            )
                        }
                    }
                }
            }
        }

        world.markLoaded()

        println(
            "[Collision] RSMod collision ready: " +
                "regions=${regions.size}, blockedTiles=$blockedTiles."
        )
    }

    private fun loadRegions(): List<RegionData> {
        val regions = ArrayList<RegionData>()

        provider.open().use { stream ->
            ZipInputStream(stream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue

                    val name = entry.name.substringAfterLast('/').substringBefore('.')
                    val parts = name.split('_')
                    require(parts.size == 2) {
                        "Invalid collision region entry '${entry.name}'."
                    }

                    val data = zip.readBytes()
                    if (data.isEmpty()) continue

                    regions +=
                        RegionData(
                            x = parts[0].toInt(),
                            z = parts[1].toInt(),
                            data = data,
                            planeCount =
                                ceil(data.size.toDouble() / BYTES_PER_PLANE)
                                    .toInt()
                                    .coerceIn(1, LEVEL_COUNT),
                        )
                }
            }
        }

        require(regions.isNotEmpty()) {
            "Collision map contained no regions."
        }

        regionIndex = regions.associateBy { packRegion(it.x, it.z) }
        return regions
    }

    private lateinit var regionIndex: Map<Int, RegionData>

    private fun openAdjacent(
        x: Int,
        z: Int,
        level: Int,
        flag: Int,
    ): Boolean {
        val regionX = x / REGION_SIZE
        val regionZ = z / REGION_SIZE
        val region = regionIndex[packRegion(regionX, regionZ)] ?: return false
        if (level !in 0 until region.planeCount) return false

        return region.open(
            localX = x and REGION_MASK,
            localZ = z and REGION_MASK,
            level = level,
            flag = flag,
        )
    }

    private fun allocateRegion(
        world: WorldCollision,
        regionX: Int,
        regionZ: Int,
        level: Int,
    ) {
        val baseX = regionX * REGION_SIZE
        val baseZ = regionZ * REGION_SIZE

        for (zoneX in 0 until REGION_SIZE step ZONE_SIZE) {
            for (zoneZ in 0 until REGION_SIZE step ZONE_SIZE) {
                world.flags.allocateIfAbsent(
                    baseX + zoneX,
                    baseZ + zoneZ,
                    level,
                )
            }
        }
    }

    private fun addBoundary(
        world: WorldCollision,
        x: Int,
        z: Int,
        level: Int,
        ownFlag: Int,
        otherX: Int,
        otherZ: Int,
        otherFlag: Int,
    ) {
        world.flags.add(x, z, level, ownFlag)

        if (world.flags.isZoneAllocated(otherX, otherZ, level)) {
            world.flags.add(otherX, otherZ, level, otherFlag)
        }
    }

    private data class RegionData(
        val x: Int,
        val z: Int,
        val data: ByteArray,
        val planeCount: Int,
    ) {
        fun open(
            localX: Int,
            localZ: Int,
            level: Int,
            flag: Int,
        ): Boolean {
            val bit =
                (
                    level * REGION_SIZE * REGION_SIZE +
                        localZ * REGION_SIZE +
                        localX
                    ) * FLAG_COUNT + flag

            val byteIndex = bit ushr 3
            if (byteIndex !in data.indices) return false

            val mask = 1 shl (bit and 7)
            return data[byteIndex].toInt() and mask != 0
        }
    }

    private companion object {
        const val REGION_SIZE = 64
        const val REGION_MASK = REGION_SIZE - 1
        const val ZONE_SIZE = 8
        const val LEVEL_COUNT = 4
        const val FLAG_COUNT = 2
        const val NORTH_FLAG = 0
        const val EAST_FLAG = 1
        const val BYTES_PER_PLANE = REGION_SIZE * REGION_SIZE * FLAG_COUNT / 8

        fun packRegion(x: Int, z: Int): Int =
            (x and 0xFFFF) or ((z and 0xFFFF) shl 16)
    }
}
