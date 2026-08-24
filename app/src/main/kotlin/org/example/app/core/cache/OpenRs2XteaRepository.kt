package org.example.app.core.cache

class OpenRs2XteaRepository(
    keys: Iterable<OpenRs2XteaKey>,
) {

    private val byMapSquare =
        keys
            .asSequence()
            .filter {
                it.archive == MAP_ARCHIVE &&
                    it.mapsquare != null
            }
            .associateBy {
                checkNotNull(it.mapsquare)
            }

    operator fun get(
        mapSquareId: Int,
    ): IntArray? =
        byMapSquare[
            mapSquareId
        ]?.keyArray

    val size: Int
        get() =
            byMapSquare.size

    private companion object {
        const val MAP_ARCHIVE: Int =
            5
    }
}