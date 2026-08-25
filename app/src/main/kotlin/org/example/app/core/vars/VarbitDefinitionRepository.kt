package org.example.app.core.vars

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves and caches varbit definitions from the active game cache.
 *
 * Features deal only in varbit IDs. They do not need to know which backing
 * varp or bit range a particular varbit occupies.
 */
class VarbitDefinitionRepository(
    private val cacheDirectory: Path,
) {

    private val definitions =
        ConcurrentHashMap<Int, VarbitDefinition>()

    fun get(
        id: Int,
    ): VarbitDefinition {
        require(id >= 0) {
            "Varbit id must be non-negative."
        }

        return definitions.computeIfAbsent(id) {
            VarbitDefinitionLoader.load(
                cacheDirectory = cacheDirectory,
                id = it,
            )
        }
    }
}