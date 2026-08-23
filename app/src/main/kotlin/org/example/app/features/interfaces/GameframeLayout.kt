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
        
        // SIDE 0
        // 161.376 -> combat
        const val COMBAT: Int = 76

        // SIDE1
        // 161:77 -> skills
        const val SKILLS: Int = 77

        // SIDE3
        // 161:79 -> inventory
        const val INVENTORY: Int = 79

        // 161:96 -> chatbox
        const val CHATBOX: Int = 96
    }

    object Interface {
        const val MINIMAP: Int = 160
        const val CHATBOX: Int = 162

        const val COMBAT: Int = 593
        const val SKILLS: Int = 320
        const val QUESTS: Int = 399
        const val INVENTORY: Int = 149
        const val EQUIPMENT: Int = 387
        const val PRAYER: Int = 541
        const val SPELLBOOK: Int = 218
    }
}