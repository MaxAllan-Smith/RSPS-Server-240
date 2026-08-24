package org.example.app.core.world.collision

import java.io.InputStream

/** Supplies the packed static OSRS walkability map used to hydrate RSMod. */
fun interface CollisionMapProvider {
    fun open(): InputStream
}
