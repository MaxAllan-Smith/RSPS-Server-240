package org.example.app.core.cache

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenRs2XteaKey(
    val archive: Int,
    val group: Int,
    @JsonProperty("name_hash")
    val nameHash: Int?,
    val name: String?,
    val mapsquare: Int?,
    val key: List<Int>,
) {
    init {
        require(key.size == KEY_COMPONENT_COUNT) {
            "XTEA key must contain exactly 4 integers."
        }
    }

    val keyArray: IntArray
        get() =
            key.toIntArray()

    private companion object {
        const val KEY_COMPONENT_COUNT: Int =
            4
    }
}