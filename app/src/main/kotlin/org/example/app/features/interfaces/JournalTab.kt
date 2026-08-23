package org.example.app.features.interfaces

internal enum class JournalTab(
    val componentId: Int,
    val varbitValue: Int,
    val interfaceId: Int,
) {
    CHARACTER_SUMMARY(
        componentId = 2,
        varbitValue = 0,
        interfaceId = GameframeLayout.Interface.CHARACTER_SUMMARY,
    ),

    QUEST_LIST(
        componentId = 10,
        varbitValue = 1,
        interfaceId = GameframeLayout.Interface.QUEST_LIST,
    ),

    ACHIEVEMENT_DIARIES(
        componentId = 18,
        varbitValue = 2,
        interfaceId = GameframeLayout.Interface.ACHIEVEMENT_DIARIES,
    );

    companion object {
        fun fromComponent(componentId: Int): JournalTab? =
            entries.firstOrNull {
                it.componentId == componentId
            }
    }
}