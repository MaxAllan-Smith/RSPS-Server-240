package org.example.app.core.items

/** Core-owned contract for loading item definitions from any backing store. */
interface ItemDefinitionSource {

    fun load(): Iterable<ItemDefinition>
}