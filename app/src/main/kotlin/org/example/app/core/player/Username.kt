package org.example.app.core.player

import java.util.Locale

fun String.normalizedUsername(): String {
    return trim().lowercase(Locale.ROOT)
}
