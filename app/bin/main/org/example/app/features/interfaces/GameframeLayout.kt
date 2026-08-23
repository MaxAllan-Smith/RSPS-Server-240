package org.example.app.features.interfaces

/**
 * Interface layout used by the revision-240 resizable gameframe.
 *
 * Keep protocol/cache-specific interface and component IDs here rather than
 * scattering raw integers throughout interface handlers.
 */
internal object GameframeLayout {

    object TopLevel {
        const val RESIZABLE: Int = 161
    }

    object Slot {
        const val CHATBOX: Int = 96
        const val SIDEBAR: Int = 97
    }

    object Interface {
        const val CHATBOX: Int = 162
        const val MINIMAP: Int = 160

        const val COMBAT: Int = 593
        const val SKILLS: Int = 320
        const val QUESTS: Int = 399
        const val INVENTORY: Int = 149
        const val EQUIPMENT: Int = 387
        const val PRAYER: Int = 541
        const val SPELLBOOK: Int = 218
    }
}