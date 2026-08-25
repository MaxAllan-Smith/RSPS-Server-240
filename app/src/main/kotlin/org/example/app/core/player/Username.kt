package org.example.app.core.player

import java.util.Locale

/** Normalizes a raw client-submitted username into the canonical lowercase form used for lookups and persistence keys. */
fun String.normalizedUsername(): String {
    return trim().lowercase(Locale.ROOT)
}
