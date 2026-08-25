package org.example.app.core.items

/** In-memory lookup of [ItemDefinition]s loaded once from an [ItemDefinitionSource]. */
class ItemDefinitionRepository(
    source: ItemDefinitionSource,
) {

    private val definitions: Map<Int, ItemDefinition> =
        source
            .load()
            .associateBy(
                ItemDefinition::id,
            )

    operator fun get(
        id: Int,
    ): ItemDefinition? =
        definitions[id]
}