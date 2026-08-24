package org.example.app.core.items

class ItemDefinitionRepository(
    private val definitions: Map<Int, ItemDefinition>,
) {

    operator fun get(
        id: Int,
    ): ItemDefinition? =
        definitions[id]
}