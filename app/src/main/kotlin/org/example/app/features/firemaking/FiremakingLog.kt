package org.example.app.features.firemaking

/**
 * Standard logs that can be directly ignited with a Tinderbox.
 *
 * Pyre logs are intentionally excluded because they belong to Shades of
 * Mort'ton funeral-pyre interactions rather than ordinary line Firemaking.
 *
 * Coloured logs are also separate interactions because their ignition rules
 * differ from ordinary logs.
 */
internal enum class FiremakingLog(
    val displayName: String,
    val itemId: Int,
    val requiredLevel: Int,
    val experienceMilli: Int,
) {

    NORMAL(
        displayName =
            "Logs",

        itemId =
            1511,

        requiredLevel =
            1,

        experienceMilli =
            40_000,
    ),

    ACHEY(
        displayName =
            "Achey tree logs",

        itemId =
            2862,

        requiredLevel =
            1,

        experienceMilli =
            40_000,
    ),

    OAK(
        displayName =
            "Oak logs",

        itemId =
            1521,

        requiredLevel =
            15,

        experienceMilli =
            60_000,
    ),

    WILLOW(
        displayName =
            "Willow logs",

        itemId =
            1519,

        requiredLevel =
            30,

        experienceMilli =
            90_000,
    ),

    TEAK(
        displayName =
            "Teak logs",

        itemId =
            6333,

        requiredLevel =
            35,

        experienceMilli =
            105_000,
    ),

    JATOBA(
        displayName =
            "Jatoba logs",

        itemId =
            32902,

        requiredLevel =
            40,

        experienceMilli =
            120_000,
    ),

    ARCTIC_PINE(
        displayName =
            "Arctic pine logs",

        itemId =
            10810,

        requiredLevel =
            42,

        experienceMilli =
            125_000,
    ),

    MAPLE(
        displayName =
            "Maple logs",

        itemId =
            1517,

        requiredLevel =
            45,

        experienceMilli =
            135_000,
    ),

    MAHOGANY(
        displayName =
            "Mahogany logs",

        itemId =
            6332,

        requiredLevel =
            50,

        experienceMilli =
            157_500,
    ),

    YEW(
        displayName =
            "Yew logs",

        itemId =
            1515,

        requiredLevel =
            60,

        experienceMilli =
            202_500,
    ),

    BLISTERWOOD(
        displayName =
            "Blisterwood logs",

        itemId =
            24691,

        requiredLevel =
            62,

        experienceMilli =
            96_000,
    ),

    CAMPHOR(
        displayName =
            "Camphor logs",

        itemId =
            32904,

        requiredLevel =
            66,

        experienceMilli =
            180_000,
    ),

    MAGIC(
        displayName =
            "Magic logs",

        itemId =
            1513,

        requiredLevel =
            75,

        experienceMilli =
            303_800,
    ),

    IRONWOOD(
        displayName =
            "Ironwood logs",

        itemId =
            32907,

        requiredLevel =
            80,

        experienceMilli =
            220_500,
    ),

    REDWOOD(
        displayName =
            "Redwood logs",

        itemId =
            19669,

        requiredLevel =
            90,

        experienceMilli =
            350_000,
    ),

    ROSEWOOD(
        displayName =
            "Rosewood logs",

        itemId =
            32910,

        requiredLevel =
            92,

        experienceMilli =
            268_000,
    );

    companion object {

        private val byItemId:
            Map<Int, FiremakingLog> =
            entries.associateBy {
                it.itemId
            }

        fun find(
            itemId: Int,
        ): FiremakingLog? =
            byItemId[
                itemId
            ]
    }
}