package org.example.app.core.items

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