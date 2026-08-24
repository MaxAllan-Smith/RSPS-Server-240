package org.example.app.core.items

interface ItemDefinitionSource {

    fun load(): Iterable<ItemDefinition>
}