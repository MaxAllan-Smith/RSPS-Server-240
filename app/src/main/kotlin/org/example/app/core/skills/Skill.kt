package org.example.app.core.skills

enum class Skill(
    val id: Int,
    val levelUpComponentId: Int,
    val levelUpFlashVarbitId: Int,
    val defaultLevel: Int = 1,
) {
    ATTACK(
        id = 0,
        levelUpComponentId = 6,
        levelUpFlashVarbitId = 20175,
    ),

    DEFENCE(
        id = 1,
        levelUpComponentId = 17,
        levelUpFlashVarbitId = 20178,
    ),

    STRENGTH(
        id = 2,
        levelUpComponentId = 49,
        levelUpFlashVarbitId = 20161,
    ),

    HITPOINTS(
        id = 3,
        levelUpComponentId = 30,
        levelUpFlashVarbitId = 20165,
        defaultLevel = 10,
    ),

    RANGED(
        id = 4,
        levelUpComponentId = 40,
        levelUpFlashVarbitId = 20183,
    ),

    PRAYER(
        id = 5,
        levelUpComponentId = 38,
        levelUpFlashVarbitId = 20170,
    ),

    MAGIC(
        id = 6,
        levelUpComponentId = 34,
        levelUpFlashVarbitId = 20171,
    ),

    COOKING(
        id = 7,
        levelUpComponentId = 12,
        levelUpFlashVarbitId = 20176,
    ),

    WOODCUTTING(
        id = 8,
        levelUpComponentId = 53,
        levelUpFlashVarbitId = 20169,
    ),

    FLETCHING(
        id = 9,
        levelUpComponentId = 25,
        levelUpFlashVarbitId = 20180,
    ),

    FISHING(
        id = 10,
        levelUpComponentId = 23,
        levelUpFlashVarbitId = 20164,
    ),

    FIREMAKING(
        id = 11,
        levelUpComponentId = 21,
        levelUpFlashVarbitId = 20163,
    ),

    CRAFTING(
        id = 12,
        levelUpComponentId = 14,
        levelUpFlashVarbitId = 20177,
    ),

    SMITHING(
        id = 13,
        levelUpComponentId = 47,
        levelUpFlashVarbitId = 20184,
    ),

    MINING(
        id = 14,
        levelUpComponentId = 36,
        levelUpFlashVarbitId = 20166,
    ),

    HERBLORE(
        id = 15,
        levelUpComponentId = 28,
        levelUpFlashVarbitId = 20181,
    ),

    AGILITY(
        id = 16,
        levelUpComponentId = 4,
        levelUpFlashVarbitId = 20162,
    ),

    THIEVING(
        id = 17,
        levelUpComponentId = 51,
        levelUpFlashVarbitId = 20168,
    ),

    SLAYER(
        id = 18,
        levelUpComponentId = 45,
        levelUpFlashVarbitId = 20173,
    ),

    FARMING(
        id = 19,
        levelUpComponentId = 19,
        levelUpFlashVarbitId = 20179,
    ),

    RUNECRAFT(
        id = 20,
        levelUpComponentId = 43,
        levelUpFlashVarbitId = 20167,
    ),

    HUNTER(
        id = 21,
        levelUpComponentId = 32,
        levelUpFlashVarbitId = 20182,
    ),

    CONSTRUCTION(
        id = 22,
        levelUpComponentId = 9,
        levelUpFlashVarbitId = 20174,
    ),

    SAILING(
        id = 23,
        levelUpComponentId = 57,
        levelUpFlashVarbitId = 20172,
    ),
}