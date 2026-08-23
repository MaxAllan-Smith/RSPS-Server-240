package org.example.app.core.skills

enum class Skill(
    val id: Int,
    val levelUpComponentId: Int,
    val defaultLevel: Int = 1,
) {
    ATTACK(
        id = 0,
        levelUpComponentId = 6,
    ),

    DEFENCE(
        id = 1,
        levelUpComponentId = 17,
    ),

    STRENGTH(
        id = 2,
        levelUpComponentId = 49,
    ),

    HITPOINTS(
        id = 3,
        levelUpComponentId = 30,
        defaultLevel = 10,
    ),

    RANGED(
        id = 4,
        levelUpComponentId = 40,
    ),

    PRAYER(
        id = 5,
        levelUpComponentId = 38,
    ),

    MAGIC(
        id = 6,
        levelUpComponentId = 34,
    ),

    COOKING(
        id = 7,
        levelUpComponentId = 12,
    ),

    WOODCUTTING(
        id = 8,
        levelUpComponentId = 53,
    ),

    FLETCHING(
        id = 9,
        levelUpComponentId = 25,
    ),

    FISHING(
        id = 10,
        levelUpComponentId = 23,
    ),

    FIREMAKING(
        id = 11,
        levelUpComponentId = 21,
    ),

    CRAFTING(
        id = 12,
        levelUpComponentId = 14,
    ),

    SMITHING(
        id = 13,
        levelUpComponentId = 47,
    ),

    MINING(
        id = 14,
        levelUpComponentId = 36,
    ),

    HERBLORE(
        id = 15,
        levelUpComponentId = 28,
    ),

    AGILITY(
        id = 16,
        levelUpComponentId = 4,
    ),

    THIEVING(
        id = 17,
        levelUpComponentId = 51,
    ),

    SLAYER(
        id = 18,
        levelUpComponentId = 45,
    ),

    FARMING(
        id = 19,
        levelUpComponentId = 19,
    ),

    RUNECRAFT(
        id = 20,
        levelUpComponentId = 43,
    ),

    HUNTER(
        id = 21,
        levelUpComponentId = 32,
    ),

    CONSTRUCTION(
        id = 22,
        levelUpComponentId = 9,
    ),

    SAILING(
        id = 23,
        levelUpComponentId = 57,
    ),
}