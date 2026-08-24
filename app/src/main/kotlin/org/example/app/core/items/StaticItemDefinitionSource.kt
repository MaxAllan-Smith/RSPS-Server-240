package org.example.app.core.items

class StaticItemDefinitionSource(
    private val definitions: Iterable<ItemDefinition>,
) : ItemDefinitionSource {

    override fun load(): Iterable<ItemDefinition> =
        definitions
}