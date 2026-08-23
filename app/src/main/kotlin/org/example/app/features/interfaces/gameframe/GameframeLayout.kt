package org.example.app.features.interfaces.gameframe

// Revision-240 resizable gameframe layout.
// Keep interface and component IDs centralized here.
internal object GameframeLayout {

    object TopLevel {
        const val RESIZABLE: Int = 161
    }

    object Slot {
        const val XP_DROPS: Int = 9
        const val MAIN_MODAL: Int = 16
        const val FLOATER: Int = 18
        const val MINIMAP_ORBS: Int = 33

        // SIDE0..SIDE13
        const val COMBAT: Int = 76
        const val SKILLS: Int = 77
        const val JOURNAL: Int = 78
        const val INVENTORY: Int = 79
        const val EQUIPMENT: Int = 80
        const val PRAYER: Int = 81
        const val SPELLBOOK: Int = 82
        const val FRIENDS_CHAT: Int = 83
        const val ACCOUNT_MANAGEMENT: Int = 84
        const val FRIENDS_LIST: Int = 85
        const val LOGOUT: Int = 86
        const val SETTINGS: Int = 87
        const val EMOTES: Int = 88
        const val MUSIC: Int = 89

        const val CHATBOX: Int = 96
    }

    object JournalSlot {
        const val CONTENT: Int = 43
    }

    object Interface {
        const val WORLD_SWITCHER: Int = 69
        const val XP_DROPS: Int = 122
        const val MINIMAP: Int = 160
        const val CHATBOX: Int = 162
        const val WORLD_SWITCHER_OPTIONS: Int = 821

        // Side tabs and content.
        const val COMBAT: Int = 593
        const val SKILLS: Int = 320
        const val INVENTORY: Int = 149
        const val EQUIPMENT: Int = 387
        const val PRAYER: Int = 541
        const val SPELLBOOK: Int = 218
        const val FRIENDS_CHAT: Int = 7
        const val ACCOUNT_MANAGEMENT: Int = 109
        const val FRIENDS_LIST: Int = 429
        const val IGNORE_LIST: Int = 432
        const val LOGOUT: Int = 182
        const val SETTINGS: Int = 116
        const val EMOTES: Int = 216
        const val MUSIC: Int = 239

        // Journal shell and content.
        const val JOURNAL: Int = 629
        const val CHARACTER_SUMMARY: Int = 712
        const val QUEST_LIST: Int = 399
        const val ACHIEVEMENT_DIARIES: Int = 259
        const val ADVENTURE_LOG: Int = 187
    }
}