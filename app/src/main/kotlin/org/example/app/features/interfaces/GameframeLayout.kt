package org.example.app.features.interfaces

// Revision-240 resizable gameframe layout.
// Keep interface and component IDs centralized here.
internal object GameframeLayout {

    object TopLevel {
        const val RESIZABLE: Int = 161
    }

    object Slot {
        // 161:33 -> minimap/orbs
        const val MINIMAP_ORBS: Int = 33

        // SIDE0
        const val COMBAT: Int = 76

        // SIDE1
        const val SKILLS: Int = 77

        // SIDE2
        const val JOURNAL: Int = 78

        // SIDE3
        const val INVENTORY: Int = 79

        // 161:96 -> chatbox
        const val CHATBOX: Int = 96
    }

    object JournalSlot {
        // 629:43 -> currently selected journal content
        const val CONTENT: Int = 43
    }

    object Interface {
        const val MINIMAP: Int = 160
        const val CHATBOX: Int = 162

        const val COMBAT: Int = 593
        const val SKILLS: Int = 320
        const val INVENTORY: Int = 149

        // Journal shell and subinterfaces.
        const val JOURNAL: Int = 629
        const val CHARACTER_SUMMARY: Int = 712
        const val QUEST_LIST: Int = 399
        const val ACHIEVEMENT_DIARIES: Int = 259
        const val ADVENTURE_LOG: Int = 187

        const val EQUIPMENT: Int = 387
        const val PRAYER: Int = 541
        const val SPELLBOOK: Int = 218
    }
}