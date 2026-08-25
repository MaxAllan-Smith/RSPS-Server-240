package org.example.app.core.items

/** In-memory [ItemDefinitionSource] for tests or hard-coded definition sets. */
class StaticItemDefinitionSource(
    private val definitions: Iterable<ItemDefinition>,
) : ItemDefinitionSource {

    override fun load(): Iterable<ItemDefinition> =
        definitions
}